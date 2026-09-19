package com.ecommerce.backend.modules.product.entity;

import com.ecommerce.backend.common.utils.Tsid;
import com.ecommerce.backend.modules.brand.entity.Brand;
import com.ecommerce.backend.modules.category.entity.Category;
import com.ecommerce.backend.modules.inventory.entity.Inventory;
import com.ecommerce.backend.modules.product.enums.ProductStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @Tsid
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    private String name;
    private Double price;
    private String description;
    private ProductStatus status;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> specification = new HashMap<>();

    private Instant createdAt;
    private Instant updatedAt;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "product")
    private List<ProductImage> productImages;

    @ManyToOne
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @ManyToOne
    @JoinColumn(name = "inventory_id")
    private Inventory inventory;
}
