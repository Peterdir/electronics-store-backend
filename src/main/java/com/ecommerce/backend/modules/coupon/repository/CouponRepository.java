package com.ecommerce.backend.modules.coupon.repository;

import com.ecommerce.backend.modules.coupon.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Boolean existsByCode(String code);

    Optional<Coupon> findByCode(String code);

    @Modifying
    @Query("""
                UPDATE Coupon c
                SET c.usedCount = COALESCE(c.usedCount, 0) + 1, c.updatedAt = :now
                WHERE c.id = :id
                    AND (c.usageLimit IS NULL OR COALESCE(c.usedCount, 0) < c.usageLimit)
            """)
    int incrementUsedCountIfAvailable(@Param("id") Long id, @Param("now") Instant now);
}
