package com.ecommerce.backend.modules.policy.entity;

import com.ecommerce.backend.common.utils.Tsid;
import com.ecommerce.backend.modules.policy.enums.PolicyStatus;
import com.ecommerce.backend.modules.policy.enums.PolicyType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Policy {

    @Id
    @Tsid
    private Long id;

    private String title;

    private String content;

    private PolicyType type;

    private Instant createdAt;

    private Instant updatedAt;

    private String slug;

    private PolicyStatus status;
}
