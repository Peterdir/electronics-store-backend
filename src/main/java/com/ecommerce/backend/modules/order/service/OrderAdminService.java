package com.ecommerce.backend.modules.order.service;

import com.ecommerce.backend.modules.order.dto.request.CancelOrderRequest;
import com.ecommerce.backend.modules.order.dto.request.UpdateOrderStatusRequest;
import com.ecommerce.backend.modules.order.dto.response.OrderDetailAdminResponse;
import com.ecommerce.backend.modules.order.dto.response.OrderListAdminResponse;
import com.ecommerce.backend.modules.order.enums.OrderStatus;
import com.ecommerce.backend.modules.order.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderAdminService {
    Page<OrderListAdminResponse> getOrders(String keyword, OrderStatus orderStatus,
                                           PaymentStatus paymentStatus, Pageable pageable);

    OrderDetailAdminResponse getOrderDetail(Long orderId);

    OrderDetailAdminResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request);

    OrderDetailAdminResponse cancelOrder(Long userId, CancelOrderRequest request);
}
