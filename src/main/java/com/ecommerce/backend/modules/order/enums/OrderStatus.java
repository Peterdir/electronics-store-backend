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
            case PENDING -> newStatus == PROCESSING || newStatus == CANCELLED;
            case PROCESSING -> newStatus == SHIPPED || newStatus == CANCELLED;
            case SHIPPED -> newStatus == DELIVERED;
            case DELIVERED, CANCELLED -> false; // Terminal states
        };
    }
}
