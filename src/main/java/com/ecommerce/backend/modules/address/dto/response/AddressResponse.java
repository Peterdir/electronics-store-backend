package com.ecommerce.backend.modules.address.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AddressResponse {

    private Long id;

    private String fullName;

    private String phone;

    private String province;

    private String district;

    private String ward;

    private String specificAddress;

    private Boolean isDefault;
}
