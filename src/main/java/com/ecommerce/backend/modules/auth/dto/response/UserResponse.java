package com.ecommerce.backend.modules.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.ecommerce.backend.modules.auth.enums.UserStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;
    private String email;
    private UserStatus status;
}
