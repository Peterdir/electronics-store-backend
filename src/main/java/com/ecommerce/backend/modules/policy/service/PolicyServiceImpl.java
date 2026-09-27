package com.ecommerce.backend.modules.policy.service;

import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.modules.policy.repository.PolicyRepository;
import com.ecommerce.backend.modules.policy.dto.response.PolicyResponse;
import com.ecommerce.backend.modules.policy.entity.Policy;
import com.ecommerce.backend.modules.policy.enums.PolicyStatus;
import com.ecommerce.backend.modules.policy.mapper.PolicyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyServiceImpl implements PolicyService {

    private final PolicyRepository policyRepository;
    private final PolicyMapper policyMapper;

    @Override
    public PolicyResponse getPolicyBySlug(String slug) {
        Policy policy = policyRepository.findBySlugAndStatus(slug, PolicyStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with slug: " + slug));

        return policyMapper.toResponse(policy);
    }
}
