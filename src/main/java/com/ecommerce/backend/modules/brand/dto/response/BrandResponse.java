package com.ecommerce.backend.modules.brand.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.ecommerce.backend.modules.brand.enums.BrandStatus;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    private String name;

    private String logo;

    private BrandStatus status;

    private Long totalProducts;

    private Instant createdAt;

    private Instant updatedAt;

}
