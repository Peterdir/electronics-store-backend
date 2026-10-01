package com.ecommerce.backend.modules.order.repository;

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
}
