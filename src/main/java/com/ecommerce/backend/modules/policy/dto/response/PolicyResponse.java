package com.ecommerce.backend.modules.policy.dto.response;

import com.ecommerce.backend.modules.policy.enums.PolicyStatus;
import com.ecommerce.backend.modules.policy.enums.PolicyType;
import lombok.*;


import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyResponse {

    private Long id;

    private String title;

    private String slug;

    private String content;

    private PolicyType type;

    private PolicyStatus status;

    private Instant updatedAt;
}
