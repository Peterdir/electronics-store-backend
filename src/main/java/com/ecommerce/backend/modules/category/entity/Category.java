package com.ecommerce.backend.modules.category.entity;

import com.ecommerce.backend.common.utils.Tsid;
import com.ecommerce.backend.modules.category.enums.CategoryStatus;
import com.ecommerce.backend.modules.product.entity.Product;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @Tsid
    private Long id;

    private String name;
    private CategoryStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    @OneToMany(mappedBy = "category")
    private List<Product> products;
}
