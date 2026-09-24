package com.ecommerce.backend.modules.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;
    private String fullName;
    private String email;
    private String phone;
}
