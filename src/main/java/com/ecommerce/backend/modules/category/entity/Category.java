package com.ecommerce.backend.modules.category.entity;

import com.ecommerce.backend.modules.product.entity.Product;
import jakarta.persistence.*;

import java.util.List;

@Entity
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "category")
    private List<Product> products;
}
