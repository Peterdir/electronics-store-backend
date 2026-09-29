package com.ecommerce.backend.modules.coupon.dto.request;

import com.ecommerce.backend.modules.coupon.enums.DiscountType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class CouponRequest {

    @NotBlank(message = "Coupon code is required")
    private String code;

    @NotNull(message = "Discount type is required")
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @Min(0)
    private Double discountValue;

    @NotNull(message = "Min order value is required")
    @Min(0)
    private BigDecimal minOrderValue;

    @NotNull(message = "Usage limit is required")
    @Min(1)
    private Integer usageLimit;

    @NotNull(message = "Start date is required")
    private Instant startDate;

    @NotNull(message = "End date is required")
    private Instant endDate;
}
