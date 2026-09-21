package com.ecommerce.backend.modules.policy.service;

import com.ecommerce.backend.modules.policy.dto.response.PolicyResponse;

public interface PolicyService {

    PolicyResponse getPolicyBySlug(String slug);
}
