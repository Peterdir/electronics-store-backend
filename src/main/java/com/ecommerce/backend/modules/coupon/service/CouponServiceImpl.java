package com.ecommerce.backend.modules.coupon.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.common.exception.DuplicateResourceException;
import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.modules.auth.repository.UserRepository;
import com.ecommerce.backend.modules.coupon.dto.request.CouponRequest;
import com.ecommerce.backend.modules.coupon.dto.response.CouponResponse;
import com.ecommerce.backend.modules.coupon.entity.Coupon;
import com.ecommerce.backend.modules.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final UserRepository userRepository;

    @Override
    public List<CouponResponse> getAllCoupons() {
        return couponRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public CouponResponse createCoupon(CouponRequest request) {
        if (couponRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("This coupon code already exists. Please choose a different code.");
        }

        validateDateRange(request);

        Coupon coupon = Coupon.builder()
                .code(request.getCode())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minOrderValue(request.getMinOrderValue())
                .usageLimit(request.getUsageLimit())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isActive(true)
                .usedCount(0)
                .build();

        couponRepository.save(coupon);

        return mapToResponse(coupon);
    }

    @Override
    public CouponResponse updateCoupon(Long id, CouponRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found."));

        if (!coupon.getCode().equals(request.getCode()) && couponRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("This coupon code already exists. Please choose a different code.");
        }

        validateDateRange(request);

        coupon.setCode(request.getCode());
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinOrderValue(request.getMinOrderValue());
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setStartDate(request.getStartDate());
        coupon.setEndDate(request.getEndDate());

        coupon = couponRepository.save(coupon);

        return mapToResponse(coupon);
    }

    @Override
    public void toggleCouponStatus(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Not found coupon."));

        coupon.setIsActive(!coupon.getIsActive());

        couponRepository.save(coupon);
    }

    private CouponResponse mapToResponse(Coupon coupon) {
        if (coupon == null) {
            throw new ResourceNotFoundException("Not found coupon.");
        }

        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .usageLimit(coupon.getUsageLimit())
                .usedCount(coupon.getUsedCount())
                .minOrderValue(coupon.getMinOrderValue())
                .startDate(coupon.getStartDate())
                .endDate(coupon.getEndDate())
                .isActive(coupon.getIsActive())
                .build();
    }

    private void validateDateRange(CouponRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End Date cannot be earlier than Start Date.");
        }
    }
}