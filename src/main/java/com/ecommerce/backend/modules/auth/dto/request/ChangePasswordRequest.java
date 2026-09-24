package com.ecommerce.backend.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "This field is required.")
    private String currentPassword;

    @NotBlank(message = "This field is required.")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).{8,}$", message = "Password must be at least 8 characters long and contain uppercase, lowercase, digit and special character.")
    private String newPassword;

    @NotBlank(message = "This field is required.")
    private String confirmPassword;
}
