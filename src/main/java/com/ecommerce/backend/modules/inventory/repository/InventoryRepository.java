package com.ecommerce.backend.modules.inventory.repository;

import com.ecommerce.backend.modules.inventory.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    @EntityGraph(attributePaths = {"productVariant", "productVariant.product"})
    @Query("""
            SELECT i from Inventory i
            WHERE (:keyword IS NULL OR LOWER(i.productVariant.product.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
                AND (:minQuantity IS NULL OR i.quantity >= :minQuantity)
                AND (:maxQuantity IS NULL OR i.quantity <= :maxQuantity)
            """)
    List<Inventory> findByCriteria(@Param("keyword") String keyword, @Param("minQuantity") Long minQuantity,
            @Param("maxQuantity") Long maxQuantity);

    Optional<Inventory> findByProductVariantId(Long productVariantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productVariant.id = :productVariantId")
    Optional<Inventory> findByProductVariantIdForUpdate(@Param("productVariantId") Long productVariantId);

    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.quantity <= :threshold")
    Long countLowStockVariants(@Param("threshold") Long threshold);

    @EntityGraph(attributePaths = {"productVariant", "productVariant.product"})
    @Query("""
        SELECT i FROM Inventory i
        WHERE i.quantity <= :threshold
        ORDER BY i.quantity ASC
    """)
    List<Inventory> findTopLowStock(@Param("threshold") Long threshold, Pageable pageable);
}
