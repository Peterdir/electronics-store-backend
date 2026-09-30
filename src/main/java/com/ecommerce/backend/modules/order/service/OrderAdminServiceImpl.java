package com.ecommerce.backend.modules.order.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.modules.inventory.entity.InventoryHistory;
import com.ecommerce.backend.modules.inventory.enums.InventoryAction;
import com.ecommerce.backend.modules.inventory.repository.InventoryHistoryRepository;
import com.ecommerce.backend.modules.inventory.repository.InventoryRepository;
import com.ecommerce.backend.modules.order.dto.request.CancelOrderRequest;
import com.ecommerce.backend.modules.order.dto.request.UpdateOrderStatusRequest;
import com.ecommerce.backend.modules.order.dto.response.OrderDetailAdminResponse;
import com.ecommerce.backend.modules.order.dto.response.OrderItemResponse;
import com.ecommerce.backend.modules.order.dto.response.OrderListAdminResponse;
import com.ecommerce.backend.modules.order.entity.Order;
import com.ecommerce.backend.modules.order.entity.OrderItem;
import com.ecommerce.backend.modules.order.enums.OrderStatus;
import com.ecommerce.backend.modules.order.enums.PaymentStatus;
import com.ecommerce.backend.modules.order.repository.OrderRepository;
import com.ecommerce.backend.modules.product.entity.ProductVariant;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderAdminServiceImpl implements OrderAdminService {

    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<OrderListAdminResponse> getOrders(String keyword, OrderStatus orderStatus, PaymentStatus paymentStatus, Pageable pageable) {
        return orderRepository.searchOrdersAdmin(keyword, orderStatus, paymentStatus, pageable)
                .map(this::mapToOrderListResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailAdminResponse getOrderDetail(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("This order no longer exists or its status has been changed. Data is being updated."));
        return mapToOrderDetailResponse(order);
    }

    @Override
    @Transactional
    public OrderDetailAdminResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("This order no longer exists or its status has been changed. Data is being updated."));

        OrderStatus currentStatus = order.getOrderStatus();
        OrderStatus newStatus = request.getNewStatus();

        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new BadRequestException("Invalid status transition. Please follow the correct order flow.");
        }

        if (newStatus == OrderStatus.CANCELLED) {
            restoreInventory(order, "Order status updated to CANCELLED by Admin");
            order.setCancelReason("Cancelled by Admin via status update");
            order.setCancelledAt(Instant.now());
        }

        order.setOrderStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        return mapToOrderDetailResponse(updatedOrder);
    }

    @Override
    @Transactional
    public OrderDetailAdminResponse cancelOrder(Long orderId, CancelOrderRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("This order no longer exists or its status has been changed. Data is being updated."));

        if (!order.getOrderStatus().canTransitionTo(OrderStatus.CANCELLED)) {
            throw new BadRequestException("Invalid status transition. Please follow the correct order flow.");
        }

        restoreInventory(order, request.getReason());
        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancelReason(request.getReason());
        order.setCancelledAt(Instant.now());
        Order updatedOrder = orderRepository.save(order);

        return mapToOrderDetailResponse(updatedOrder);
    }

    private void restoreInventory(Order order, String reason) {
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

                                InventoryHistory history = InventoryHistory.builder()
                                        .inventory(inventory)
                                        .action(InventoryAction.ADD)
                                        .quantityChanged((long) item.getQuantity())
                                        .finalStock(newQuantity)
                                        .reason("Restocked from cancelled order #" + order.getId() + ": " + reason)
                                        .performedBy("Admin")
                                        .createdAt(Instant.now())
                                        .build();

                                inventoryHistoryRepository.save(history);
                            });
                }
            }
        }
    }

    private OrderListAdminResponse mapToOrderListResponse(Order order) {
        return OrderListAdminResponse.builder()
                .id(order.getId())
                .customerName(order.getUser() != null ? order.getUser().getFullName() : order.getRecipientName())
                .customerEmail(order.getUser() != null ? order.getUser().getEmail() : null)
                .totalPrice(order.getTotalPrice())
                .orderDate(order.getCreatedAt())
                .paymentStatus(order.getPaymentStatus())
                .orderStatus(order.getOrderStatus())
                .build();
    }

    private OrderDetailAdminResponse mapToOrderDetailResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .variantId(item.getProductVariant() != null ? item.getProductVariant().getId() : null)
                        .productName(item.getProductName())
                        .variantSku(item.getVariantSku())
                        .thumbnailUrl(item.getThumbnailUrl())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getSubtotal())
                        .build())
                .toList();
        return OrderDetailAdminResponse.builder()
                .id(order.getId())
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .customerName(order.getUser() != null ? order.getUser().getFullName() : null)
                .customerEmail(order.getUser() != null ? order.getUser().getEmail() : null)
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
                .items(itemResponses)
                .build();
    }
}
