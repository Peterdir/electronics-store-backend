package com.ecommerce.backend.modules.category.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.ecommerce.backend.modules.category.enums.CategoryStatus;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    private String name;

    private CategoryStatus status;

    private Long totalProducts;

    private Instant createdAt;

    private Instant updatedAt;
}
