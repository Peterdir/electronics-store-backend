package com.ecommerce.backend.modules.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "This field is required")
    @Email(message = "")
    private String email;

    @NotBlank(message = "This field is required")
    private String password;


}
