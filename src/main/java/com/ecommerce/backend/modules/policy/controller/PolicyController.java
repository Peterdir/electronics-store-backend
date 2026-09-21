package com.ecommerce.backend.modules.policy.controller;

import com.ecommerce.backend.modules.policy.dto.response.PolicyResponse;
import com.ecommerce.backend.modules.policy.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    @GetMapping("/{slug}")
    public ResponseEntity<PolicyResponse> getPolicyBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(policyService.getPolicyBySlug(slug));
    }
}
