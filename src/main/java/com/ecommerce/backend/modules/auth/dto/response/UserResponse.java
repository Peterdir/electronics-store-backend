package com.ecommerce.backend.modules.auth.dto.response;

import com.ecommerce.backend.modules.auth.enums.UserStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private UserStatus status;
}
