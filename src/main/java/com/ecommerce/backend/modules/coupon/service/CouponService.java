package com.ecommerce.backend.modules.coupon.service;

import com.ecommerce.backend.modules.coupon.dto.request.CouponRequest;
import com.ecommerce.backend.modules.coupon.dto.response.CouponResponse;

import java.util.List;

public interface CouponService {

    List<CouponResponse> getAllCoupons();

    CouponResponse createCoupon(CouponRequest request);

    CouponResponse updateCoupon(Long id, CouponRequest request);

    void toggleCouponStatus(Long id);
}
