package com.ecommerce.backend.modules.inventory.controller;

import com.ecommerce.backend.modules.inventory.dto.request.AdjustStockRequest;
import com.ecommerce.backend.modules.inventory.dto.response.InventoryHistoryResponse;
import com.ecommerce.backend.modules.inventory.dto.response.InventoryResponse;
import com.ecommerce.backend.modules.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
public class AdminInventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<List<InventoryResponse>> getInventoryList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long minQuantity,
            @RequestParam(required = false) Long maxQuantity) {
        return ResponseEntity.ok(inventoryService.getInventoryList(keyword, status, minQuantity, maxQuantity));
    }

    @PostMapping("/{id}/adjust")
    public ResponseEntity<InventoryResponse> adjustStock(
            @PathVariable Long id,
            @Valid @RequestBody AdjustStockRequest request) {
        return ResponseEntity.ok(inventoryService.adjustStock(id, request));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<InventoryHistoryResponse>> getInventoryHistory(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryService.getInventoryHistory(id));
    }
}
