package com.ecommerce.backend.modules.brand.entity;

import com.ecommerce.backend.common.utils.Tsid;
import com.ecommerce.backend.modules.brand.enums.BrandStatus;
import com.ecommerce.backend.modules.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "brands")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Brand {

    @Id
    @Tsid
    private Long id;

    private String name;

    private BrandStatus status;

    private String logo;

    private Instant createdAt;

    private Instant updatedAt;

    @OneToMany(mappedBy = "brand")
    @Builder.Default
    private List<Product> products = new ArrayList<>();
}
