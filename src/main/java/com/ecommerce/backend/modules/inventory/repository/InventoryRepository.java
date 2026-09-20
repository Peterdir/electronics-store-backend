package com.ecommerce.backend.modules.inventory.repository;

import com.ecommerce.backend.modules.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    @Query("""
            SELECT i from Inventory i
            WHERE (:keyword IS NULL OR LOWER(i.productVariant.product.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
                AND (:minQuantity IS NULL OR i.quantity >= :minQuantity)
                AND (:maxQuantity IS NULL OR i.quantity <= :maxQuantity)
           """)
    List<Inventory> findByCriteria(@Param("keyword") String keyword,@Param("minQuantity") Long minQuantity,
                                   @Param("maxQuantity") Long maxQuantity);

}
