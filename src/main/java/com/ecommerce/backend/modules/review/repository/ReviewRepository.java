package com.ecommerce.backend.modules.review.repository;

import com.ecommerce.backend.modules.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = {"product", "product.productImages", "productVariant"})
    List<Review> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId);

    // Kiểm tra User đã Review Product chưa
    boolean existsByUserIdAndOrderItemIdAndIsDeletedFalse(Long userId, Long orderItemId);

    @Query("SELECT AVERAGE(r.rating) " +
            "FROM Review r " +
            "WHERE r.product.id = :productId AND r.isDeleted = false")
    Double getAverageRatingByProductId(@Param("productId") Long productId);

    @EntityGraph(attributePaths = {"user", "product"})
    @Query("""
        SELECT r FROM Review r
        WHERE r.isDeleted = false AND
        (:rating IS NULL OR r.rating = :rating) AND
        (:productName IS NULL OR LOWER(r.product.name) LIKE LOWER(CONCAT('%', :productName, '%')))
    """)
    Page<Review> findAllForAdmin(@Param("rating") Double rating, @Param("productName") String productName, Pageable pageable);
}
