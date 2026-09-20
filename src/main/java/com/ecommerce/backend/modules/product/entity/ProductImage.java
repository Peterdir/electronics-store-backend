package com.ecommerce.backend.modules.product.entity;

import com.ecommerce.backend.common.utils.Tsid;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage {

    @Id
    @Tsid
    private Long id;

    private String imageUrl;

    private int displayOrder;
    private Boolean isPrimary;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne
    @JoinColumn(name = "product_variant_id")
    private ProductVariant productVariant;
}
