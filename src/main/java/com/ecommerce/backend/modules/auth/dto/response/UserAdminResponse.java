package com.ecommerce.backend.modules.auth.dto.response;

import com.ecommerce.backend.modules.auth.enums.Role;
import com.ecommerce.backend.modules.auth.enums.UserStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserAdminResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    private String fullName;

    private String email;

    private String phone;

    private Role role;

    private UserStatus status;

}
