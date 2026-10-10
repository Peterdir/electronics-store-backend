package com.ecommerce.backend.modules.order.enums;

public enum OrderStatus {
    PENDING,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    RETURN_REQUESTED,
    RETURNED,
    RETURN_REJECTED;

    public boolean canTransitionTo(OrderStatus newStatus) {
        if (newStatus == null || this == newStatus) {
            return false;
        }

        return switch (this) {
            case PENDING -> newStatus == PROCESSING || newStatus == CANCELLED;
            case PROCESSING -> newStatus == SHIPPED || newStatus == CANCELLED;
            case SHIPPED -> newStatus == DELIVERED;
            case DELIVERED -> newStatus == RETURN_REQUESTED;
            case RETURN_REQUESTED -> newStatus == RETURNED || newStatus == RETURN_REJECTED;
            case CANCELLED, RETURNED, RETURN_REJECTED -> false; // Terminal states
        };
    }
}
