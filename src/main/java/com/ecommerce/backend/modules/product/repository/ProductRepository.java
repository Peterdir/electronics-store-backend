package com.ecommerce.backend.modules.product.repository;

import com.ecommerce.backend.modules.product.entity.Product;
import com.ecommerce.backend.modules.product.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    List<Product> findByCategoryIdAndStatus(Long categoryId, ProductStatus status);

    @Modifying
    @Query("UPDATE Product p SET p.status = :status WHERE p.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") ProductStatus status);

    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.status = ProductStatus.ACTIVE")
    Optional<Product> findActiveProductById(@Param("id") Long productId);

    Optional<Product> findByIdAndStatus(Long productId, ProductStatus status);

    List<Product> findTop5ByNameContaining(String keyword);

    List<Product> findAllByIdIn(List<Long> productIds);

    Page<Product> findByNameContainingIgnoreCaseAndStatus(String keyword, ProductStatus status, Pageable pageable);
}
