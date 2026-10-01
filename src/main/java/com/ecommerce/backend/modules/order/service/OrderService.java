package com.ecommerce.backend.modules.order.service;

import com.ecommerce.backend.modules.order.dto.request.CancelOrderRequest;
import com.ecommerce.backend.modules.order.dto.response.BuyAgainResponse;
import com.ecommerce.backend.modules.order.dto.response.OrderDetailUserResponse;
import com.ecommerce.backend.modules.order.dto.response.OrderListUserResponse;
import com.ecommerce.backend.modules.order.dto.response.OrderPaymentResponse;
import com.ecommerce.backend.modules.order.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    // Lấy danh sách lịch sử đơn hàng của người dùng hiện tại
    Page<OrderListUserResponse> getMyOrders(Long userId, OrderStatus orderStatus, Pageable pageable);

     // Xem chi tiết một đơn hàng của người dùng
    OrderDetailUserResponse getMyOrderDetail(Long userId, Long orderId);

    // Mua lại đơn hàng cũ
    BuyAgainResponse buyAgain(Long userId, Long orderId);

    // Hủy đơn hàng của người dùng
    OrderDetailUserResponse cancelMyOrder(Long userId, Long orderId, CancelOrderRequest request);

    // Thanh toán lại cho đơn hàng chưa thanh toán
    OrderPaymentResponse payOrder(Long userId, Long orderId);
}
