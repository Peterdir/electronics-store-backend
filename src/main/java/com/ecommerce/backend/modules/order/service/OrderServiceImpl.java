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
import com.ecommerce.backend.modules.inventory.entity.InventoryHistory;
import com.ecommerce.backend.modules.inventory.enums.InventoryAction;
import com.ecommerce.backend.modules.inventory.repository.InventoryHistoryRepository;
import com.ecommerce.backend.modules.inventory.repository.InventoryRepository;
import com.ecommerce.backend.modules.order.dto.request.CancelOrderRequest;
import com.ecommerce.backend.modules.order.dto.response.BuyAgainResponse;
import com.ecommerce.backend.modules.order.dto.response.OrderDetailUserResponse;
import com.ecommerce.backend.modules.order.dto.response.OrderItemResponse;
import com.ecommerce.backend.modules.order.dto.response.OrderListUserResponse;
import com.ecommerce.backend.modules.order.dto.response.OrderPaymentResponse;
import com.ecommerce.backend.modules.order.entity.Order;
import com.ecommerce.backend.modules.order.entity.OrderItem;
import com.ecommerce.backend.modules.order.enums.OrderStatus;
import com.ecommerce.backend.modules.order.enums.PaymentStatus;
import com.ecommerce.backend.modules.order.repository.OrderRepository;
import com.ecommerce.backend.modules.product.entity.ProductVariant;
import com.ecommerce.backend.modules.product.enums.ProductStatus;
import com.ecommerce.backend.modules.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;
    private final CartService cartService;

    @Override
    @Transactional(readOnly = true)
    public Page<OrderListUserResponse> getMyOrders(Long userId, OrderStatus orderStatus, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new BadRequestException("This account has been locked. Please contact support");
        }

        return orderRepository.findByUserIdAndOrderStatus(userId, orderStatus, pageable)
                .map(this::mapToOrderListUserResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailUserResponse getMyOrderDetail(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("This order no longer exists or its status has been changed. Data is being updated."));

        if (order.getUser() == null || !order.getUser().getId().equals(userId)) {
            throw new BadRequestException("You do not have permission to view this order.");
        }

        if (order.getUser().getStatus() == UserStatus.INACTIVE) {
            throw new BadRequestException("This account has been locked. Please contact support");
        }

        return mapToOrderDetailUserResponse(order);
    }

    @Override
    @Transactional
    public BuyAgainResponse buyAgain(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("This order no longer exists or its status has been changed. Data is being updated."));

        if (order.getUser() == null || !order.getUser().getId().equals(userId)) {
            throw new BadRequestException("You do not have permission to view this order.");
        }

        if (order.getUser().getStatus() == UserStatus.INACTIVE) {
            throw new BadRequestException("This account has been locked. Please contact support.");
        }

        Cart cart = getOrCreateCart(userId);

        List<String> addedItemNames = new ArrayList<>();
        List<String> unavailableItemNames = new ArrayList<>();

        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                if (item == null) continue;

                ProductVariant variant = item.getProductVariant();
                if (variant == null || variant.getId() == null) {
                    unavailableItemNames.add(item.getProductName());
                    continue;
                }

                ProductVariant freshVariant = productVariantRepository.findById(variant.getId()).orElse(null);
                if (freshVariant == null) {
                    unavailableItemNames.add(item.getProductName());
                    continue;
                }

                boolean isInactive = freshVariant.getStatus() != ProductStatus.ACTIVE
                        || freshVariant.getProduct() == null
                        || freshVariant.getProduct().getStatus() != ProductStatus.ACTIVE;

                if (isInactive) {
                    unavailableItemNames.add(item.getProductName());
                    continue;
                }

                Long stock = getAvailableStock(freshVariant);
                if (stock <= 0) {
                    unavailableItemNames.add(item.getProductName());
                    continue;
                }

                Optional<CartItem> existingItemOpt = cart.getItems().stream()
                        .filter(ci -> ci.getProductVariant() != null && ci.getProductVariant().getId().equals(freshVariant.getId()))
                        .findFirst();

                int currentInCart = existingItemOpt.map(CartItem::getQuantity).orElse(0);
                int requested = item.getQuantity() != null && item.getQuantity() > 0 ? item.getQuantity() : 1;

                if (currentInCart >= stock) {
                    unavailableItemNames.add(item.getProductName());
                    continue;
                }

                int toAdd = Math.min(requested, (int) (stock - currentInCart));
                if (toAdd <= 0) {
                    unavailableItemNames.add(item.getProductName());
                    continue;
                }

                if (existingItemOpt.isPresent()) {
                    CartItem existingItem = existingItemOpt.get();
                    existingItem.setQuantity(currentInCart + toAdd);
                } else {
                    CartItem newItem = CartItem.builder()
                            .cart(cart)
                            .productVariant(freshVariant)
                            .quantity(toAdd)
                            .build();
                    cart.getItems().add(newItem);
                }
                addedItemNames.add(item.getProductName());
            }
        }

        cartRepository.save(cart);

        String message;
        if (!unavailableItemNames.isEmpty() && !addedItemNames.isEmpty()) {
            message = "Some items from your previous order are currently unavailable and could not be added to your cart.";
        } else if (!addedItemNames.isEmpty()) {
            message = "Items have been added to your cart.";
        } else {
            message = "Some items from your previous order are currently unavailable and could not be added to your cart.";
        }

        CartResponse cartResponse = cartService.getCart(userId);

        return BuyAgainResponse.builder()
                .message(message)
                .addedItemsCount(addedItemNames.size())
                .unavailableItemsCount(unavailableItemNames.size())
                .addedItemNames(addedItemNames)
                .unavailableItemNames(unavailableItemNames)
                .cart(cartResponse)
                .build();
    }

    @Override
    @Transactional
    public OrderDetailUserResponse cancelMyOrder(Long userId, Long orderId, CancelOrderRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("This order no longer exists or its status has been changed. Data is being updated."));

        if (order.getUser() == null || !order.getUser().getId().equals(userId)) {
            throw new BadRequestException("You do not have permission to cancel this order.");
        }

        if (order.getUser().getStatus() == UserStatus.INACTIVE) {
            throw new BadRequestException("This account has been locked. Please contact support.");
        }

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("This order is already cancelled.");
        }

        if (order.getOrderStatus() == OrderStatus.DELIVERED) {
            throw new BadRequestException("Cannot cancel an order that has already been delivered.");
        }

        if (order.getOrderStatus() == OrderStatus.SHIPPED) {
            throw new BadRequestException("Cannot cancel an order that is currently being shipped.");
        }

        if (!canUserCancel(order)) {
            throw new BadRequestException("This order cannot be cancelled in its current status: " + order.getOrderStatus());
        }

        restoreInventory(order, request.getReason());
        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancelReason(request.getReason());
        order.setCancelledAt(Instant.now());
        Order updatedOrder = orderRepository.save(order);

        return mapToOrderDetailUserResponse(updatedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderPaymentResponse payOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("This order no longer exists or its status has been changed. Data is being updated."));

        if (order.getUser() == null || !order.getUser().getId().equals(userId)) {
            throw new BadRequestException("You do not have permission to pay for this order.");
        }

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot make payment for a cancelled order.");
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Order is already paid.");
        }

        String paymentUrl = "/api/v1/payments/checkout?orderId=" + order.getId() + "&amount=" + order.getTotalPrice();

        return OrderPaymentResponse.builder()
                .orderId(order.getId())
                .paymentStatus(order.getPaymentStatus())
                .paymentMethod(order.getPaymentMethod())
                .totalPrice(order.getTotalPrice())
                .message("Payment checkout initiated successfully.")
                .paymentUrl(paymentUrl)
                .build();
    }

    private void restoreInventory(Order order, String reason) {
        if (order.getItems() == null) return;

        for (OrderItem item : order.getItems()) {
            if (item != null) {
                ProductVariant productVariant = item.getProductVariant();

                if (productVariant != null && productVariant.getId() != null) {
                    inventoryRepository.findByProductVariantId(productVariant.getId())
                            .ifPresent(inventory -> {
                                long currentQuantity = inventory.getQuantity() != null ? inventory.getQuantity() : 0L;
                                long newQuantity = currentQuantity + item.getQuantity();
                                inventory.setQuantity(newQuantity);
                                inventory.setUpdatedAt(Instant.now());
                                inventoryRepository.save(inventory);

                                String customerName = order.getUser() != null && order.getUser().getFullName() != null
                                        ? order.getUser().getFullName()
                                        : "Unknown user";

                                InventoryHistory history = InventoryHistory.builder()
                                        .inventory(inventory)
                                        .action(InventoryAction.ADD)
                                        .quantityChanged((long) item.getQuantity())
                                        .finalStock(newQuantity)
                                        .reason("Restocked from cancelled order #" + order.getId() + ": " + reason)
                                        .performedBy("Customer: " + customerName)
                                        .createdAt(Instant.now())
                                        .build();

                                inventoryHistoryRepository.save(history);
                            });
                }
            }
        }
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("Not found user."));
                    Cart newCart = Cart.builder()
                            .user(user)
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    private Long getAvailableStock(ProductVariant variant) {
        if (variant == null) return 0L;
        if (variant.getInventory() != null && variant.getInventory().getQuantity() != null) {
            return variant.getInventory().getQuantity();
        }
        return inventoryRepository.findByProductVariantId(variant.getId())
                .map(inv -> inv.getQuantity() != null ? inv.getQuantity() : 0L)
                .orElse(0L);
    }

    private boolean canUserCancel(Order order) {
        if (order == null || order.getOrderStatus() == null) return false;
        if (order.getOrderStatus() == OrderStatus.DELIVERED ||
            order.getOrderStatus() == OrderStatus.SHIPPED ||
            order.getOrderStatus() == OrderStatus.CANCELLED) {
            return false;
        }
        return order.getOrderStatus() == OrderStatus.PENDING ||
               order.getOrderStatus() == OrderStatus.PROCESSING ||
               order.getPaymentStatus() == PaymentStatus.UNPAID;
    }

    private boolean canUserPay(Order order) {
        if (order == null) return false;
        return order.getPaymentStatus() == PaymentStatus.UNPAID &&
               order.getOrderStatus() != OrderStatus.CANCELLED &&
               order.getOrderStatus() != OrderStatus.DELIVERED;
    }

    private boolean canUserBuyAgain(Order order) {
        if (order == null || order.getOrderStatus() == null) return false;
        return order.getOrderStatus() == OrderStatus.DELIVERED ||
               order.getOrderStatus() == OrderStatus.CANCELLED;
    }

    private OrderListUserResponse mapToOrderListUserResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems() != null ? order.getItems().stream()
                .map(this::mapToOrderItemResponse)
                .toList() : List.of();

        int totalItems = order.getItems() != null ? order.getItems().stream()
                .mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                .sum() : 0;

        return OrderListUserResponse.builder()
                .id(order.getId())
                .orderDate(order.getCreatedAt())
                .totalPrice(order.getTotalPrice())
                .orderStatus(order.getOrderStatus())
                .paymentStatus(order.getPaymentStatus())
                .paymentMethod(order.getPaymentMethod())
                .totalItems(totalItems)
                .canCancel(canUserCancel(order))
                .canPay(canUserPay(order))
                .canBuyAgain(canUserBuyAgain(order))
                .items(itemResponses)
                .build();
    }

    private OrderDetailUserResponse mapToOrderDetailUserResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems() != null ? order.getItems().stream()
                .map(this::mapToOrderItemResponse)
                .toList() : List.of();

        return OrderDetailUserResponse.builder()
                .id(order.getId())
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
                .cancelReason(order.getCancelReason())
                .cancelledAt(order.getCancelledAt())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .canCancel(canUserCancel(order))
                .canPay(canUserPay(order))
                .canBuyAgain(canUserBuyAgain(order))
                .items(itemResponses)
                .build();
    }

    private OrderItemResponse mapToOrderItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .variantId(item.getProductVariant() != null ? item.getProductVariant().getId() : null)
                .productName(item.getProductName())
                .variantSku(item.getVariantSku())
                .thumbnailUrl(item.getThumbnailUrl())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .build();
    }
}
