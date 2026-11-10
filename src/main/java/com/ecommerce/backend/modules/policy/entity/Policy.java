package com.ecommerce.backend.modules.policy.entity;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.ecommerce.backend.common.utils.Tsid;
import com.ecommerce.backend.modules.policy.enums.PolicyStatus;
import com.ecommerce.backend.modules.policy.enums.PolicyType;
import jakarta.persistence.*;
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
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    private String title;

    private String content;

    @Enumerated(EnumType.STRING)
    private PolicyType type;

    private Instant createdAt;

    private Instant updatedAt;

    private String slug;

    @Enumerated(EnumType.STRING)
    private PolicyStatus status;
}
