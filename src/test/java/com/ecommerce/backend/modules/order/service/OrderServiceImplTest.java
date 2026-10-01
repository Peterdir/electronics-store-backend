package com.ecommerce.backend.modules.order.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.modules.auth.entity.User;
import com.ecommerce.backend.modules.auth.enums.UserStatus;
import com.ecommerce.backend.modules.auth.repository.UserRepository;
import com.ecommerce.backend.modules.cart.dto.response.CartResponse;
import com.ecommerce.backend.modules.cart.entity.Cart;
import com.ecommerce.backend.modules.cart.entity.CartItem;
import com.ecommerce.backend.modules.cart.repository.CartRepository;
import com.ecommerce.backend.modules.cart.service.CartService;
import com.ecommerce.backend.modules.inventory.entity.Inventory;
import com.ecommerce.backend.modules.inventory.entity.InventoryHistory;
import com.ecommerce.backend.modules.inventory.repository.InventoryHistoryRepository;
import com.ecommerce.backend.modules.inventory.repository.InventoryRepository;
import com.ecommerce.backend.modules.order.dto.request.CancelOrderRequest;
import com.ecommerce.backend.modules.order.dto.response.BuyAgainResponse;
import com.ecommerce.backend.modules.order.dto.response.OrderDetailUserResponse;
import com.ecommerce.backend.modules.order.dto.response.OrderListUserResponse;
import com.ecommerce.backend.modules.order.dto.response.OrderPaymentResponse;
import com.ecommerce.backend.modules.order.entity.Order;
import com.ecommerce.backend.modules.order.entity.OrderItem;
import com.ecommerce.backend.modules.order.enums.OrderStatus;
import com.ecommerce.backend.modules.order.enums.PaymentMethod;
import com.ecommerce.backend.modules.order.enums.PaymentStatus;
import com.ecommerce.backend.modules.order.repository.OrderRepository;
import com.ecommerce.backend.modules.product.entity.Product;
import com.ecommerce.backend.modules.product.entity.ProductVariant;
import com.ecommerce.backend.modules.product.enums.ProductStatus;
import com.ecommerce.backend.modules.product.repository.ProductVariantRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl Unit Test Suite")
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryHistoryRepository inventoryHistoryRepository;

    @Mock
    private CartService cartService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User user;
    private User otherUser;
    private Product product;
    private ProductVariant variant1;
    private ProductVariant variant2;
    private Inventory inventory1;
    private Inventory inventory2;
    private Order order;
    private OrderItem item1;
    private OrderItem item2;
    private Cart cart;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("user@example.com")
                .fullName("John Doe")
                .status(UserStatus.ACTIVE)
                .build();

        otherUser = User.builder()
                .id(2L)
                .email("other@example.com")
                .fullName("Jane Smith")
                .build();

        product = Product.builder()
                .id(10L)
                .name("Smartphone Pro")
                .status(ProductStatus.ACTIVE)
                .build();

        inventory1 = Inventory.builder()
                .id(100L)
                .quantity(50L)
                .build();

        variant1 = ProductVariant.builder()
                .id(101L)
                .sku("PHONE-PRO-BLK")
                .price(BigDecimal.valueOf(999.99))
                .status(ProductStatus.ACTIVE)
                .product(product)
                .inventory(inventory1)
                .build();

        inventory2 = Inventory.builder()
                .id(200L)
                .quantity(20L)
                .build();

        variant2 = ProductVariant.builder()
                .id(102L)
                .sku("PHONE-PRO-BLU")
                .price(BigDecimal.valueOf(1049.99))
                .status(ProductStatus.ACTIVE)
                .product(product)
                .inventory(inventory2)
                .build();

        item1 = OrderItem.builder()
                .id(1001L)
                .productVariant(variant1)
                .productName("Smartphone Pro - Black")
                .variantSku("PHONE-PRO-BLK")
                .thumbnailUrl("https://example.com/img1.jpg")
                .unitPrice(BigDecimal.valueOf(999.99))
                .quantity(1)
                .subtotal(BigDecimal.valueOf(999.99))
                .build();

        item2 = OrderItem.builder()
                .id(1002L)
                .productVariant(variant2)
                .productName("Smartphone Pro - Blue")
                .variantSku("PHONE-PRO-BLU")
                .thumbnailUrl("https://example.com/img2.jpg")
                .unitPrice(BigDecimal.valueOf(1049.99))
                .quantity(2)
                .subtotal(BigDecimal.valueOf(2099.98))
                .build();

        List<OrderItem> items = new ArrayList<>(List.of(item1, item2));

        order = Order.builder()
                .id(5001L)
                .user(user)
                .recipientName("John Doe")
                .recipientPhone("0987654321")
                .shippingAddress("123 Main St, Hanoi")
                .subtotal(BigDecimal.valueOf(3099.97))
                .shippingFee(BigDecimal.valueOf(30.00))
                .couponCode("SAVE10")
                .discountAmount(BigDecimal.valueOf(100.00))
                .totalPrice(BigDecimal.valueOf(3029.97))
                .orderStatus(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .paymentMethod(PaymentMethod.VNPAY)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .items(items)
                .build();

        item1.setOrder(order);
        item2.setOrder(order);

        cart = Cart.builder()
                .id(901L)
                .user(user)
                .items(new ArrayList<>())
                .build();
    }

    // =========================================================================
    // AC-ORDER-01: Main Flow & Exception E1: View Order History (getMyOrders)
    // =========================================================================
    @Nested
    @DisplayName("AC-ORDER-01: Get My Orders Tests")
    class GetMyOrdersTests {

        @Test
        @DisplayName("TC-ORDER-01 [Positive]: Get user order history without status filter returns sorted paged orders")
        void getMyOrders_WithoutFilter_Success() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> orderPage = new PageImpl<>(List.of(order), pageable, 1);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(orderRepository.findByUserIdAndOrderStatus(1L, null, pageable)).thenReturn(orderPage);

            Page<OrderListUserResponse> result = orderService.getMyOrders(1L, null, pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            OrderListUserResponse orderResponse = result.getContent().get(0);
            assertEquals(5001L, orderResponse.getId());
            assertEquals(BigDecimal.valueOf(3029.97), orderResponse.getTotalPrice());
            assertEquals(OrderStatus.PENDING, orderResponse.getOrderStatus());
            assertEquals(PaymentStatus.UNPAID, orderResponse.getPaymentStatus());
            assertEquals(3, orderResponse.getTotalItems());
            assertTrue(orderResponse.getCanCancel());
            assertTrue(orderResponse.getCanPay());
            assertFalse(orderResponse.getCanBuyAgain());
            assertEquals(2, orderResponse.getItems().size());
        }

        @Test
        @DisplayName("TC-ORDER-02 [Positive]: Get user order history filtered by OrderStatus tab")
        void getMyOrders_WithStatusFilter_Success() {
            Pageable pageable = PageRequest.of(0, 10);
            order.setOrderStatus(OrderStatus.DELIVERED);
            order.setPaymentStatus(PaymentStatus.PAID);
            Page<Order> orderPage = new PageImpl<>(List.of(order), pageable, 1);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(orderRepository.findByUserIdAndOrderStatus(1L, OrderStatus.DELIVERED, pageable)).thenReturn(orderPage);

            Page<OrderListUserResponse> result = orderService.getMyOrders(1L, OrderStatus.DELIVERED, pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            OrderListUserResponse orderResponse = result.getContent().get(0);
            assertEquals(OrderStatus.DELIVERED, orderResponse.getOrderStatus());
            assertFalse(orderResponse.getCanCancel());
            assertFalse(orderResponse.getCanPay());
            assertTrue(orderResponse.getCanBuyAgain());
        }

        @Test
        @DisplayName("TC-ORDER-03 [Boundary - E1]: User with no orders receives empty page")
        void getMyOrders_EmptyHistory_ReturnsEmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(orderRepository.findByUserIdAndOrderStatus(1L, null, pageable)).thenReturn(emptyPage);

            Page<OrderListUserResponse> result = orderService.getMyOrders(1L, null, pageable);

            assertNotNull(result);
            assertEquals(0, result.getTotalElements());
            assertTrue(result.getContent().isEmpty());
        }

        @Test
        @DisplayName("TC-ORDER-03B [Negative]: User account is INACTIVE throws BadRequestException")
        void getMyOrders_InactiveAccount_ThrowsBadRequestException() {
            Pageable pageable = PageRequest.of(0, 10);
            user.setStatus(UserStatus.INACTIVE);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    orderService.getMyOrders(1L, null, pageable)
            );
            assertEquals("This account has been locked. Please contact support", ex.getMessage());
            verify(orderRepository, never()).findByUserIdAndOrderStatus(any(), any(), any());
        }

        @Test
        @DisplayName("TC-ORDER-03C [Negative]: User not found throws ResourceNotFoundException")
        void getMyOrders_UserNotFound_ThrowsResourceNotFoundException() {
            Pageable pageable = PageRequest.of(0, 10);
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    orderService.getMyOrders(99L, null, pageable)
            );
        }
    }

    // =========================================================================
    // AC-ORDER-02: Flow A1 & Exception E3: View Order Details (getMyOrderDetail)
    // =========================================================================
    @Nested
    @DisplayName("AC-ORDER-02: Get Order Detail Tests")
    class GetOrderDetailTests {

        @Test
        @DisplayName("TC-ORDER-04 [Positive]: Actor views detail of own order successfully")
        void getMyOrderDetail_OwnerUser_Success() {
            when(orderRepository.findById(5001L)).thenReturn(Optional.of(order));

            OrderDetailUserResponse detail = orderService.getMyOrderDetail(1L, 5001L);

            assertNotNull(detail);
            assertEquals(5001L, detail.getId());
            assertEquals("John Doe", detail.getRecipientName());
            assertEquals("0987654321", detail.getRecipientPhone());
            assertEquals("123 Main St, Hanoi", detail.getShippingAddress());
            assertEquals("SAVE10", detail.getCouponCode());
            assertEquals(BigDecimal.valueOf(100.00), detail.getDiscountAmount());
            assertEquals(BigDecimal.valueOf(3029.97), detail.getTotalPrice());
            assertEquals(2, detail.getItems().size());
            assertTrue(detail.getCanCancel());
            assertTrue(detail.getCanPay());
        }

        @Test
        @DisplayName("TC-ORDER-05 [Negative - E3]: Actor attempts to view order belonging to another user throws BadRequestException")
        void getMyOrderDetail_DifferentUser_ThrowsPermissionDenied() {
            when(orderRepository.findById(5001L)).thenReturn(Optional.of(order));

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    orderService.getMyOrderDetail(2L, 5001L)
            );
            assertEquals("You do not have permission to view this order.", ex.getMessage());
        }

        @Test
        @DisplayName("TC-ORDER-06 [Negative]: Non-existent order throws ResourceNotFoundException")
        void getMyOrderDetail_NotFound_ThrowsException() {
            when(orderRepository.findById(9999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    orderService.getMyOrderDetail(1L, 9999L)
            );
        }
    }

    // =========================================================================
    // AC-ORDER-03: Flow A2 & Exception E2: Buy Again (buyAgain)
    // =========================================================================
    @Nested
    @DisplayName("AC-ORDER-03: Buy Again Tests")
    class BuyAgainTests {

        @Test
        @DisplayName("TC-ORDER-07 [Positive - A2]: All products in old order are active and in stock -> added to cart successfully")
        void buyAgain_AllAvailable_Success() {
            when(orderRepository.findById(5001L)).thenReturn(Optional.of(order));
            when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
            when(productVariantRepository.findById(101L)).thenReturn(Optional.of(variant1));
            when(productVariantRepository.findById(102L)).thenReturn(Optional.of(variant2));
            when(cartRepository.save(any(Cart.class))).thenReturn(cart);

            CartResponse mockCartResponse = CartResponse.builder()
                    .id(cart.getId())
                    .totalItems(3)
                    .totalPrice(BigDecimal.valueOf(3099.97))
                    .build();
            when(cartService.getCart(1L)).thenReturn(mockCartResponse);

            BuyAgainResponse response = orderService.buyAgain(1L, 5001L);

            assertNotNull(response);
            assertEquals("Items have been added to your cart.", response.getMessage());
            assertEquals(2, response.getAddedItemsCount());
            assertEquals(0, response.getUnavailableItemsCount());
            assertEquals(2, cart.getItems().size());
            verify(cartRepository, times(1)).save(cart);
        }

        @Test
        @DisplayName("TC-ORDER-08 [Positive]: Item already in cart has quantity increased up to available stock")
        void buyAgain_ItemAlreadyInCart_IncrementsQuantity() {
            CartItem existingCartItem = CartItem.builder()
                    .cart(cart)
                    .productVariant(variant1)
                    .quantity(2)
                    .build();
            cart.getItems().add(existingCartItem);

            when(orderRepository.findById(5001L)).thenReturn(Optional.of(order));
            when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
            when(productVariantRepository.findById(101L)).thenReturn(Optional.of(variant1));
            when(productVariantRepository.findById(102L)).thenReturn(Optional.of(variant2));
            when(cartRepository.save(any(Cart.class))).thenReturn(cart);
            when(cartService.getCart(1L)).thenReturn(CartResponse.builder().id(cart.getId()).build());

            BuyAgainResponse response = orderService.buyAgain(1L, 5001L);

            assertNotNull(response);
            assertEquals(2, response.getAddedItemsCount());
            assertEquals(3, existingCartItem.getQuantity()); // 2 + 1
        }

        @Test
        @DisplayName("TC-ORDER-09 [Negative/Partial - E2]: Some products out of stock or inactive -> adds only available items and shows warning")
        void buyAgain_SomeItemsUnavailable_ReturnsWarningMessage() {
            variant2.setStatus(ProductStatus.INACTIVE); // Inactive variant

            when(orderRepository.findById(5001L)).thenReturn(Optional.of(order));
            when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
            when(productVariantRepository.findById(101L)).thenReturn(Optional.of(variant1));
            when(productVariantRepository.findById(102L)).thenReturn(Optional.of(variant2));
            when(cartRepository.save(any(Cart.class))).thenReturn(cart);
            when(cartService.getCart(1L)).thenReturn(CartResponse.builder().id(cart.getId()).build());

            BuyAgainResponse response = orderService.buyAgain(1L, 5001L);

            assertNotNull(response);
            assertEquals("Some items from your previous order are currently unavailable and could not be added to your cart.", response.getMessage());
            assertEquals(1, response.getAddedItemsCount());
            assertEquals(1, response.getUnavailableItemsCount());
            assertThat(response.getAddedItemNames()).contains("Smartphone Pro - Black");
            assertThat(response.getUnavailableItemNames()).contains("Smartphone Pro - Blue");
        }

        @Test
        @DisplayName("TC-ORDER-10 [Negative - E2]: All items out of stock -> adds nothing and displays warning")
        void buyAgain_AllItemsOutOfStock_ReturnsWarning() {
            inventory1.setQuantity(0L);
            inventory2.setQuantity(0L);

            when(orderRepository.findById(5001L)).thenReturn(Optional.of(order));
            when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
            when(productVariantRepository.findById(101L)).thenReturn(Optional.of(variant1));
            when(productVariantRepository.findById(102L)).thenReturn(Optional.of(variant2));
            when(cartRepository.save(any(Cart.class))).thenReturn(cart);
            when(cartService.getCart(1L)).thenReturn(CartResponse.builder().id(cart.getId()).build());

            BuyAgainResponse response = orderService.buyAgain(1L, 5001L);

            assertNotNull(response);
            assertEquals("Some items from your previous order are currently unavailable and could not be added to your cart.", response.getMessage());
            assertEquals(0, response.getAddedItemsCount());
            assertEquals(2, response.getUnavailableItemsCount());
            assertTrue(cart.getItems().isEmpty());
        }

        @Test
        @DisplayName("TC-ORDER-11 [Negative - E3]: Buy again on another user's order throws BadRequestException")
        void buyAgain_UnauthorizedOrder_ThrowsException() {
            when(orderRepository.findById(5001L)).thenReturn(Optional.of(order));

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    orderService.buyAgain(2L, 5001L)
            );
            assertEquals("You do not have permission to view this order.", ex.getMessage());
            verify(cartRepository, never()).save(any(Cart.class));
        }
    }

    // =========================================================================
    // AC-ORDER-04: Flow A4: Cancel Order (cancelMyOrder)
    // =========================================================================
    @Nested
    @DisplayName("AC-ORDER-04: Cancel Order Tests")
    class CancelOrderTests {

        private CancelOrderRequest cancelRequest;

        @BeforeEach
        void setUpCancel() {
            cancelRequest = CancelOrderRequest.builder()
                    .reason("Changed my mind, found better deal")
                    .build();
        }

        @Test
        @DisplayName("TC-ORDER-12 [Positive - A4]: Cancel pending order successfully restores inventory and logs history")
        void cancelMyOrder_PendingOrder_Success() {
            when(orderRepository.findById(5001L)).thenReturn(Optional.of(order));
            when(inventoryRepository.findByProductVariantId(101L)).thenReturn(Optional.of(inventory1));
            when(inventoryRepository.findByProductVariantId(102L)).thenReturn(Optional.of(inventory2));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            OrderDetailUserResponse response = orderService.cancelMyOrder(1L, 5001L, cancelRequest);

            assertNotNull(response);
            assertEquals(OrderStatus.CANCELLED, response.getOrderStatus());
            assertEquals("Changed my mind, found better deal", response.getCancelReason());
            assertNotNull(response.getCancelledAt());

            // Check inventory restored: variant1 (50 + 1 = 51), variant2 (20 + 2 = 22)
            assertEquals(51L, inventory1.getQuantity());
            assertEquals(22L, inventory2.getQuantity());
            verify(inventoryRepository, times(2)).save(any(Inventory.class));
            verify(inventoryHistoryRepository, times(2)).save(any(InventoryHistory.class));
            verify(orderRepository, times(1)).save(order);
        }

        @Test
        @DisplayName("TC-ORDER-13 [Negative]: Cancel order owned by someone else throws BadRequestException")
        void cancelMyOrder_Unauthorized_ThrowsException() {
            when(orderRepository.findById(5001L)).thenReturn(Optional.of(order));

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    orderService.cancelMyOrder(2L, 5001L, cancelRequest)
            );
            assertEquals("You do not have permission to cancel this order.", ex.getMessage());
            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("TC-ORDER-14 [Negative]: Cancel order already delivered throws BadRequestException")
        void cancelMyOrder_AlreadyDelivered_ThrowsException() {
            order.setOrderStatus(OrderStatus.DELIVERED);
            when(orderRepository.findById(5001L)).thenReturn(Optional.of(order));

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    orderService.cancelMyOrder(1L, 5001L, cancelRequest)
            );
            assertEquals("Cannot cancel an order that has already been delivered.", ex.getMessage());
        }

        @Test
        @DisplayName("TC-ORDER-15 [Negative]: Cancel order currently shipped throws BadRequestException")
        void cancelMyOrder_Shipped_ThrowsException() {
            order.setOrderStatus(OrderStatus.SHIPPED);
            when(orderRepository.findById(5001L)).thenReturn(Optional.of(order));

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    orderService.cancelMyOrder(1L, 5001L, cancelRequest)
            );
            assertEquals("Cannot cancel an order that is currently being shipped.", ex.getMessage());
        }

        @Test
        @DisplayName("TC-ORDER-16 [Negative]: Cancel order already cancelled throws BadRequestException")
        void cancelMyOrder_AlreadyCancelled_ThrowsException() {
            order.setOrderStatus(OrderStatus.CANCELLED);
            when(orderRepository.findById(5001L)).thenReturn(Optional.of(order));

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    orderService.cancelMyOrder(1L, 5001L, cancelRequest)
            );
            assertEquals("This order is already cancelled.", ex.getMessage());
        }
    }

    // =========================================================================
    // AC-ORDER-05: Flow A3: Pay Order (payOrder)
    // =========================================================================
    @Nested
    @DisplayName("AC-ORDER-05: Pay Order Tests")
    class PayOrderTests {

        @Test
        @DisplayName("TC-ORDER-17 [Positive - A3]: Pay for unpaid order returns payment checkout details")
        void payOrder_UnpaidOrder_Success() {
            when(orderRepository.findById(5001L)).thenReturn(Optional.of(order));

            OrderPaymentResponse response = orderService.payOrder(1L, 5001L);

            assertNotNull(response);
            assertEquals(5001L, response.getOrderId());
            assertEquals(PaymentStatus.UNPAID, response.getPaymentStatus());
            assertEquals(BigDecimal.valueOf(3029.97), response.getTotalPrice());
            assertEquals("Payment checkout initiated successfully.", response.getMessage());
            assertThat(response.getPaymentUrl()).contains("orderId=5001");
        }

        @Test
        @DisplayName("TC-ORDER-18 [Negative]: Pay for already paid order throws BadRequestException")
        void payOrder_AlreadyPaid_ThrowsException() {
            order.setPaymentStatus(PaymentStatus.PAID);
            when(orderRepository.findById(5001L)).thenReturn(Optional.of(order));

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    orderService.payOrder(1L, 5001L)
            );
            assertEquals("Order is already paid.", ex.getMessage());
        }

        @Test
        @DisplayName("TC-ORDER-19 [Negative]: Pay for cancelled order throws BadRequestException")
        void payOrder_CancelledOrder_ThrowsException() {
            order.setOrderStatus(OrderStatus.CANCELLED);
            when(orderRepository.findById(5001L)).thenReturn(Optional.of(order));

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    orderService.payOrder(1L, 5001L)
            );
            assertEquals("Cannot make payment for a cancelled order.", ex.getMessage());
        }

        @Test
        @DisplayName("TC-ORDER-20 [Negative]: Pay for another user's order throws BadRequestException")
        void payOrder_Unauthorized_ThrowsException() {
            when(orderRepository.findById(5001L)).thenReturn(Optional.of(order));

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    orderService.payOrder(2L, 5001L)
            );
            assertEquals("You do not have permission to pay for this order.", ex.getMessage());
        }
    }
}
