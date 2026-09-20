package com.ecommerce.backend.modules.inventory.entity;

import com.ecommerce.backend.common.utils.Tsid;
import com.ecommerce.backend.modules.inventory_history.entity.InventoryHistory;
import com.ecommerce.backend.modules.product.entity.Product;
import com.ecommerce.backend.modules.product.entity.ProductVariant;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inventories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @Tsid
    private Long id;

    private Long quantity;

    private Instant updatedAt;

    @OneToOne
    @JoinColumn(name = "product_variant_id", nullable = false, unique = true)
    private ProductVariant productVariant;

    @OneToMany(mappedBy = "inventory")
    @Builder.Default
    private List<InventoryHistory> inventoryHistories = new ArrayList<>();
}
