package com.ecommerce.backend.modules.policy.mapper;

import com.ecommerce.backend.modules.policy.dto.response.PolicyResponse;
import com.ecommerce.backend.modules.policy.entity.Policy;
import org.springframework.stereotype.Component;

@Component
public class PolicyMapper {

    public PolicyResponse toResponse(Policy policy) {
        if (policy == null) return null;

        return PolicyResponse.builder()
                .id(policy.getId())
                .title(policy.getTitle())
                .slug(policy.getSlug())
                .content(policy.getContent())
                .type(policy.getType())
                .status(policy.getStatus())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }
}
