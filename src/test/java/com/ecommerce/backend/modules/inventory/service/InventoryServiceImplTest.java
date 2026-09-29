package com.ecommerce.backend.modules.inventory.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.modules.inventory.dto.request.AdjustStockRequest;
import com.ecommerce.backend.modules.inventory.dto.response.InventoryHistoryResponse;
import com.ecommerce.backend.modules.inventory.dto.response.InventoryResponse;
import com.ecommerce.backend.modules.inventory.entity.Inventory;
import com.ecommerce.backend.modules.inventory.entity.InventoryHistory;
import com.ecommerce.backend.modules.inventory.enums.InventoryAction;
import com.ecommerce.backend.modules.inventory.enums.InventoryStatus;
import com.ecommerce.backend.modules.inventory.mapper.InventoryMapper;
import com.ecommerce.backend.modules.inventory.repository.InventoryHistoryRepository;
import com.ecommerce.backend.modules.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryServiceImpl Unit Test Suite")
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
        mockInventory.setQuantity(50L);

        adjustRequest = new AdjustStockRequest();
        adjustRequest.setQuantity(10L);
        adjustRequest.setReason("Stock adjustment test");

        mockResponse = new InventoryResponse();
        mockResponse.setId(1L);
        mockResponse.setStatus(InventoryStatus.IN_STOCK);
    }

    // =========================================================================
    // AC-INV-01: Lấy danh sách tồn kho (getInventoryList)
    // =========================================================================
    @Nested
    @DisplayName("AC-INV-01: Get Inventory List Tests")
    class GetInventoryListTests {

        @Test
        @DisplayName("TC-INV-01 [Positive]: Filter inventory by keyword and status")
        void getInventoryList_WithFilters_ReturnsFilteredList() {
            when(inventoryRepository.findByCriteria("MacBook", 5L, 100L)).thenReturn(List.of(mockInventory));
            when(inventoryMapper.toResponse(mockInventory)).thenReturn(mockResponse);

            List<InventoryResponse> responses = inventoryService.getInventoryList("MacBook", "IN_STOCK", 5L, 100L);

            assertNotNull(responses);
            assertEquals(1, responses.size());
            verify(inventoryRepository, times(1)).findByCriteria("MacBook", 5L, 100L);
        }

        @Test
        @DisplayName("TC-INV-02 [Positive]: Filter inventory with status using space format")
        void getInventoryList_StatusWithSpace_ReturnsMatches() {
            mockResponse.setStatus(InventoryStatus.OUT_OF_STOCK);
            when(inventoryRepository.findByCriteria(null, null, null)).thenReturn(List.of(mockInventory));
            when(inventoryMapper.toResponse(mockInventory)).thenReturn(mockResponse);

            List<InventoryResponse> responses = inventoryService.getInventoryList(null, "out of stock", null, null);

            assertNotNull(responses);
            assertEquals(1, responses.size());
        }

        @Test
        @DisplayName("TC-INV-03 [Boundary]: No status filter provided returns all items")
        void getInventoryList_NullOrBlankStatus_ReturnsAll() {
            when(inventoryRepository.findByCriteria(null, null, null)).thenReturn(List.of(mockInventory));
            when(inventoryMapper.toResponse(mockInventory)).thenReturn(mockResponse);

            List<InventoryResponse> responses = inventoryService.getInventoryList(null, "", null, null);

            assertNotNull(responses);
            assertEquals(1, responses.size());
        }
    }

    // =========================================================================
    // AC-INV-02: Điều chỉnh tồn kho (adjustStock)
    // =========================================================================
    @Nested
    @DisplayName("AC-INV-02: Adjust Stock Tests")
    class AdjustStockTests {

        @Test
        @DisplayName("TC-INV-04 [Positive]: Adjust stock ADD action increases quantity")
        void adjustStock_Add_Success() {
            adjustRequest.setAction(InventoryAction.ADD);
            when(inventoryRepository.findById(1L)).thenReturn(Optional.of(mockInventory));
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(mockInventory);
            when(inventoryMapper.toResponse(any(Inventory.class))).thenReturn(mockResponse);

            InventoryResponse response = inventoryService.adjustStock(1L, adjustRequest);

            assertEquals(60L, mockInventory.getQuantity());
            verify(inventoryRepository, times(1)).save(mockInventory);

            ArgumentCaptor<InventoryHistory> historyCaptor = ArgumentCaptor.forClass(InventoryHistory.class);
            verify(inventoryHistoryRepository, times(1)).save(historyCaptor.capture());

            InventoryHistory capturedHistory = historyCaptor.getValue();
            assertEquals(InventoryAction.ADD, capturedHistory.getAction());
            assertEquals(10L, capturedHistory.getQuantityChanged());
            assertEquals(60L, capturedHistory.getFinalStock());
        }

        @Test
        @DisplayName("TC-INV-05 [Positive]: Adjust stock DEDUCT action decreases quantity")
        void adjustStock_Deduct_Success() {
            adjustRequest.setAction(InventoryAction.DEDUCT);
            when(inventoryRepository.findById(1L)).thenReturn(Optional.of(mockInventory));
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(mockInventory);
            when(inventoryMapper.toResponse(any(Inventory.class))).thenReturn(mockResponse);

            InventoryResponse response = inventoryService.adjustStock(1L, adjustRequest);

            assertEquals(40L, mockInventory.getQuantity());
            verify(inventoryRepository, times(1)).save(mockInventory);
        }

        @Test
        @DisplayName("TC-INV-06 [Boundary]: Adjust stock with null currentQuantity defaults to 0")
        void adjustStock_NullInitialQuantity_DefaultsToZero() {
            mockInventory.setQuantity(null);
            adjustRequest.setAction(InventoryAction.ADD);
            adjustRequest.setQuantity(15L);

            when(inventoryRepository.findById(1L)).thenReturn(Optional.of(mockInventory));
            when(inventoryRepository.save(any(Inventory.class))).thenReturn(mockInventory);
            when(inventoryMapper.toResponse(any(Inventory.class))).thenReturn(mockResponse);

            inventoryService.adjustStock(1L, adjustRequest);

            assertEquals(15L, mockInventory.getQuantity());
        }

        @Test
        @DisplayName("TC-INV-07 [Negative]: Throw BadRequestException when deducting more than current stock")
        void adjustStock_DeductMoreThanAvailable_ThrowsBadRequest() {
            adjustRequest.setAction(InventoryAction.DEDUCT);
            adjustRequest.setQuantity(100L); // 100 > 50

            when(inventoryRepository.findById(1L)).thenReturn(Optional.of(mockInventory));

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    inventoryService.adjustStock(1L, adjustRequest)
            );

            assertThat(ex.getMessage()).contains("Cannot deduct more than the current stock");
            verify(inventoryRepository, never()).save(any());
            verify(inventoryHistoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-INV-08 [Negative]: Throw ResourceNotFoundException when inventory ID does not exist")
        void adjustStock_NotFound_ThrowsResourceNotFoundException() {
            when(inventoryRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    inventoryService.adjustStock(999L, adjustRequest)
            );

            verify(inventoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-INV-09 [Negative/Boundary]: Throw BadRequestException when inventory action is invalid or null")
        void adjustStock_InvalidAction_ThrowsBadRequestException() {
            adjustRequest.setAction(null);
            when(inventoryRepository.findById(1L)).thenReturn(Optional.of(mockInventory));

            assertThrows(BadRequestException.class, () ->
                    inventoryService.adjustStock(1L, adjustRequest)
            );
        }
    }

    // =========================================================================
    // AC-INV-03: Lịch sử biến động tồn kho (getInventoryHistory)
    // =========================================================================
    @Nested
    @DisplayName("AC-INV-03: Get Inventory History Tests")
    class GetInventoryHistoryTests {

        @Test
        @DisplayName("TC-INV-10 [Positive]: Return history entries ordered by date desc")
        void getInventoryHistory_Success() {
            InventoryHistory history = InventoryHistory.builder().id(10L).action(InventoryAction.ADD).build();
            InventoryHistoryResponse historyResponse = new InventoryHistoryResponse();

            when(inventoryRepository.existsById(1L)).thenReturn(true);
            when(inventoryHistoryRepository.findByInventoryIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(history));
            when(inventoryMapper.toHistoryResponse(history)).thenReturn(historyResponse);

            List<InventoryHistoryResponse> responses = inventoryService.getInventoryHistory(1L);

            assertNotNull(responses);
            assertEquals(1, responses.size());
            verify(inventoryHistoryRepository, times(1)).findByInventoryIdOrderByCreatedAtDesc(1L);
        }

        @Test
        @DisplayName("TC-INV-11 [Negative]: Throw ResourceNotFoundException when inventory does not exist")
        void getInventoryHistory_NotFound_ThrowsResourceNotFoundException() {
            when(inventoryRepository.existsById(999L)).thenReturn(false);

            assertThrows(ResourceNotFoundException.class, () ->
                    inventoryService.getInventoryHistory(999L)
            );

            verify(inventoryHistoryRepository, never()).findByInventoryIdOrderByCreatedAtDesc(anyLong());
        }
    }
}
