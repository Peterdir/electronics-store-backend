package com.ecommerce.backend.modules.cart.repository;

import com.ecommerce.backend.modules.cart.entity.Cart;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    @EntityGraph(attributePaths = {
            "items",
            "items.productVariant",
            "items.productVariant.product",
            "items.productVariant.inventory"
    })
    Optional<Cart> findByUserId(Long userId);
}
