package com.ecommerce.backend.modules.order.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.modules.address.entity.Address;
import com.ecommerce.backend.modules.address.repository.AddressRepository;
import com.ecommerce.backend.modules.auth.entity.User;
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
import com.ecommerce.backend.modules.order.dto.response.OrderItemResponse;
import com.ecommerce.backend.modules.order.entity.Order;
import com.ecommerce.backend.modules.order.entity.OrderItem;
import com.ecommerce.backend.modules.order.enums.OrderStatus;
import com.ecommerce.backend.modules.order.enums.PaymentMethod;
import com.ecommerce.backend.modules.order.enums.PaymentStatus;
import com.ecommerce.backend.modules.order.repository.OrderRepository;
import com.ecommerce.backend.modules.product.entity.ProductVariant;
import com.ecommerce.backend.modules.product.enums.ProductStatus;
import com.ecommerce.backend.modules.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;
    private final CartRepository cartRepository;
    private final CouponRepository couponRepository;
    private final OrderRepository orderRepository;

    private static final BigDecimal STANDARD_SHIPPING_FEE = new BigDecimal("30000");
    private static final BigDecimal EXPRESS_SHIPPING_FEE = new BigDecimal("55000");

    @Override
    @Transactional
    public CheckoutResponse placeOrder(Long userId, CheckoutRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        ShippingInfo shippingInfo = resolveShippingAddress(request, userId);

        BigDecimal shippingFee = calculateShippingFee(request.getShippingMethod());

        boolean isBuyNow = request.getDirectItems() != null && !request.getDirectItems().isEmpty();

        OrderItemsResult itemsResult;

        Cart cart = null;

        if (isBuyNow) {
            itemsResult = buildOrderItemsFromDirectItems(request.getDirectItems());
        }
        else {
            cart = cartRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cart not found."));

            if (cart.getItems() == null || cart.getItems().isEmpty()) {
                throw new BadRequestException("Your cart is empty.");
            }

            itemsResult = buildOrderItemsFromCart(cart);
        }

        CouponResult couponResult = applyCouponIfPresent(
                request.getCouponCode(), itemsResult.subtotal());

        BigDecimal totalPrice = calculateTotalPrice(itemsResult.subtotal(), shippingFee, couponResult.discountAmount());

        Order order = buildOrder(user, shippingInfo, shippingFee,
                couponResult, itemsResult, totalPrice, request.getPaymentMethod());

        if (isBuyNow) {
            deductInventoryFromOrderItems(order, user.getFullName());
        } else {
            deductInventoryFromCart(cart, order, user.getFullName());
        }

        if (couponResult.coupon() != null) {
            couponResult.coupon().setUsedCount(couponResult.coupon().getUsedCount() + 1);
            couponRepository.save(couponResult.coupon());
        }

        Order savedOrder = orderRepository.save(order);

        if (!isBuyNow) {
            cart.getItems().clear();
            cartRepository.save(cart);
        }

        return buildResponse(savedOrder, request.getPaymentMethod(), false);
    }

    public CheckoutResponse placeGuestOrder(CheckoutRequest request) {

        if (request.getDirectItems() == null || request.getDirectItems().isEmpty()) {
            throw new BadRequestException("Your cart is empty.");
        }

        validateShippingFields(request);

        if (request.getAddressId() != null) {
            throw new AccessDeniedException("Address book is only available for registered users.");
        }

        ShippingInfo shippingInfo = new ShippingInfo(
                request.getRecipientName(),
                request.getRecipientPhone(),
                request.getShippingAddress()
        );

        BigDecimal shippingFee = calculateShippingFee(request.getShippingMethod());

        OrderItemsResult itemsResult = buildOrderItemsFromDirectItems(request.getDirectItems());

        CouponResult couponResult = applyCouponIfPresent(request.getCouponCode(), itemsResult.subtotal());

        BigDecimal totalPrice = calculateTotalPrice(itemsResult.subtotal(), shippingFee, couponResult.discountAmount());

        Order order = buildOrder(null, shippingInfo, shippingFee,
                couponResult, itemsResult, totalPrice, request.getPaymentMethod());

        deductInventoryFromOrderItems(order, "Guest: " + shippingInfo.recipientName);

        if (couponResult.coupon() != null) {
            couponResult.coupon().setUsedCount(couponResult.coupon().getUsedCount() + 1);
            couponRepository.save(couponResult.coupon());
        }

        Order savedOrder = orderRepository.save(order);
        
        return buildResponse(savedOrder, request.getPaymentMethod(), true);
    }


    private record ShippingInfo(
            String recipientName, String recipientPhone, String shippingAddress
    ){}

    private record OrderItemsResult(
            List<OrderItem> items, BigDecimal subtotal) {}

    private record CouponResult(
            Coupon coupon, String code, BigDecimal discountAmount) {}


    private ShippingInfo resolveShippingAddress(CheckoutRequest request, Long userId) {
        if (request.getAddressId() != null) {
            Address address = addressRepository.findById(request.getAddressId())
                    .orElseThrow(() -> new ResourceNotFoundException("Address not found."));

            if (!address.getUser().getId().equals(userId)) {
                throw new AccessDeniedException("You don't have permission to use this address.");
            }

            return new ShippingInfo(
                    address.getFullName(),
                    address.getPhone(),
                    buildFullAddress(address)
            );
        }

        validateShippingFields(request);
        return new ShippingInfo(
                request.getRecipientName(),
                request.getRecipientPhone(),
                request.getShippingAddress()
        );
    }

    private String buildFullAddress(Address address) {
        return Stream.of(
                address.getSpecificAddress(),
                address.getWard(),
                address.getDistrict(),
                address.getProvince()
        ).filter(StringUtils::hasText)
                .collect(Collectors.joining(", "));
    }

    private void validateShippingFields(CheckoutRequest request) {
        if (!StringUtils.hasText(request.getRecipientName())) {
            throw new BadRequestException("Recipient name is required.");
        }

        if (!StringUtils.hasText(request.getRecipientPhone())) {
            throw new BadRequestException("Phone number is required.");
        }

        if (!StringUtils.hasText(request.getShippingAddress())) {
            throw new BadRequestException("Shipping address is required.");
        }
    }

    private BigDecimal calculateShippingFee(String shippingMethod) {
        if (shippingMethod == null) return STANDARD_SHIPPING_FEE;

        return switch (shippingMethod.toUpperCase()) {
            case "EXPRESS" -> EXPRESS_SHIPPING_FEE;
            default -> STANDARD_SHIPPING_FEE;
        };
    }

    private OrderItemsResult buildOrderItemsFromDirectItems(List<CheckoutItem> items) {
        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CheckoutItem item : items) {
            ProductVariant productVariant = productVariantRepository.findById(item.getProductVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Some items are out of stock. Please update your cart."));

            if (productVariant.getProduct() == null ||
                    productVariant.getProduct().getStatus() != ProductStatus.ACTIVE ||
                    productVariant.getStatus() != ProductStatus.ACTIVE
            ) {
                throw new BadRequestException("Some items are out of stock. Please update your cart.");
            }

            Inventory inventory = inventoryRepository.findByProductVariantId(item.getProductVariantId())
                    .orElseThrow(() -> new BadRequestException("Some items are out of stock. Please update your cart."));
            long currentStock = inventory.getQuantity() != null ? inventory.getQuantity() : 0L;
            if (item.getQuantity() > currentStock) {
                throw new BadRequestException("Some items are out of stock. Please update your cart.");
            }

            BigDecimal unitPrice = productVariant.getPrice() != null ? productVariant.getPrice() : BigDecimal.ZERO;
            BigDecimal itemSubtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

            subtotal = subtotal.add(itemSubtotal);

            orderItems.add(OrderItem.builder()
                            .productVariant(productVariant)
                            .productName(productVariant.getProduct().getName())
                            .variantSku(productVariant.getSku())
                            .thumbnailUrl(productVariant.getImageUrl())
                            .unitPrice(productVariant.getPrice())
                            .quantity(item.getQuantity())
                            .subtotal(itemSubtotal)
                    .build());
        }

        return new OrderItemsResult(orderItems, subtotal);
    }

    private OrderItemsResult buildOrderItemsFromCart(Cart cart) {
        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            ProductVariant productVariant = item.getProductVariant();

            if (productVariant == null || productVariant.getProduct() == null ||
                productVariant.getProduct().getStatus() != ProductStatus.ACTIVE ||
                    productVariant.getStatus() != ProductStatus.ACTIVE) {
                throw new BadRequestException("Some items are out of stock. Please update your cart.");
            }

            Inventory inventory = inventoryRepository.findByProductVariantId(productVariant.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Some items are out of stock. Please update your cart."));
            long currentStock = inventory.getQuantity() != null ? inventory.getQuantity() : 0L;
            if (item.getQuantity() > currentStock) {
                throw new BadRequestException("Some items are out of stock. Please update your cart.");
            }

            BigDecimal unitPrice = productVariant.getPrice() != null ? productVariant.getPrice() : BigDecimal.ZERO;
            BigDecimal itemSubtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

            subtotal = subtotal.add(itemSubtotal);

            orderItems.add(OrderItem.builder()
                    .productVariant(productVariant)
                    .productName(productVariant.getProduct().getName())
                    .variantSku(productVariant.getSku())
                    .thumbnailUrl(productVariant.getImageUrl())
                    .unitPrice(unitPrice)
                    .quantity(item.getQuantity())
                    .subtotal(itemSubtotal)
                    .build());
        }

        return new OrderItemsResult(orderItems, subtotal);
    }

    private CouponResult applyCouponIfPresent(String couponCode, BigDecimal subtotal) {
        if (!StringUtils.hasText(couponCode)) {
            return new CouponResult(null, null, BigDecimal.ZERO);
        }

        Coupon coupon = couponRepository.findByCode(couponCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired voucher code."));

        validateCoupon(coupon, subtotal);

        BigDecimal discount = calculateDiscount(coupon, subtotal);
        return new CouponResult(coupon, couponCode, discount);
    }

    private void validateCoupon(Coupon coupon, BigDecimal subtotal) {
        if (!coupon.getIsActive()) {
            throw new BadRequestException("Invalid or expired voucher code.");
        }

        Instant now = Instant.now();

        if (coupon.getStartDate() != null && now.isBefore(coupon.getStartDate())) {
            throw new BadRequestException("Invalid or expired voucher code.");
        }

        if (coupon.getEndDate() != null && now.isAfter(coupon.getEndDate())) {
            throw new BadRequestException("Invalid or expired voucher code.");
        }

        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new BadRequestException("Invalid or expired voucher code.");
        }

        if (coupon.getMinOrderValue() != null && subtotal.compareTo(coupon.getMinOrderValue()) < 0) {
            throw new BadRequestException("Order value does not meet the minimum requirement.");
        }
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal subtotal) {
        if (coupon.getDiscountType() != null && coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            return subtotal.multiply(BigDecimal.valueOf(coupon.getDiscountValue())).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        }
        else {
            return BigDecimal.valueOf(coupon.getDiscountValue()).min(subtotal);
        }
    }

    private BigDecimal calculateTotalPrice(BigDecimal subtotal, BigDecimal shippingFee, BigDecimal discount) {
        BigDecimal total = subtotal.add(shippingFee).subtract(discount);

        return total.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : total;
    }

    private Order buildOrder(
            User user,
            ShippingInfo shippingInfo,
            BigDecimal shippingFee,
            CouponResult couponResult,
            OrderItemsResult itemsResult,
            BigDecimal totalPrice,
            PaymentMethod paymentMethod
    ) {
        Order order = Order.builder()
                .user(user)
                .recipientName(shippingInfo.recipientName())
                .recipientPhone(shippingInfo.recipientPhone())
                .shippingAddress(shippingInfo.shippingAddress())
                .subtotal(itemsResult.subtotal())
                .shippingFee(shippingFee)
                .couponCode(couponResult.code())
                .discountAmount(couponResult.discountAmount())
                .totalPrice(totalPrice)
                .orderStatus(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .paymentMethod(paymentMethod)
                .build();

        for (OrderItem item : itemsResult.items()) {
            item.setOrder(order);
        }

        order.setItems(new ArrayList<>(itemsResult.items()));

        return order;
    }

    private void deductInventoryFromOrderItems(Order order, String customerName) {
        for (OrderItem orderItem : order.getItems()) {
            deductSingleItem(orderItem.getProductVariant().getId(), order.getId(), orderItem.getQuantity(), customerName);
        }
    }

    private void deductInventoryFromCart(Cart cart, Order order, String customerName) {
        for (CartItem cartItem : cart.getItems()) {
            deductSingleItem(cartItem.getProductVariant().getId(), order.getId(), cartItem.getQuantity(), customerName);
        }
    }

    private void deductSingleItem(Long variantId, Long orderId, int quantity, String customerName) {
        Inventory inventory = inventoryRepository.findByProductVariantId(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Some items are out of stock. Please update your cart."));

        long currentStock = inventory.getQuantity();
        long newQuantity = currentStock - quantity;

        inventory.setUpdatedAt(Instant.now());
        inventory.setQuantity(newQuantity);
        inventoryRepository.save(inventory);

        inventoryHistoryRepository.save(InventoryHistory.builder()
                .inventory(inventory)
                .action(InventoryAction.DEDUCT)
                .quantityChanged((long) quantity)
                .finalStock(newQuantity)
                .performedBy(customerName)
                .reason("Deducted for order #: " + orderId)
                .createdAt(Instant.now())
                .build());
    }

    private CheckoutResponse buildResponse(Order order, PaymentMethod paymentMethod, boolean isGuest) {

        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .variantId(item.getProductVariant() != null
                                ? item.getProductVariant().getId() : null)
                        .productName(item.getProductName())
                        .variantSku(item.getVariantSku())
                        .thumbnailUrl(item.getThumbnailUrl())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getSubtotal())
                        .build())
                .toList();

        String message;
        String paymentUrl = null;

        if (paymentMethod == PaymentMethod.COD) {
            message = "Order placed successfully!";
        } else {
            message = "Order created. Redirecting to payment gateway...";
            paymentUrl = "/api/v1/payments/vnpay/create?orderId=" + order.getId();
        }

        return CheckoutResponse.builder()
                .orderId(order.getId())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .shippingAddress(order.getShippingAddress())
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .couponCode(order.getCouponCode())
                .discountAmount(order.getDiscountAmount())
                .totalPrice(order.getTotalPrice())
                .orderStatus(order.getOrderStatus())
                .paymentStatus(order.getPaymentStatus())
                .paymentMethod(order.getPaymentMethod())
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .message(message)
                .paymentUrl(paymentUrl)
                .isGuestOrder(isGuest)
                .build();
    }
}
