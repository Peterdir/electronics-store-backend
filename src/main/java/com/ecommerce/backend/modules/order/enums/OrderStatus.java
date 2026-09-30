package com.ecommerce.backend.modules.order.enums;

import java.util.Set;

public enum OrderStatus {
    PENDING,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus newStatus) {
        if (newStatus == null || this == newStatus) {
            return false;
        }
        return switch (this) {
            case PENDING -> Set.of(PROCESSING, CANCELLED).contains(newStatus);
            case PROCESSING -> Set.of(SHIPPED, CANCELLED).contains(newStatus);
            case SHIPPED -> Set.of(DELIVERED, CANCELLED).contains(newStatus);
            case DELIVERED, CANCELLED -> false; // Terminal states
        };
    }
}
