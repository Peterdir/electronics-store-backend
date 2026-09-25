package com.ecommerce.backend.modules.inventory.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.modules.inventory.dto.request.AdjustStockRequest;
import com.ecommerce.backend.modules.inventory.dto.response.InventoryHistoryResponse;
import com.ecommerce.backend.modules.inventory.dto.response.InventoryResponse;
import com.ecommerce.backend.modules.inventory.entity.Inventory;
import com.ecommerce.backend.modules.inventory.entity.InventoryHistory;
import com.ecommerce.backend.modules.inventory.enums.InventoryAction;
import com.ecommerce.backend.modules.inventory.mapper.InventoryMapper;
import com.ecommerce.backend.modules.inventory.repository.InventoryHistoryRepository;
import com.ecommerce.backend.modules.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;
    private final InventoryMapper inventoryMapper;

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponse> getInventoryList(String keyword, String status, Long minQuantity, Long maxQuantity) {
        return inventoryRepository.findByCriteria(keyword, minQuantity, maxQuantity).stream()
                .map(inventoryMapper::toResponse)
                .filter(res -> status == null || status.isBlank()
                        || res.getStatus().name().equalsIgnoreCase(status.trim())
                        || res.getStatus().name().replace("_", " ").equalsIgnoreCase(status.trim()))
                .toList();
    }

    @Override
    @Transactional
    public InventoryResponse adjustStock(Long id, AdjustStockRequest request) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found with id: " + id));

        Long currentStock = inventory.getQuantity() != null ? inventory.getQuantity() : 0L;
        Long newStock = calculateNewStock(currentStock, request);

        inventory.setQuantity(newStock);
        inventory.setUpdatedAt(Instant.now());

        Inventory updatedInventory = inventoryRepository.save(inventory);

        InventoryHistory history = InventoryHistory.builder()
                .action(request.getAction())
                .quantityChanged(request.getQuantity())
                .finalStock(newStock)
                .reason(request.getReason())
                .performedBy("Admin")
                .createdAt(Instant.now())
                .inventory(updatedInventory)
                .build();
        inventoryHistoryRepository.save(history);

        return inventoryMapper.toResponse(updatedInventory);
    }

    private Long calculateNewStock(Long currentStock, AdjustStockRequest request) {
        if (request.getAction() == InventoryAction.ADD) {
            return currentStock + request.getQuantity();
        }

        if (request.getAction() == InventoryAction.DEDUCT) {
            if (currentStock < request.getQuantity()) {
                throw new BadRequestException(
                        "Cannot deduct more than the current stock. The resulting stock cannot be negative.");
            }

            return currentStock - request.getQuantity();
        }

        throw new BadRequestException("Invalid inventory action: " + request.getAction());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryHistoryResponse> getInventoryHistory(Long id) {
        if (!inventoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Inventory not found with id: " + id);
        }

        return inventoryHistoryRepository.findByInventoryIdOrderByCreatedAtDesc(id).stream()
                .map(inventoryMapper::toHistoryResponse)
                .toList();
    }
}
