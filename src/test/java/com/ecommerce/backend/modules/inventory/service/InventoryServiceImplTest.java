package com.ecommerce.backend.modules.inventory.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.modules.inventory.dto.request.AdjustStockRequest;
import com.ecommerce.backend.modules.inventory.dto.response.InventoryResponse;
import com.ecommerce.backend.modules.inventory.entity.Inventory;
import com.ecommerce.backend.modules.inventory.entity.InventoryHistory;
import com.ecommerce.backend.modules.inventory.enums.InventoryAction;
import com.ecommerce.backend.modules.inventory.mapper.InventoryMapper;
import com.ecommerce.backend.modules.inventory.repository.InventoryHistoryRepository;
import com.ecommerce.backend.modules.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryHistoryRepository inventoryHistoryRepository;

    @Mock
    private InventoryMapper inventoryMapper;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private Inventory mockInventory;
    private AdjustStockRequest adjustRequest;
    private InventoryResponse mockResponse;

    @BeforeEach
    void setUp() {
        mockInventory = new Inventory();
        mockInventory.setId(1L);
        mockInventory.setQuantity(50L); // Tồn kho hiện tại là 50

        adjustRequest = new AdjustStockRequest();
        adjustRequest.setQuantity(10L);
        adjustRequest.setReason("Test Adjustment");

        mockResponse = new InventoryResponse();
        mockResponse.setId(1L);
    }

    @Test
    void adjustStock_Add_Success() {
        // Arrange
        adjustRequest.setAction(InventoryAction.ADD);
        when(inventoryRepository.findById(anyLong())).thenReturn(Optional.of(mockInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(mockInventory);
        when(inventoryMapper.toResponse(any(Inventory.class))).thenReturn(mockResponse);

        // Act
        InventoryResponse response = inventoryService.adjustStock(1L, adjustRequest);

        // Assert
        // Xác nhận số lượng kho mới phải là 50 + 10 = 60
        assertEquals(60L, mockInventory.getQuantity());

        // Xác nhận hàm save được gọi 1 lần trên kho
        verify(inventoryRepository, times(1)).save(mockInventory);

        // Nâng cao: Dùng ArgumentCaptor để "chộp" (bắt lấy) đối tượng History lưu xuống DB xem có đúng số liệu không
        ArgumentCaptor<InventoryHistory> historyCaptor = ArgumentCaptor.forClass(InventoryHistory.class);
        verify(inventoryHistoryRepository, times(1)).save(historyCaptor.capture());
        
        InventoryHistory capturedHistory = historyCaptor.getValue();
        assertEquals(InventoryAction.ADD, capturedHistory.getAction());
        assertEquals(10L, capturedHistory.getQuantityChanged());
        assertEquals(60L, capturedHistory.getFinalStock());
    }

    @Test
    void adjustStock_Deduct_Success() {
        // Arrange
        adjustRequest.setAction(InventoryAction.DEDUCT);
        when(inventoryRepository.findById(anyLong())).thenReturn(Optional.of(mockInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(mockInventory);
        when(inventoryMapper.toResponse(any(Inventory.class))).thenReturn(mockResponse);

        // Act
        InventoryResponse response = inventoryService.adjustStock(1L, adjustRequest);

        // Assert
        // Xác nhận số lượng kho mới phải là 50 - 10 = 40
        assertEquals(40L, mockInventory.getQuantity());
        verify(inventoryRepository, times(1)).save(mockInventory);
        verify(inventoryHistoryRepository, times(1)).save(any(InventoryHistory.class));
    }

    @Test
    void adjustStock_Deduct_OutOfStock() {
        // Arrange
        adjustRequest.setAction(InventoryAction.DEDUCT);
        adjustRequest.setQuantity(100L); // Yêu cầu trừ 100, nhưng tồn kho chỉ có 50

        when(inventoryRepository.findById(anyLong())).thenReturn(Optional.of(mockInventory));

        // Act & Assert
        // Cá cược là phải ném ra lỗi BadRequestException
        assertThrows(BadRequestException.class, () -> {
            inventoryService.adjustStock(1L, adjustRequest);
        });

        // Đảm bảo rằng nếu lỗi thì tuyệt đối KHÔNG ĐƯỢC LƯU XUỐNG DB
        verify(inventoryRepository, never()).save(any(Inventory.class));
        verify(inventoryHistoryRepository, never()).save(any(InventoryHistory.class));
    }

    @Test
    void adjustStock_InventoryNotFound() {
        // Arrange
        when(inventoryRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            inventoryService.adjustStock(99L, adjustRequest);
        });

        // Đảm bảo không gọi save
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }
}
