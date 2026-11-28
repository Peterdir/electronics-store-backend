package com.ecommerce.backend.modules.order.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.modules.address.entity.Address;
import com.ecommerce.backend.modules.address.repository.AddressRepository;
import com.ecommerce.backend.modules.auth.entity.User;
import com.ecommerce.backend.modules.auth.enums.UserStatus;
import com.ecommerce.backend.modules.auth.repository.UserRepository;
import com.ecommerce.backend.modules.cart.entity.Cart;
import com.ecommerce.backend.modules.cart.entity.CartItem;
import com.ecommerce.backend.modules.cart.repository.CartRepository;
import com.ecommerce.backend.modules.coupon.entity.Coupon;
import com.ecommerce.backend.modules.coupon.enums.DiscountType;
import com.ecommerce.backend.modules.coupon.repository.CouponRepository;
import com.ecommerce.backend.modules.inventory.entity.Inventory;
import com.ecommerce.backend.modules.inventory.entity.InventoryHistory;
import com.ecommerce.backend.modules.inventory.enums.InventoryAction;
import com.ecommerce.backend.modules.inventory.repository.InventoryHistoryRepository;
import com.ecommerce.backend.modules.inventory.repository.InventoryRepository;
import com.ecommerce.backend.modules.order.dto.request.CheckoutItem;
import com.ecommerce.backend.modules.order.dto.request.CheckoutRequest;
import com.ecommerce.backend.modules.order.dto.response.CheckoutResponse;
import com.ecommerce.backend.modules.order.entity.Order;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CheckoutServiceImpl Unit Test Suite")
class CheckoutServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryHistoryRepository inventoryHistoryRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private CheckoutServiceImpl checkoutService;

    private User user;
    private User otherUser;
    private Address address;
    private Product product;
    private ProductVariant variant1;
    private ProductVariant variant2;
    private Inventory inventory1;
    private Inventory inventory2;
    private Cart cart;
    private Coupon percentageCoupon;
    private Coupon fixedCoupon;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("customer@example.com")
                .fullName("Nguyen Van A")
                .status(UserStatus.ACTIVE)
                .build();

        otherUser = User.builder()
                .id(2L)
                .email("other@example.com")
                .fullName("Tran Thi B")
                .status(UserStatus.ACTIVE)
                .build();

        address = Address.builder()
                .id(10L)
                .user(user)
                .fullName("Nguyen Van A")
                .phone("0901234567")
                .specificAddress("123 Le Loi")
                .ward("")
                .district("Quan 1")
                .province("TP.HCM")
                .isDefault(true)
                .build();

        product = Product.builder()
                .id(100L)
                .name("iPhone 16 Pro")
                .status(ProductStatus.ACTIVE)
                .build();

        variant1 = ProductVariant.builder()
                .id(1001L)
                .product(product)
                .sku("IP16P-256-BLK")
                .price(new BigDecimal("100000"))
                .imageUrl("/uploads/products/ip16.jpg")
                .status(ProductStatus.ACTIVE)
                .build();

        variant2 = ProductVariant.builder()
                .id(1002L)
                .product(product)
                .sku("IP16P-512-WHT")
                .price(new BigDecimal("150000"))
                .imageUrl("/uploads/products/ip16-wht.jpg")
                .status(ProductStatus.ACTIVE)
                .build();

        inventory1 = Inventory.builder()
                .id(2001L)
                .productVariant(variant1)
                .quantity(10L)
                .build();

        inventory2 = Inventory.builder()
                .id(2002L)
                .productVariant(variant2)
                .quantity(5L)
                .build();

        cart = Cart.builder()
                .id(300L)
                .user(user)
                .items(new ArrayList<>(List.of(
                        CartItem.builder().id(1L).productVariant(variant1).quantity(2).build()
                )))
                .build();

        Instant now = Instant.now();
        percentageCoupon = Coupon.builder()
                .id(50L)
                .code("SALE15")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(15.0)
                .minOrderValue(new BigDecimal("100000"))
                .usageLimit(100)
                .usedCount(10)
                .startDate(now.minus(1, ChronoUnit.DAYS))
                .endDate(now.plus(7, ChronoUnit.DAYS))
                .isActive(true)
                .build();

        fixedCoupon = Coupon.builder()
                .id(51L)
                .code("GIAM300K")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(300000.0)
                .minOrderValue(new BigDecimal("50000"))
                .usageLimit(10)
                .usedCount(1)
                .startDate(now.minus(1, ChronoUnit.DAYS))
                .endDate(now.plus(7, ChronoUnit.DAYS))
                .isActive(true)
                .build();
    }

    private void stubOrderSave() {
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(999L);
            return o;
        });
    }

    private CheckoutRequest manualCodRequest() {
        return CheckoutRequest.builder()
                .recipientName("Nguyen Van A")
                .recipientPhone("0901234567")
                .shippingAddress("456 Nguyen Hue, Quan 1")
                .shippingMethod("STANDARD")
                .paymentMethod(PaymentMethod.COD)
                .build();
    }

    @Nested
    @DisplayName("Registered User Cart Checkout Tests")
    class RegisteredCartCheckoutTests {

        @Test
        @DisplayName("Should place COD order using saved address, deduct stock, log history, and clear cart")
        void placeOrder_CartWithSavedAddressCod_Success() {
            CheckoutRequest request = CheckoutRequest.builder()
                    .addressId(10L)
                    .shippingMethod("STANDARD")
                    .paymentMethod(PaymentMethod.COD)
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(addressRepository.findById(10L)).thenReturn(Optional.of(address));
            when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
            when(inventoryRepository.findByProductVariantId(1001L)).thenReturn(Optional.of(inventory1));
            when(inventoryRepository.findByProductVariantIdForUpdate(1001L)).thenReturn(Optional.of(inventory1));
            stubOrderSave();

            CheckoutResponse response = checkoutService.placeOrder(1L, request);

            assertNotNull(response);
            assertEquals(999L, response.getOrderId());
            assertEquals("123 Le Loi, Quan 1, TP.HCM", response.getShippingAddress());
            assertEquals(new BigDecimal("200000"), response.getSubtotal());
            assertEquals(new BigDecimal("30000"), response.getShippingFee());
            assertEquals(BigDecimal.ZERO, response.getDiscountAmount());
            assertEquals(new BigDecimal("230000"), response.getTotalPrice());
            assertEquals(OrderStatus.PENDING, response.getOrderStatus());
            assertEquals(PaymentStatus.UNPAID, response.getPaymentStatus());
            assertEquals(PaymentMethod.COD, response.getPaymentMethod());
            assertEquals("Order placed successfully!", response.getMessage());
            assertNull(response.getPaymentUrl());
            assertFalse(response.getIsGuestOrder());
            assertEquals(1, response.getItems().size());

            // Tồn kho giảm từ 10 xuống 8 và lưu lịch sử kho
            assertEquals(8L, inventory1.getQuantity());
            verify(inventoryRepository).save(inventory1);

            ArgumentCaptor<InventoryHistory> historyCaptor = ArgumentCaptor.forClass(InventoryHistory.class);
            verify(inventoryHistoryRepository).save(historyCaptor.capture());
            InventoryHistory history = historyCaptor.getValue();
            assertEquals(InventoryAction.DEDUCT, history.getAction());
            assertEquals(2L, history.getQuantityChanged());
            assertEquals(8L, history.getFinalStock());
            assertEquals("Nguyen Van A", history.getPerformedBy());
            assertThat(history.getReason()).contains("999");

            // Giỏ hàng bị xóa sạch
            assertTrue(cart.getItems().isEmpty());
            verify(cartRepository).save(cart);

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());
            assertEquals("customer@example.com", orderCaptor.getValue().getContactEmail());
        }

        @Test
        @DisplayName("Should place VNPAY order with EXPRESS shipping, manual address, custom email, and PERCENTAGE coupon")
        void placeOrder_CartExpressVnPayWithPercentageCoupon_Success() {
            CheckoutRequest request = CheckoutRequest.builder()
                    .recipientName("Le Van B")
                    .recipientPhone("0912345678")
                    .shippingAddress("789 Hai Ba Trung")
                    .shippingMethod("express")
                    .paymentMethod(PaymentMethod.VNPAY)
                    .couponCode(" SALE15 ")
                    .contactEmail("  custom@mail.com  ")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
            when(inventoryRepository.findByProductVariantId(1001L)).thenReturn(Optional.of(inventory1));
            when(inventoryRepository.findByProductVariantIdForUpdate(1001L)).thenReturn(Optional.of(inventory1));
            when(couponRepository.findByCode("SALE15")).thenReturn(Optional.of(percentageCoupon));
            when(couponRepository.incrementUsedCountIfAvailable(anyLong(), any(Instant.class))).thenReturn(1);
            stubOrderSave();

            CheckoutResponse response = checkoutService.placeOrder(1L, request);

            assertEquals(new BigDecimal("200000"), response.getSubtotal());
            assertEquals(new BigDecimal("55000"), response.getShippingFee());
            assertEquals(new BigDecimal("30000"), response.getDiscountAmount()); // 15% of 200,000
            assertEquals(new BigDecimal("225000"), response.getTotalPrice());
            assertEquals("Order created. Redirecting to payment gateway...", response.getMessage());
            assertEquals("/api/payments/vnpay/create?orderId=999", response.getPaymentUrl());

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());
            assertEquals("custom@mail.com", orderCaptor.getValue().getContactEmail());
            verify(couponRepository).incrementUsedCountIfAvailable(anyLong(), any(Instant.class));
        }

        @Test
        @DisplayName("Should cap FIXED_AMOUNT coupon discount at subtotal and default null shippingMethod to STANDARD")
        void placeOrder_FixedCouponExceedsSubtotalAndNullShipping_CapsDiscountAtSubtotal() {
            CheckoutRequest request = CheckoutRequest.builder()
                    .recipientName("Nguyen Van A")
                    .recipientPhone("0901234567")
                    .shippingAddress("123 Le Loi")
                    .shippingMethod(null)
                    .paymentMethod(PaymentMethod.COD)
                    .couponCode("GIAM300K")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
            when(inventoryRepository.findByProductVariantId(1001L)).thenReturn(Optional.of(inventory1));
            when(inventoryRepository.findByProductVariantIdForUpdate(1001L)).thenReturn(Optional.of(inventory1));
            when(couponRepository.findByCode("GIAM300K")).thenReturn(Optional.of(fixedCoupon));
            when(couponRepository.incrementUsedCountIfAvailable(anyLong(), any(Instant.class))).thenReturn(1);
            stubOrderSave();

            CheckoutResponse response = checkoutService.placeOrder(1L, request);

            // Subtotal = 200,000 -> fixed 300,000 bị giới hạn ở 200,000; tổng = 0 + 30,000 shipping
            assertEquals(new BigDecimal("200000"), response.getDiscountAmount());
            assertEquals(new BigDecimal("30000"), response.getShippingFee());
            assertEquals(new BigDecimal("30000"), response.getTotalPrice());
        }
    }

    @Nested
    @DisplayName("Buy Now (Direct Items) Checkout Tests")
    class BuyNowCheckoutTests {

        @Test
        @DisplayName("Should merge duplicate directItems, deduct combined stock, and leave cart untouched")
        void placeOrder_DirectItemsMerged_DoesNotClearCart() {
            CheckoutRequest request = CheckoutRequest.builder()
                    .recipientName("Nguyen Van A")
                    .recipientPhone("0901234567")
                    .shippingAddress("123 Le Loi")
                    .shippingMethod("STANDARD")
                    .paymentMethod(PaymentMethod.COD)
                    .directItems(List.of(
                            CheckoutItem.builder().productVariantId(1001L).quantity(2).build(),
                            CheckoutItem.builder().productVariantId(1001L).quantity(3).build(),
                            CheckoutItem.builder().productVariantId(1002L).quantity(1).build()
                    ))
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(productVariantRepository.findById(1001L)).thenReturn(Optional.of(variant1));
            when(productVariantRepository.findById(1002L)).thenReturn(Optional.of(variant2));
            when(inventoryRepository.findByProductVariantId(1001L)).thenReturn(Optional.of(inventory1));
            when(inventoryRepository.findByProductVariantId(1002L)).thenReturn(Optional.of(inventory2));
            when(inventoryRepository.findByProductVariantIdForUpdate(1001L)).thenReturn(Optional.of(inventory1));
            when(inventoryRepository.findByProductVariantIdForUpdate(1002L)).thenReturn(Optional.of(inventory2));
            stubOrderSave();

            CheckoutResponse response = checkoutService.placeOrder(1L, request);

            // (2+3)*100,000 + 1*150,000 = 650,000
            assertEquals(new BigDecimal("650000"), response.getSubtotal());
            assertEquals(new BigDecimal("680000"), response.getTotalPrice());
            assertEquals(2, response.getItems().size());
            assertEquals(5L, inventory1.getQuantity());
            assertEquals(4L, inventory2.getQuantity());
            verifyNoInteractions(cartRepository);
        }

        @Test
        @DisplayName("Should reject merged duplicate directItems when combined quantity exceeds stock")
        void placeOrder_MergedDirectItemsExceedStock_ThrowsBadRequest() {
            CheckoutRequest request = CheckoutRequest.builder()
                    .recipientName("Nguyen Van A")
                    .recipientPhone("0901234567")
                    .shippingAddress("123 Le Loi")
                    .shippingMethod("STANDARD")
                    .paymentMethod(PaymentMethod.COD)
                    .directItems(List.of(
                            CheckoutItem.builder().productVariantId(1001L).quantity(6).build(),
                            CheckoutItem.builder().productVariantId(1001L).quantity(5).build()
                    ))
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(productVariantRepository.findById(1001L)).thenReturn(Optional.of(variant1));
            when(inventoryRepository.findByProductVariantId(1001L)).thenReturn(Optional.of(inventory1));

            assertThrows(BadRequestException.class, () -> checkoutService.placeOrder(1L, request));
            verify(orderRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Guest Checkout Tests")
    class GuestCheckoutTests {

        @Test
        @DisplayName("Should place guest order successfully with blank contactEmail converted to null")
        void placeGuestOrder_ValidRequest_Success() {
            CheckoutRequest request = CheckoutRequest.builder()
                    .recipientName("Khach Hang")
                    .recipientPhone("0988777666")
                    .shippingAddress("100 Cach Mang Thang 8")
                    .shippingMethod("EXPRESS")
                    .paymentMethod(PaymentMethod.VNPAY)
                    .contactEmail("   ")
                    .directItems(List.of(CheckoutItem.builder().productVariantId(1001L).quantity(2).build()))
                    .build();

            when(productVariantRepository.findById(1001L)).thenReturn(Optional.of(variant1));
            when(inventoryRepository.findByProductVariantId(1001L)).thenReturn(Optional.of(inventory1));
            when(inventoryRepository.findByProductVariantIdForUpdate(1001L)).thenReturn(Optional.of(inventory1));
            stubOrderSave();

            CheckoutResponse response = checkoutService.placeGuestOrder(request);

            assertTrue(response.getIsGuestOrder());
            assertEquals(new BigDecimal("255000"), response.getTotalPrice());
            assertEquals("/api/payments/vnpay/create?orderId=999", response.getPaymentUrl());

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());
            assertNull(orderCaptor.getValue().getUser());
            assertNull(orderCaptor.getValue().getContactEmail());

            ArgumentCaptor<InventoryHistory> historyCaptor = ArgumentCaptor.forClass(InventoryHistory.class);
            verify(inventoryHistoryRepository).save(historyCaptor.capture());
            assertEquals("Guest: Khach Hang", historyCaptor.getValue().getPerformedBy());
        }

        @Test
        @DisplayName("Should reject guest order when directItems is missing or empty")
        void placeGuestOrder_EmptyDirectItems_ThrowsBadRequest() {
            CheckoutRequest nullItems = manualCodRequest();
            assertThrows(BadRequestException.class, () -> checkoutService.placeGuestOrder(nullItems));

            CheckoutRequest emptyItems = manualCodRequest();
            emptyItems.setDirectItems(List.of());
            assertThrows(BadRequestException.class, () -> checkoutService.placeGuestOrder(emptyItems));
        }

        @Test
        @DisplayName("Should reject guest order when addressId is provided")
        void placeGuestOrder_WithAddressId_ThrowsAccessDenied() {
            CheckoutRequest request = CheckoutRequest.builder()
                    .addressId(10L)
                    .recipientName("Khach Hang")
                    .recipientPhone("0988777666")
                    .shippingAddress("100 CMT8")
                    .shippingMethod("STANDARD")
                    .paymentMethod(PaymentMethod.COD)
                    .directItems(List.of(CheckoutItem.builder().productVariantId(1001L).quantity(1).build()))
                    .build();

            assertThrows(AccessDeniedException.class, () -> checkoutService.placeGuestOrder(request));
        }
    }

    @Nested
    @DisplayName("Validation, Stock & Coupon Negative Tests")
    class ValidationAndNegativeTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user does not exist")
        void placeOrder_UserNotFound_ThrowsNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());
            CheckoutRequest request = manualCodRequest();

            assertThrows(ResourceNotFoundException.class, () -> checkoutService.placeOrder(99L, request));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when saved address does not exist")
        void placeOrder_AddressNotFound_ThrowsNotFound() {
            CheckoutRequest request = CheckoutRequest.builder()
                    .addressId(999L)
                    .shippingMethod("STANDARD")
                    .paymentMethod(PaymentMethod.COD)
                    .build();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(addressRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> checkoutService.placeOrder(1L, request));
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when saved address belongs to another user")
        void placeOrder_AddressOwnedByOtherUser_ThrowsAccessDenied() {
            address.setUser(otherUser);
            CheckoutRequest request = CheckoutRequest.builder()
                    .addressId(10L)
                    .shippingMethod("STANDARD")
                    .paymentMethod(PaymentMethod.COD)
                    .build();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(addressRepository.findById(10L)).thenReturn(Optional.of(address));

            assertThrows(AccessDeniedException.class, () -> checkoutService.placeOrder(1L, request));
        }

        @Test
        @DisplayName("Should throw BadRequestException when manual shipping fields are blank")
        void placeOrder_MissingManualShippingFields_ThrowsBadRequest() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            CheckoutRequest noName = manualCodRequest();
            noName.setRecipientName(" ");
            assertThrows(BadRequestException.class, () -> checkoutService.placeOrder(1L, noName));

            CheckoutRequest noPhone = manualCodRequest();
            noPhone.setRecipientPhone("");
            assertThrows(BadRequestException.class, () -> checkoutService.placeOrder(1L, noPhone));

            CheckoutRequest noAddress = manualCodRequest();
            noAddress.setShippingAddress(null);
            assertThrows(BadRequestException.class, () -> checkoutService.placeOrder(1L, noAddress));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when cart does not exist or BadRequest when empty")
        void placeOrder_CartMissingOrEmpty_ThrowsExpectedException() {
            CheckoutRequest request = manualCodRequest();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> checkoutService.placeOrder(1L, request));

            Cart emptyCart = Cart.builder().id(301L).user(user).items(new ArrayList<>()).build();
            when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(emptyCart));
            assertThrows(BadRequestException.class, () -> checkoutService.placeOrder(1L, request));
        }

        @Test
        @DisplayName("Should reject inactive product/variant or missing/insufficient stock in cart")
        void placeOrder_CartItemInactiveOrInsufficientStock_ThrowsExpectedException() {
            CheckoutRequest request = manualCodRequest();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

            // Variant bị INACTIVE
            variant1.setStatus(ProductStatus.INACTIVE);
            assertThrows(BadRequestException.class, () -> checkoutService.placeOrder(1L, request));

            // Product bị INACTIVE
            variant1.setStatus(ProductStatus.ACTIVE);
            product.setStatus(ProductStatus.INACTIVE);
            assertThrows(BadRequestException.class, () -> checkoutService.placeOrder(1L, request));

            // Inventory không tồn tại
            product.setStatus(ProductStatus.ACTIVE);
            when(inventoryRepository.findByProductVariantId(1001L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> checkoutService.placeOrder(1L, request));

            // Tồn kho hiện tại null hoặc nhỏ hơn số lượng đặt
            inventory1.setQuantity(null);
            when(inventoryRepository.findByProductVariantId(1001L)).thenReturn(Optional.of(inventory1));
            assertThrows(BadRequestException.class, () -> checkoutService.placeOrder(1L, request));
        }

        @Test
        @DisplayName("Should reject direct item when variant not found, inactive, or missing inventory")
        void placeOrder_DirectItemInvalid_ThrowsExpectedException() {
            CheckoutRequest request = manualCodRequest();
            request.setDirectItems(List.of(CheckoutItem.builder().productVariantId(1001L).quantity(1).build()));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            when(productVariantRepository.findById(1001L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> checkoutService.placeOrder(1L, request));

            variant1.setStatus(ProductStatus.INACTIVE);
            when(productVariantRepository.findById(1001L)).thenReturn(Optional.of(variant1));
            assertThrows(BadRequestException.class, () -> checkoutService.placeOrder(1L, request));

            variant1.setStatus(ProductStatus.ACTIVE);
            when(inventoryRepository.findByProductVariantId(1001L)).thenReturn(Optional.empty());
            assertThrows(BadRequestException.class, () -> checkoutService.placeOrder(1L, request));
        }

        @Test
        @DisplayName("Should throw BadRequestException if locked stock becomes insufficient during deduction")
        void placeOrder_StockInsufficientAtLockTime_ThrowsBadRequest() {
            CheckoutRequest request = manualCodRequest();
            Inventory lockedLowStock = Inventory.builder().id(2001L).quantity(1L).build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
            when(inventoryRepository.findByProductVariantId(1001L)).thenReturn(Optional.of(inventory1));
            when(inventoryRepository.findByProductVariantIdForUpdate(1001L)).thenReturn(Optional.of(lockedLowStock));
            stubOrderSave();

            assertThrows(BadRequestException.class, () -> checkoutService.placeOrder(1L, request));
            verify(inventoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject invalid, inactive, not-started, expired, exhausted, or under-min coupon")
        void placeOrder_InvalidCouponScenarios_ThrowExpectedExceptions() {
            CheckoutRequest request = manualCodRequest();
            request.setCouponCode("BADCODE");

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
            when(inventoryRepository.findByProductVariantId(1001L)).thenReturn(Optional.of(inventory1));

            // 1. Không tìm thấy
            when(couponRepository.findByCode("BADCODE")).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> checkoutService.placeOrder(1L, request));

            // 2. Ngưng kích hoạt
            request.setCouponCode("SALE15");
            percentageCoupon.setIsActive(false);
            when(couponRepository.findByCode("SALE15")).thenReturn(Optional.of(percentageCoupon));
            assertThrows(BadRequestException.class, () -> checkoutService.placeOrder(1L, request));

            // 3. Chưa đến ngày bắt đầu
            percentageCoupon.setIsActive(true);
            percentageCoupon.setStartDate(Instant.now().plus(1, ChronoUnit.DAYS));
            assertThrows(BadRequestException.class, () -> checkoutService.placeOrder(1L, request));

            // 4. Đã hết hạn
            percentageCoupon.setStartDate(Instant.now().minus(5, ChronoUnit.DAYS));
            percentageCoupon.setEndDate(Instant.now().minus(1, ChronoUnit.HOURS));
            assertThrows(BadRequestException.class, () -> checkoutService.placeOrder(1L, request));

            // 5. Hết lượt dùng
            percentageCoupon.setEndDate(Instant.now().plus(5, ChronoUnit.DAYS));
            percentageCoupon.setUsedCount(100);
            assertThrows(BadRequestException.class, () -> checkoutService.placeOrder(1L, request));

            // 6. Chưa đủ giá trị đơn tối thiểu
            percentageCoupon.setUsedCount(10);
            percentageCoupon.setMinOrderValue(new BigDecimal("500000"));
            assertThrows(BadRequestException.class, () -> checkoutService.placeOrder(1L, request));
        }

        @Test
        @DisplayName("Should throw BadRequestException when concurrent coupon increment returns 0")
        void placeOrder_CouponRaceCondition_ThrowsBadRequest() {
            CheckoutRequest request = manualCodRequest();
            request.setCouponCode("SALE15");

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
            when(inventoryRepository.findByProductVariantId(1001L)).thenReturn(Optional.of(inventory1));
            when(inventoryRepository.findByProductVariantIdForUpdate(1001L)).thenReturn(Optional.of(inventory1));
            when(couponRepository.findByCode("SALE15")).thenReturn(Optional.of(percentageCoupon));
            when(couponRepository.incrementUsedCountIfAvailable(anyLong(), any(Instant.class))).thenReturn(0);
            stubOrderSave();

            assertThrows(BadRequestException.class, () -> checkoutService.placeOrder(1L, request));
            verify(cartRepository, never()).save(any());
        }
    }
}
