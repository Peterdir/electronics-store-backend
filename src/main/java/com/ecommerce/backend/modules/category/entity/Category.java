package com.ecommerce.backend.modules.category.entity;

import com.ecommerce.backend.common.utils.Tsid;
import com.ecommerce.backend.modules.category.enums.CategoryStatus;
import com.ecommerce.backend.modules.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @Tsid
    private Long id;

    private String name;

    private CategoryStatus status;

    private Instant createdAt;

    private Instant updatedAt;

    @OneToMany(mappedBy = "category")
    @Builder.Default
    private List<Product> products = new ArrayList<>();
}
