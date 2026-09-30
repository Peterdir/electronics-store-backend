package com.ecommerce.backend.modules.wishlist.repository;

import com.ecommerce.backend.modules.wishlist.entity.Wishlist;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    @EntityGraph(attributePaths = {
            "product",
            "product.productImages",
            "product.variants",
            "product.variants.inventory"
    })
    List<Wishlist> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    boolean existsByUserIdAndProductId(Long userId, Long productId);
    
    void deleteByUserIdAndProductId(Long userId, Long productId);
}
