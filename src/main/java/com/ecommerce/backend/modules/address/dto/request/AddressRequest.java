package com.ecommerce.backend.modules.address.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AddressRequest {

    @NotBlank(message = "This field is required.")
    private String fullName;

    @NotBlank(message = "This field is required.")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "Invalid phone number format.")
    private String phone;

    @NotBlank(message = "This field is required.")
    private String province;

    @NotBlank(message = "This field is required.")
    private String district;

    @NotBlank(message = "This field is required.")
    private String ward;

    @NotBlank(message = "This field is required.")
    private String specificAddress;

    private Boolean isDefault;
}
