package com.ecommerce.backend.modules.inventory.entity;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.ecommerce.backend.common.utils.Tsid;
import com.ecommerce.backend.modules.inventory.enums.InventoryAction;
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
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    private InventoryAction action;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long userId;

    private String performedBy;

    private Long quantityChanged;

    private Long finalStock;

    private String reason;

    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id")
    private Inventory inventory;

    // Thiếu UserId
}
