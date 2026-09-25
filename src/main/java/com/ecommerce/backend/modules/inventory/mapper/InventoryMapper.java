package com.ecommerce.backend.modules.inventory.mapper;

import com.ecommerce.backend.modules.inventory.dto.response.InventoryHistoryResponse;
import com.ecommerce.backend.modules.inventory.dto.response.InventoryResponse;
import com.ecommerce.backend.modules.inventory.entity.Inventory;
import com.ecommerce.backend.modules.inventory.entity.InventoryHistory;
import com.ecommerce.backend.modules.inventory.enums.InventoryStatus;
import com.ecommerce.backend.modules.product.entity.Product;
import com.ecommerce.backend.modules.product.entity.ProductVariant;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {

    public InventoryResponse toResponse(Inventory inventory) {
        if (inventory == null) {
            return null;
        }

        ProductVariant variant = inventory.getProductVariant();
        Product product = (variant != null) ? variant.getProduct() : null;

        return InventoryResponse.builder()
                .id(inventory.getId())
                .productId(product != null ? product.getId() : null)
                .productName(product != null ? product.getName() : null)
                .variantId(variant != null ? variant.getId() : null)
                .sku(variant != null ? variant.getSku() : null)
                .quantity(inventory.getQuantity())
                .status(calculateStatus(inventory.getQuantity()))
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }

    public InventoryHistoryResponse toHistoryResponse(InventoryHistory history) {
        if (history == null) {
            return null;
        }

        return InventoryHistoryResponse.builder()
                .id(history.getId())
                .action(history.getAction())
                .quantityChanged(history.getQuantityChanged())
                .finalStock(history.getFinalStock())
                .reason(history.getReason())
                .performedBy(history.getPerformedBy())
                .createdAt(history.getCreatedAt())
                .build();
    }

    private InventoryStatus calculateStatus(Long quantity) {
        if (quantity == null || quantity <= 0) {
            return InventoryStatus.OUT_OF_STOCK;
        }
        if (quantity <= 10) {
            return InventoryStatus.LOW_STOCK;
        }
        return InventoryStatus.IN_STOCK;
    }
}
