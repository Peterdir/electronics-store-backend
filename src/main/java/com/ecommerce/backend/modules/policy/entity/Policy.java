package com.ecommerce.backend.modules.policy.entity;

import com.ecommerce.backend.common.utils.Tsid;
import com.ecommerce.backend.modules.policy.enums.PolicyType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "policies")
public class Policy {

    @Id
    @Tsid
    private Long id;

    private String title;
    private String content;
    private PolicyType type;
    private Instant createdAt;
    private Instant updatedAt;
}
