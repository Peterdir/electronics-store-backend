package com.ecommerce.backend.modules.category.dto.request;

import com.ecommerce.backend.modules.category.enums.CategoryStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryRequest {

    @NotBlank
    private String name;

    private CategoryStatus status;
}
