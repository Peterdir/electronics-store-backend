package com.ecommerce.backend.modules.order.repository;

import com.ecommerce.backend.modules.order.entity.OrderItem;
import com.ecommerce.backend.modules.order.enums.OrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT item FROM OrderItem item WHERE item.id = :id")
    Optional<OrderItem> findByIdForUpdate(@Param("id") Long id);

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
