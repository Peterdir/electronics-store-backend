package com.ecommerce.backend.modules.inventory_history.entity;

import com.ecommerce.backend.common.utils.Tsid;
import com.ecommerce.backend.modules.inventory.entity.Inventory;
import com.ecommerce.backend.modules.inventory_history.enums.InventoryAction;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "inventory_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryHistory {

    @Id
    @Tsid
    private Long id;

    private InventoryAction action;

    private Long userId;

    private String performedBy;

    private Long quantityChanged;

    private Long finalStock;

    private String reason;

    private Instant createdAt;

    @ManyToOne
    @JoinColumn(name = "inventory_id")
    private Inventory inventory;

    // Thiếu UserId
}
