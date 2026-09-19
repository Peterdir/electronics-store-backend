package com.ecommerce.backend.modules.product.entity;

import jakarta.persistence.*;

@Entity
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String url;

    private String color;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
}
