package com.ecommerce.backend.modules.review.repository;

import com.ecommerce.backend.modules.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId);

    // Kiểm tra User đã Review Product chưa
    boolean existsByUserIdAndOrderItemIdAndIsDeletedFalse(Long userId, Long orderItemId);

    @Query("SELECT AVERAGE(r.rating) " +
            "FROM Review r " +
            "WHERE r.product.id = :productId AND r.isDeleted = false")
    Double getAverageRatingByProductId(@Param("productId") Long productId);
}
