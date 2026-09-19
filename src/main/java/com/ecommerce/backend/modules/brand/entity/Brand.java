package com.ecommerce.backend.modules.brand.entity;

import com.ecommerce.backend.modules.product.entity.Product;
import jakarta.persistence.*;

import java.util.List;

@Entity
public class Brand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "brand")
    private List<Product> products;
}
