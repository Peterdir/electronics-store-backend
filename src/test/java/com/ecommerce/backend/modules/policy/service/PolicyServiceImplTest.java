package com.ecommerce.backend.modules.policy.service;

import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.modules.policy.dto.response.PolicyResponse;
import com.ecommerce.backend.modules.policy.entity.Policy;
import com.ecommerce.backend.modules.policy.enums.PolicyStatus;
import com.ecommerce.backend.modules.policy.mapper.PolicyMapper;
import com.ecommerce.backend.modules.policy.repository.PolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PolicyServiceImpl Unit Test Suite")
class PolicyServiceImplTest {

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private PolicyMapper policyMapper;

    @InjectMocks
    private PolicyServiceImpl policyService;

    private Policy testPolicy;
    private PolicyResponse testResponse;

    @BeforeEach
    void setUp() {
        testPolicy = new Policy();
        testPolicy.setId(1L);
        testPolicy.setTitle("Privacy Policy");
        testPolicy.setSlug("privacy-policy");
        testPolicy.setStatus(PolicyStatus.ACTIVE);

        testResponse = PolicyResponse.builder()
                .id(1L)
                .title("Privacy Policy")
                .slug("privacy-policy")
                .build();
    }

    // =========================================================================
    // AC-POLICY-01: Lấy chính sách theo slug (getPolicyBySlug)
    // =========================================================================
    @Nested
    @DisplayName("AC-POLICY-01: Get Policy By Slug Tests")
    class GetPolicyBySlugTests {

        @Test
        @DisplayName("TC-POLICY-01 [Positive]: Should return active policy by slug successfully")
        void getPolicyBySlug_Success() {
            when(policyRepository.findBySlugAndStatus("privacy-policy", PolicyStatus.ACTIVE))
                    .thenReturn(Optional.of(testPolicy));
            when(policyMapper.toResponse(testPolicy)).thenReturn(testResponse);

            PolicyResponse response = policyService.getPolicyBySlug("privacy-policy");

            assertNotNull(response);
            assertEquals("privacy-policy", response.getSlug());
            verify(policyRepository, times(1)).findBySlugAndStatus("privacy-policy", PolicyStatus.ACTIVE);
        }

        @Test
        @DisplayName("TC-POLICY-02 [Negative]: Should throw ResourceNotFoundException when policy not found or inactive")
        void getPolicyBySlug_NotFound_ThrowsException() {
            when(policyRepository.findBySlugAndStatus("invalid-slug", PolicyStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                    policyService.getPolicyBySlug("invalid-slug")
            );

            assertThat(ex.getMessage()).contains("Policy not found with slug: invalid-slug");
        }
    }
}
