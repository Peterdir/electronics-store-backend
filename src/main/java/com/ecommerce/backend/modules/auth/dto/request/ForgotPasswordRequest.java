package com.ecommerce.backend.modules.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {

    @NotBlank(message = "This field is required.")
    @Email(message = "Invalid email address.")
    private String email;
}
