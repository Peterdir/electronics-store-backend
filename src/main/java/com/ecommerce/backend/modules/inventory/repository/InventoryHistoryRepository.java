package com.ecommerce.backend.modules.inventory.repository;

import com.ecommerce.backend.modules.inventory.entity.InventoryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryHistoryRepository extends JpaRepository<InventoryHistory, Long> {

    List<InventoryHistory> findByInventoryIdOrderByCreatedAtDesc(Long inventoryId);
}
