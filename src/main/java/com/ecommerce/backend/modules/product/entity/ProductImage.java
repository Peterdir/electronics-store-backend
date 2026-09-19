package com.ecommerce.backend.modules.product.entity;

import com.ecommerce.backend.common.utils.Tsid;
import jakarta.persistence.*;

@Entity
@Table(name = "product_images")
public class ProductImage {

    @Id
    @Tsid
    private Long id;

    private String imageUrl;

    private int displayOrder;
    private Boolean isPrimary;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
}
