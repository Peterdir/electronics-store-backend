package com.ecommerce.backend.modules.brand.dto.request;

import com.ecommerce.backend.modules.brand.enums.BrandStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandRequest {

    @NotBlank(message = "Brand name is required.")
    private String name;

    private BrandStatus status;
}
