package com.ecommerce.backend.modules.order.controller;

import com.ecommerce.backend.modules.order.dto.request.CancelOrderRequest;
import com.ecommerce.backend.modules.order.dto.response.BuyAgainResponse;
import com.ecommerce.backend.modules.order.dto.response.OrderDetailUserResponse;
import com.ecommerce.backend.modules.order.dto.response.OrderListUserResponse;
import com.ecommerce.backend.modules.order.dto.response.OrderPaymentResponse;
import com.ecommerce.backend.modules.order.enums.OrderStatus;
import com.ecommerce.backend.modules.order.enums.PaymentStatus;
import com.ecommerce.backend.modules.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderController Unit Test Suite")
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private OrderController orderController;

    private Long userId;

    @BeforeEach
    void setUp() {
        userId = 100L;
        lenient().when(jwt.getSubject()).thenReturn(String.valueOf(userId));
    }

    @Test
    @DisplayName("GET /api/orders returns list of user orders")
    void getMyOrders_ReturnsOk() {
        OrderListUserResponse orderResponse = OrderListUserResponse.builder()
                .id(1L)
                .orderDate(Instant.now())
                .totalPrice(BigDecimal.valueOf(250.0))
                .orderStatus(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .build();
        Page<OrderListUserResponse> page = new PageImpl<>(List.of(orderResponse));

        when(orderService.getMyOrders(eq(userId), isNull(), any(Pageable.class))).thenReturn(page);

        ResponseEntity<Page<OrderListUserResponse>> response = orderController.getMyOrders(
                jwt, null, 0, 10, "createdAt", "desc"
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getTotalElements());
        verify(orderService, times(1)).getMyOrders(eq(userId), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/orders/{id} returns order details")
    void getMyOrderDetail_ReturnsOk() {
        OrderDetailUserResponse detail = OrderDetailUserResponse.builder()
                .id(1L)
                .recipientName("Alice")
                .totalPrice(BigDecimal.valueOf(100.0))
                .build();

        when(orderService.getMyOrderDetail(userId, 1L)).thenReturn(detail);

        ResponseEntity<OrderDetailUserResponse> response = orderController.getMyOrderDetail(jwt, 1L);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Alice", response.getBody().getRecipientName());
        verify(orderService, times(1)).getMyOrderDetail(userId, 1L);
    }

    @Test
    @DisplayName("POST /api/orders/{id}/buy-again returns BuyAgainResponse")
    void buyAgain_ReturnsOk() {
        BuyAgainResponse buyAgainResponse = BuyAgainResponse.builder()
                .message("Items have been added to your cart.")
                .addedItemsCount(2)
                .unavailableItemsCount(0)
                .build();

        when(orderService.buyAgain(userId, 1L)).thenReturn(buyAgainResponse);

        ResponseEntity<BuyAgainResponse> response = orderController.buyAgain(jwt, 1L);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Items have been added to your cart.", response.getBody().getMessage());
        verify(orderService, times(1)).buyAgain(userId, 1L);
    }

    @Test
    @DisplayName("PUT /api/orders/{id}/cancel returns updated OrderDetailUserResponse")
    void cancelMyOrder_ReturnsOk() {
        CancelOrderRequest request = CancelOrderRequest.builder().reason("No longer needed").build();
        OrderDetailUserResponse cancelledDetail = OrderDetailUserResponse.builder()
                .id(1L)
                .orderStatus(OrderStatus.CANCELLED)
                .cancelReason("No longer needed")
                .build();

        when(orderService.cancelMyOrder(userId, 1L, request)).thenReturn(cancelledDetail);

        ResponseEntity<OrderDetailUserResponse> response = orderController.cancelMyOrder(jwt, 1L, request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(OrderStatus.CANCELLED, response.getBody().getOrderStatus());
        verify(orderService, times(1)).cancelMyOrder(userId, 1L, request);
    }

    @Test
    @DisplayName("POST /api/orders/{id}/pay returns OrderPaymentResponse")
    void payOrder_ReturnsOk() {
        OrderPaymentResponse paymentResponse = OrderPaymentResponse.builder()
                .orderId(1L)
                .paymentStatus(PaymentStatus.UNPAID)
                .totalPrice(BigDecimal.valueOf(150.0))
                .message("Payment checkout initiated successfully.")
                .paymentUrl("/checkout?orderId=1")
                .build();

        when(orderService.payOrder(userId, 1L)).thenReturn(paymentResponse);

        ResponseEntity<OrderPaymentResponse> response = orderController.payOrder(jwt, 1L);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("/checkout?orderId=1", response.getBody().getPaymentUrl());
        verify(orderService, times(1)).payOrder(userId, 1L);
    }
}
