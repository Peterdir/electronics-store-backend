package com.ecommerce.backend.modules.order.repository;

import com.ecommerce.backend.modules.order.entity.OrderItem;
import com.ecommerce.backend.modules.order.enums.OrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @EntityGraph(attributePaths = {"order", "productVariant", "productVariant.product"})
    @Query("""
            SELECT oi FROM OrderItem oi
            JOIN oi.order o
            WHERE o.user.id = :userId
            AND o.orderStatus = :status
            AND NOT EXISTS (SELECT r FROM Review r WHERE r.orderItem.id = oi.id AND r.isDeleted = false) 
            """)
    List<OrderItem> findPendingReviewItems(@Param("userId") Long userId, @Param("status") OrderStatus status);
}
