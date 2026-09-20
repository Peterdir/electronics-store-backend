package com.ecommerce.backend.modules.product.repository;

import com.ecommerce.backend.modules.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    Optional<ProductVariant> findByProductIdAndSku(Long productId, String sku);

}
