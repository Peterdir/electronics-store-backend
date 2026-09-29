package com.ecommerce.backend.modules.coupon.entity;

import com.ecommerce.backend.common.utils.Tsid;
import com.ecommerce.backend.modules.coupon.enums.DiscountType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @Tsid
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    private Double discountValue;

    private BigDecimal minOrderValue;

    @Column(nullable = false)
    private Integer usageLimit; // Tổng lượt cho phép sử dụng

    @Builder.Default
    private Integer usedCount = 0; // Số lượt đã dùng

    private Instant startDate;

    private Instant endDate;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
