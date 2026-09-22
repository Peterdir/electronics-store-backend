package com.ecommerce.backend.modules.auth.dto.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthResponse {
    private String accessToken;

    private String tokenType; // Bearer
}
