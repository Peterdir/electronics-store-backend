package com.ecommerce.backend.modules.order.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.modules.auth.entity.User;
import com.ecommerce.backend.modules.inventory.entity.Inventory;
import com.ecommerce.backend.modules.inventory.entity.InventoryHistory;
import com.ecommerce.backend.modules.inventory.repository.InventoryHistoryRepository;
import com.ecommerce.backend.modules.inventory.repository.InventoryRepository;
import com.ecommerce.backend.modules.order.dto.request.CancelOrderRequest;
import com.ecommerce.backend.modules.order.dto.request.UpdateOrderStatusRequest;
import com.ecommerce.backend.modules.order.dto.response.OrderDetailAdminResponse;
import com.ecommerce.backend.modules.order.dto.response.OrderListAdminResponse;
import com.ecommerce.backend.modules.order.entity.Order;
import com.ecommerce.backend.modules.order.entity.OrderItem;
import com.ecommerce.backend.modules.order.enums.OrderStatus;
import com.ecommerce.backend.modules.order.enums.PaymentMethod;
import com.ecommerce.backend.modules.order.enums.PaymentStatus;
import com.ecommerce.backend.modules.order.repository.OrderRepository;
import com.ecommerce.backend.modules.product.entity.ProductVariant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderAdminServiceImpl Unit Test Suite")
class OrderAdminServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryHistoryRepository inventoryHistoryRepository;

    @InjectMocks
    private OrderAdminServiceImpl orderAdminService;

    private User user;
    private Order order;
    private OrderItem item;
    private ProductVariant variant;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("admin_user@example.com")
                .fullName("Admin Customer")
                .build();

        variant = ProductVariant.builder()
                .id(101L)
                .sku("SKU-001")
                .price(BigDecimal.valueOf(100.0))
                .build();

        inventory = Inventory.builder()
                .id(201L)
                .quantity(10L)
                .build();

        item = OrderItem.builder()
                .id(301L)
                .productVariant(variant)
                .productName("Sample Product")
                .unitPrice(BigDecimal.valueOf(100.0))
                .quantity(2)
                .subtotal(BigDecimal.valueOf(200.0))
                .build();

        order = Order.builder()
                .id(401L)
                .user(user)
                .recipientName("Recipient")
                .recipientPhone("0123456789")
                .shippingAddress("Address")
                .subtotal(BigDecimal.valueOf(200.0))
                .totalPrice(BigDecimal.valueOf(200.0))
                .orderStatus(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PAID)
                .paymentMethod(PaymentMethod.COD)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .items(new ArrayList<>(List.of(item)))
                .build();

        item.setOrder(order);
    }

    @Nested
    @DisplayName("Admin Get Orders Tests")
    class GetOrdersTests {
        @Test
        @DisplayName("Search admin orders returns mapped page")
        void getOrders_Success() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> orderPage = new PageImpl<>(List.of(order), pageable, 1);
            when(orderRepository.searchOrdersAdmin("", null, null, pageable)).thenReturn(orderPage);

            Page<OrderListAdminResponse> result = orderAdminService.getOrders("", null, null, pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals(401L, result.getContent().get(0).getId());
        }
    }

    @Nested
    @DisplayName("Admin Get Order Detail Tests")
    class GetOrderDetailTests {
        @Test
        @DisplayName("Get order detail success")
        void getOrderDetail_Success() {
            when(orderRepository.findById(401L)).thenReturn(Optional.of(order));

            OrderDetailAdminResponse detail = orderAdminService.getOrderDetail(401L);

            assertNotNull(detail);
            assertEquals(401L, detail.getId());
            assertEquals("Admin Customer", detail.getCustomerName());
        }

        @Test
        @DisplayName("Get order detail not found throws ResourceNotFoundException")
        void getOrderDetail_NotFound() {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> orderAdminService.getOrderDetail(999L));
        }
    }

    @Nested
    @DisplayName("Admin Update Order Status Tests")
    class UpdateOrderStatusTests {
        @Test
        @DisplayName("Valid transition updates status successfully")
        void updateOrderStatus_Valid_Success() {
            when(orderRepository.findById(401L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

            UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                    .newStatus(OrderStatus.PROCESSING)
                    .build();

            OrderDetailAdminResponse response = orderAdminService.updateOrderStatus(401L, request);

            assertNotNull(response);
            assertEquals(OrderStatus.PROCESSING, response.getOrderStatus());
        }

        @Test
        @DisplayName("Invalid transition throws BadRequestException")
        void updateOrderStatus_Invalid_ThrowsBadRequest() {
            when(orderRepository.findById(401L)).thenReturn(Optional.of(order));

            UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                    .newStatus(OrderStatus.DELIVERED) // Cannot jump from PENDING to DELIVERED
                    .build();

            assertThrows(BadRequestException.class, () -> orderAdminService.updateOrderStatus(401L, request));
        }

        @Test
        @DisplayName("Transition to CANCELLED restores stock")
        void updateOrderStatus_ToCancelled_RestoresStock() {
            when(orderRepository.findById(401L)).thenReturn(Optional.of(order));
            when(inventoryRepository.findByProductVariantId(101L)).thenReturn(Optional.of(inventory));
            when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

            UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                    .newStatus(OrderStatus.CANCELLED)
                    .build();

            OrderDetailAdminResponse response = orderAdminService.updateOrderStatus(401L, request);

            assertNotNull(response);
            assertEquals(OrderStatus.CANCELLED, response.getOrderStatus());
            assertEquals(12L, inventory.getQuantity()); // 10 + 2
            verify(inventoryRepository, times(1)).save(inventory);
            verify(inventoryHistoryRepository, times(1)).save(any(InventoryHistory.class));
        }
    }

    @Nested
    @DisplayName("Admin Cancel Order Tests")
    class CancelOrderTests {
        @Test
        @DisplayName("Cancel order success restores inventory and sets reason")
        void cancelOrder_Success() {
            when(orderRepository.findById(401L)).thenReturn(Optional.of(order));
            when(inventoryRepository.findByProductVariantId(101L)).thenReturn(Optional.of(inventory));
            when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

            CancelOrderRequest request = CancelOrderRequest.builder()
                    .reason("Customer requested refund")
                    .build();

            OrderDetailAdminResponse response = orderAdminService.cancelOrder(401L, request);

            assertNotNull(response);
            assertEquals(OrderStatus.CANCELLED, response.getOrderStatus());
            assertEquals("Customer requested refund", response.getCancelReason());
            assertEquals(12L, inventory.getQuantity());
            verify(inventoryRepository, times(1)).save(inventory);
            verify(inventoryHistoryRepository, times(1)).save(any(InventoryHistory.class));
        }
    }
}
