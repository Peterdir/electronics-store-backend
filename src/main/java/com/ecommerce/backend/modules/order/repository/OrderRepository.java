package com.ecommerce.backend.modules.order.repository;

import com.ecommerce.backend.modules.dashboard.dto.response.OrderStatusStatResponse;
import com.ecommerce.backend.modules.order.entity.Order;
import com.ecommerce.backend.modules.order.enums.OrderStatus;
import com.ecommerce.backend.modules.order.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"user"})
    @Query("""
        SELECT o FROM Order o LEFT JOIN o.user u WHERE
        (:keyword IS NULL OR :keyword = '' OR
        LOWER(o.recipientName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
        LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
        o.recipientPhone LIKE LOWER(CONCAT('%', :keyword, '%')) OR
        str(o.id) LIKE CONCAT('%', :keyword, '%')
        )
        AND (:orderStatus IS NULL OR :orderStatus = o.orderStatus)
        AND (:paymentStatus IS NULL OR :paymentStatus = o.paymentStatus)
        """)
    Page<Order> searchOrdersAdmin(
            @Param("keyword") String keyword,
            @Param("orderStatus") OrderStatus orderStatus,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            Pageable pageable
    );

    @Query("""
        SELECT o FROM Order o WHERE
        o.user.id = :userId
        AND (:orderStatus IS NULL OR o.orderStatus = :orderStatus)
        """)
    Page<Order> findByUserIdAndOrderStatus(
            @Param("userId") Long userId,
            @Param("orderStatus") OrderStatus orderStatus,
            Pageable pageable
    );

    @Query("""
        SELECT COALESCE(SUM(o.totalPrice), 0)
        FROM Order o
        WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate
            AND (o.orderStatus = OrderStatus.DELIVERED
            OR (o.paymentStatus = PaymentStatus.PAID
            AND o.orderStatus NOT IN (OrderStatus.CANCELLED, OrderStatus.RETURNED)))
    """)
    BigDecimal calculateTotalRevenue(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("""
        SELECT COUNT(o)
        FROM Order o
        WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate
    """)
    Long countOrdersBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query(value = """
        SELECT 
            DATE_FORMAT(created_at, '%Y-%m-%d') AS orderDate,
            COALESCE(SUM(
                CASE 
                    WHEN order_status = 'DELIVERED' 
                        OR (
                            payment_status = 'PAID' AND order_status NOT IN ('CANCELLED', 'RETURNED')
                        ) 
                        THEN total_price 
                        ELSE 0 
                        END
                ),
                0) 
            AS dailyRevenue,
            COUNT(id) AS dailyOrders
        FROM orders
        WHERE created_at >= :startDate AND created_at <= :endDate
        GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d')
        ORDER BY orderDate ASC
    """, nativeQuery = true)
    List<Object[]> getDailyRevenueAndOrders(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("""
        SELECT new com.ecommerce.backend.modules.dashboard.dto.response.OrderStatusStatResponse(o.orderStatus, COUNT(o))
        FROM Order o
        WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate
        GROUP BY o.orderStatus
    """)
    List<OrderStatusStatResponse> countOrdersByStatus(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
}
