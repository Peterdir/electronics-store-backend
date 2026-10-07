package com.ecommerce.backend.modules.coupon.dto.response;

import com.ecommerce.backend.modules.coupon.enums.DiscountType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class CouponResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    private String code;

    private DiscountType discountType;

    private Double discountValue;

    private BigDecimal minOrderValue;

    private Integer usageLimit;

    private Integer usedCount;

    private Instant startDate;

    private Instant endDate;

    private boolean isActive;
}
