package com.ecommerce.backend.modules.policy;

import com.ecommerce.backend.modules.policy.entity.Policy;
import com.ecommerce.backend.modules.policy.enums.PolicyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {

    Optional<Policy> findBySlugAndStatus(String slug, PolicyStatus status);
}
