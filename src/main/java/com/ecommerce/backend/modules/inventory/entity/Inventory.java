package com.ecommerce.backend.modules.inventory.entity;

import com.ecommerce.backend.common.utils.Tsid;
import com.ecommerce.backend.modules.product.entity.Product;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "inventories")
public class Inventory {

    @Id
    @Tsid
    private Long id;

    private Long quantity;
    private Instant updated;

    @OneToMany(mappedBy = "inventory")
    List<Product> products;
}
