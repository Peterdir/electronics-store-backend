package com.ecommerce.backend.modules.product.entity;

import com.ecommerce.backend.modules.brand.entity.Brand;
import com.ecommerce.backend.modules.category.entity.Category;
import jakarta.persistence.*;

import java.util.List;

@Entity

public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private long price;
    private long stock;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "product")
    private List<ProductImage> productImages;

    @ManyToOne
    @JoinColumn(name = "brand_id")
    private Brand brand;
}
