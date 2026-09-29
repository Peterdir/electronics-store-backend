package com.ecommerce.backend.modules.coupon.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.common.exception.DuplicateResourceException;
import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.modules.auth.repository.UserRepository;
import com.ecommerce.backend.modules.coupon.dto.request.CouponRequest;
import com.ecommerce.backend.modules.coupon.dto.response.CouponResponse;
import com.ecommerce.backend.modules.coupon.entity.Coupon;
import com.ecommerce.backend.modules.coupon.enums.DiscountType;
import com.ecommerce.backend.modules.coupon.repository.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CouponServiceImpl Unit Test Suite")
class CouponServiceImplTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CouponServiceImpl couponService;

    private Coupon testCoupon;
    private CouponRequest testRequest;
    private Instant now;
    private Instant futureDate;

    @BeforeEach
    void setUp() {
        now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        futureDate = now.plus(7, ChronoUnit.DAYS);

        testCoupon = Coupon.builder()
                .id(1L)
                .code("SUMMER2026")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(15.0)
                .minOrderValue(BigDecimal.valueOf(200_000))
                .usageLimit(100)
                .usedCount(5)
                .startDate(now)
                .endDate(futureDate)
                .isActive(true)
                .build();

        testRequest = new CouponRequest();
        testRequest.setCode("SUMMER2026");
        testRequest.setDiscountType(DiscountType.PERCENTAGE);
        testRequest.setDiscountValue(15.0);
        testRequest.setMinOrderValue(BigDecimal.valueOf(200_000));
        testRequest.setUsageLimit(100);
        testRequest.setStartDate(now);
        testRequest.setEndDate(futureDate);
    }

    // =========================================================================
    // AC-COUPON-01: Lấy danh sách phiếu giảm giá (getAllCoupons)
    // =========================================================================
    @Nested
    @DisplayName("AC-COUPON-01: Get All Coupons Tests")
    class GetAllCouponsTests {

        @Test
        @DisplayName("TC-COUPON-01 [Positive]: Should return list of coupons when coupons exist")
        void getAllCoupons_WhenCouponsExist_ReturnsList() {
            // Arrange
            when(couponRepository.findAll()).thenReturn(List.of(testCoupon));

            // Act
            List<CouponResponse> responses = couponService.getAllCoupons();

            // Assert
            assertNotNull(responses);
            assertEquals(1, responses.size());
            CouponResponse response = responses.get(0);
            assertEquals(testCoupon.getId(), response.getId());
            assertEquals("SUMMER2026", response.getCode());
            assertEquals(DiscountType.PERCENTAGE, response.getDiscountType());
            assertEquals(15.0, response.getDiscountValue());
            assertEquals(BigDecimal.valueOf(200_000), response.getMinOrderValue());
            assertEquals(100, response.getUsageLimit());
            assertEquals(5, response.getUsedCount());
            assertTrue(response.isActive());

            verify(couponRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("TC-COUPON-02 [Boundary]: Should return empty list when no coupons exist")
        void getAllCoupons_WhenNoCoupons_ReturnsEmptyList() {
            // Arrange
            when(couponRepository.findAll()).thenReturn(Collections.emptyList());

            // Act
            List<CouponResponse> responses = couponService.getAllCoupons();

            // Assert
            assertNotNull(responses);
            assertTrue(responses.isEmpty());
            verify(couponRepository, times(1)).findAll();
        }
    }

    // =========================================================================
    // AC-COUPON-02: Tạo mới phiếu giảm giá (createCoupon)
    // =========================================================================
    @Nested
    @DisplayName("AC-COUPON-02: Create Coupon Tests")
    class CreateCouponTests {

        @Test
        @DisplayName("TC-COUPON-03 [Positive]: Should create PERCENTAGE coupon successfully")
        void createCoupon_ValidPercentageRequest_Success() {
            // Arrange
            when(couponRepository.existsByCode("SUMMER2026")).thenReturn(false);
            when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> {
                Coupon c = invocation.getArgument(0);
                c.setId(10L);
                return c;
            });

            // Act
            CouponResponse response = couponService.createCoupon(testRequest);

            // Assert
            assertNotNull(response);
            assertEquals("SUMMER2026", response.getCode());
            assertEquals(DiscountType.PERCENTAGE, response.getDiscountType());
            assertEquals(15.0, response.getDiscountValue());
            assertEquals(0, response.getUsedCount());
            assertTrue(response.isActive());

            verify(couponRepository, times(1)).existsByCode("SUMMER2026");
            verify(couponRepository, times(1)).save(any(Coupon.class));
        }

        @Test
        @DisplayName("TC-COUPON-04 [Positive]: Should create FIXED_AMOUNT coupon successfully")
        void createCoupon_ValidFixedAmountRequest_Success() {
            // Arrange
            testRequest.setCode("FIXED50K");
            testRequest.setDiscountType(DiscountType.FIXED_AMOUNT);
            testRequest.setDiscountValue(50_000.0);

            when(couponRepository.existsByCode("FIXED50K")).thenReturn(false);
            when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> {
                Coupon c = invocation.getArgument(0);
                c.setId(11L);
                return c;
            });

            // Act
            CouponResponse response = couponService.createCoupon(testRequest);

            // Assert
            assertNotNull(response);
            assertEquals("FIXED50K", response.getCode());
            assertEquals(DiscountType.FIXED_AMOUNT, response.getDiscountType());
            assertEquals(50_000.0, response.getDiscountValue());

            verify(couponRepository, times(1)).save(any(Coupon.class));
        }

        @Test
        @DisplayName("TC-COUPON-05 [Negative]: Should throw DuplicateResourceException when code already exists")
        void createCoupon_DuplicateCode_ThrowsDuplicateResourceException() {
            // Arrange
            when(couponRepository.existsByCode("SUMMER2026")).thenReturn(true);

            // Act & Assert
            DuplicateResourceException exception = assertThrows(DuplicateResourceException.class, () ->
                    couponService.createCoupon(testRequest)
            );

            assertThat(exception.getMessage()).contains("already exists");
            verify(couponRepository, never()).save(any(Coupon.class));
        }

        @Test
        @DisplayName("TC-COUPON-06 [Negative/Boundary]: Should throw BadRequestException when endDate is before startDate")
        void createCoupon_EndDateBeforeStartDate_ThrowsBadRequestException() {
            // Arrange
            testRequest.setStartDate(now.plus(5, ChronoUnit.DAYS));
            testRequest.setEndDate(now.plus(2, ChronoUnit.DAYS)); // End before start

            when(couponRepository.existsByCode(anyString())).thenReturn(false);

            // Act & Assert
            BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    couponService.createCoupon(testRequest)
            );

            assertThat(exception.getMessage()).contains("End Date cannot be earlier than Start Date");
            verify(couponRepository, never()).save(any(Coupon.class));
        }

        @Test
        @DisplayName("TC-COUPON-07 [Boundary]: Should succeed when startDate equals endDate (same second)")
        void createCoupon_StartDateEqualsEndDate_Success() {
            // Arrange
            testRequest.setStartDate(now);
            testRequest.setEndDate(now);

            when(couponRepository.existsByCode("SUMMER2026")).thenReturn(false);
            when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            CouponResponse response = couponService.createCoupon(testRequest);

            // Assert
            assertNotNull(response);
            assertEquals(now, response.getStartDate());
            assertEquals(now, response.getEndDate());
            verify(couponRepository, times(1)).save(any(Coupon.class));
        }
    }

    // =========================================================================
    // AC-COUPON-03: Cập nhật phiếu giảm giá (updateCoupon)
    // =========================================================================
    @Nested
    @DisplayName("AC-COUPON-03: Update Coupon Tests")
    class UpdateCouponTests {

        @Test
        @DisplayName("TC-COUPON-08 [Positive]: Should update coupon successfully when code remains unchanged")
        void updateCoupon_SameCode_Success() {
            // Arrange
            when(couponRepository.findById(1L)).thenReturn(Optional.of(testCoupon));
            when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> invocation.getArgument(0));

            testRequest.setDiscountValue(20.0);
            testRequest.setUsageLimit(200);

            // Act
            CouponResponse response = couponService.updateCoupon(1L, testRequest);

            // Assert
            assertNotNull(response);
            assertEquals(20.0, response.getDiscountValue());
            assertEquals(200, response.getUsageLimit());
            verify(couponRepository, never()).existsByCode(anyString());
            verify(couponRepository, times(1)).save(testCoupon);
        }

        @Test
        @DisplayName("TC-COUPON-09 [Positive]: Should update coupon successfully when changing to a new unique code")
        void updateCoupon_NewUniqueCode_Success() {
            // Arrange
            testRequest.setCode("AUTUMN2026");
            when(couponRepository.findById(1L)).thenReturn(Optional.of(testCoupon));
            when(couponRepository.existsByCode("AUTUMN2026")).thenReturn(false);
            when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            CouponResponse response = couponService.updateCoupon(1L, testRequest);

            // Assert
            assertNotNull(response);
            assertEquals("AUTUMN2026", response.getCode());
            verify(couponRepository, times(1)).existsByCode("AUTUMN2026");
            verify(couponRepository, times(1)).save(testCoupon);
        }

        @Test
        @DisplayName("TC-COUPON-10 [Negative]: Should throw ResourceNotFoundException when coupon ID does not exist")
        void updateCoupon_NotFound_ThrowsResourceNotFoundException() {
            // Arrange
            when(couponRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                    couponService.updateCoupon(999L, testRequest)
            );

            assertThat(exception.getMessage()).contains("Coupon not found");
            verify(couponRepository, never()).save(any(Coupon.class));
        }

        @Test
        @DisplayName("TC-COUPON-11 [Negative]: Should throw DuplicateResourceException when new code already belongs to another coupon")
        void updateCoupon_NewCodeAlreadyExists_ThrowsDuplicateResourceException() {
            // Arrange
            testRequest.setCode("EXISTING_CODE");
            when(couponRepository.findById(1L)).thenReturn(Optional.of(testCoupon));
            when(couponRepository.existsByCode("EXISTING_CODE")).thenReturn(true);

            // Act & Assert
            DuplicateResourceException exception = assertThrows(DuplicateResourceException.class, () ->
                    couponService.updateCoupon(1L, testRequest)
            );

            assertThat(exception.getMessage()).contains("already exists");
            verify(couponRepository, never()).save(any(Coupon.class));
        }

        @Test
        @DisplayName("TC-COUPON-12 [Negative/Boundary]: Should throw BadRequestException when updating with endDate earlier than startDate")
        void updateCoupon_InvalidDateRange_ThrowsBadRequestException() {
            // Arrange
            when(couponRepository.findById(1L)).thenReturn(Optional.of(testCoupon));
            testRequest.setStartDate(now.plus(10, ChronoUnit.DAYS));
            testRequest.setEndDate(now.plus(5, ChronoUnit.DAYS));

            // Act & Assert
            BadRequestException exception = assertThrows(BadRequestException.class, () ->
                    couponService.updateCoupon(1L, testRequest)
            );

            assertThat(exception.getMessage()).contains("End Date cannot be earlier than Start Date");
            verify(couponRepository, never()).save(any(Coupon.class));
        }
    }

    // =========================================================================
    // AC-COUPON-04: Đổi trạng thái kích hoạt phiếu giảm giá (toggleCouponStatus)
    // =========================================================================
    @Nested
    @DisplayName("AC-COUPON-04: Toggle Coupon Status Tests")
    class ToggleCouponStatusTests {

        @Test
        @DisplayName("TC-COUPON-13 [Positive]: Should toggle active status from true to false")
        void toggleCouponStatus_FromActiveToInactive_Success() {
            // Arrange
            testCoupon.setIsActive(true);
            when(couponRepository.findById(1L)).thenReturn(Optional.of(testCoupon));

            // Act
            couponService.toggleCouponStatus(1L);

            // Assert
            assertFalse(testCoupon.getIsActive());
            verify(couponRepository, times(1)).save(testCoupon);
        }

        @Test
        @DisplayName("TC-COUPON-14 [Positive]: Should toggle active status from false to true")
        void toggleCouponStatus_FromInactiveToActive_Success() {
            // Arrange
            testCoupon.setIsActive(false);
            when(couponRepository.findById(1L)).thenReturn(Optional.of(testCoupon));

            // Act
            couponService.toggleCouponStatus(1L);

            // Assert
            assertTrue(testCoupon.getIsActive());
            verify(couponRepository, times(1)).save(testCoupon);
        }

        @Test
        @DisplayName("TC-COUPON-15 [Negative]: Should throw ResourceNotFoundException when toggling non-existent coupon")
        void toggleCouponStatus_NotFound_ThrowsResourceNotFoundException() {
            // Arrange
            when(couponRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                    couponService.toggleCouponStatus(999L)
            );

            assertThat(exception.getMessage()).contains("Not found coupon");
            verify(couponRepository, never()).save(any(Coupon.class));
        }
    }
}
