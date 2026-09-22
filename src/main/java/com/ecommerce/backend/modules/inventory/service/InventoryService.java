package com.ecommerce.backend.modules.inventory.service;

import com.ecommerce.backend.modules.inventory.dto.request.AdjustStockRequest;
import com.ecommerce.backend.modules.inventory.dto.response.InventoryHistoryResponse;
import com.ecommerce.backend.modules.inventory.dto.response.InventoryResponse;

import java.util.List;

public interface InventoryService {

    List<InventoryResponse> getInventoryList(String keyword, String status, Long minQuantity, Long maxQuantity);

    InventoryResponse adjustStock(Long id, AdjustStockRequest request);

    List<InventoryHistoryResponse> getInventoryHistory(Long id);
}
