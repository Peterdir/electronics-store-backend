package com.ecommerce.backend.modules.coupon.repository;

import com.ecommerce.backend.modules.coupon.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Boolean existsByCode(String code);

    Optional<Coupon> findByCode(String code);
}
