# Kế hoạch Tối ưu Hiệu năng & Gia cố Bảo mật — Ecommerce Backend

> Tài liệu này tổng hợp các lỗ hổng bảo mật, lỗi logic/đồng thời (concurrency) và điểm nghẽn hiệu năng đã được rà soát trực tiếp trong mã nguồn, kèm lộ trình và phương án sửa chi tiết để triển khai theo từng giai đoạn.

---

## Tổng quan & Lộ trình thực hiện (Roadmap)

### Giai đoạn 1 — An toàn Giao dịch, Thanh toán VNPAY & Tồn kho (Ưu tiên Critical / High)
- [ ] **1.1.** Sửa lỗi trùng `vnp_TxnRef` khi thanh toán lại, chặn tạo link thanh toán cho đơn đã `CANCELLED` và xử lý trường hợp IPN trả về cho đơn đã hủy trong `VnPayServiceImpl`.
- [ ] **1.2.** Bổ sung khóa bi quan (`PESSIMISTIC_WRITE`) theo thứ tự tăng dần `variantId` khi hủy đơn, duyệt trả hàng và điều chỉnh tồn kho thủ công (`OrderServiceImpl`, `OrderAdminServiceImpl`, `InventoryServiceImpl`, `ReturnRequestServiceImpl`).
- [ ] **1.3.** Hoàn thiện luồng trạng thái trả hàng trong `ReturnRequestServiceImpl` (chuyển đơn sang `RETURN_REQUESTED`, ghi `InventoryHistory`, cập nhật `PaymentStatus.PENDING_REFUND`).
- [ ] **1.4.** Hoàn lại lượt dùng coupon (`usedCount`) khi hủy đơn và bổ sung validate giá trị phần trăm giảm giá (`<= 100%`) cho `Coupon`.
- [ ] **1.5.** Sửa `@EntityGraph` trong `WishlistRepository` để tránh `MultipleBagFetchException` và tích Đề-các (Cartesian product).

### Giai đoạn 2 — Bảo mật Hệ thống, Xác thực & Phân quyền
- [ ] **2.1.** Thu hồi JWT tức thì theo `userId` (`user:revoke_before:{userId}`) và `jti` trong Redis khi khóa tài khoản, đổi role hoặc đổi/đặt lại mật khẩu; bổ sung `@Transactional` cho `UserServiceImpl.changePassword`.
- [ ] **2.2.** Bổ sung Redis Rate Limiting cho `/api/auth/**` (chống brute-force & spam SMTP) và Guest Checkout (`/api/orders/checkout`); khắc phục lỗi User Enumeration ở `login()` và `forgotPassword()`.
- [ ] **2.3.** Siết chặt `SecurityConfig`: giới hạn `permitAll()` đúng `HttpMethod.GET` cho danh mục/sản phẩm công khai và loại bỏ fallback JWT secret mặc định ở môi trường production.
- [ ] **2.4.** Giới hạn kích thước trang (`size <= 100`), kiểm tra `page >= 0`, whitelist trường `sortBy` ở các Controller, và giới hạn số lượng `ids` so sánh sản phẩm.
- [ ] **2.5.** Xóa file ảnh vật lý trên đĩa sau khi transaction commit trong `ProductServiceImpl.deleteProductImage()`.

### Giai đoạn 3 — Tối ưu Hiệu năng, Truy vấn Database & Caching
- [ ] **3.1.** Loại bỏ N+1 query ở `CategoryMapper` và tải toàn bộ bảng `products` ở `BrandRepository.findAllWithBrands()` / `BrandServiceImpl.deleteBrand()` bằng DTO Projection (`LEFT JOIN ... GROUP BY`) và `existsByBrandId`.
- [ ] **3.2.** Loại bỏ truy vấn lặp/dư thừa trong vòng lặp tại `CheckoutServiceImpl` và `OrderServiceImpl.buyAgain()`.
- [ ] **3.3.** Bật `@EnableAsync` và tách việc gửi email trong `MailService` sang chạy bất đồng bộ sau khi transaction commit (`TransactionSynchronization.afterCommit`) để không giữ DB connection pool.
- [ ] **3.4.** Bổ sung `unique = true` và `@Index` trên các Entity (`Cart`, `Wishlist`, `Coupon`, `ProductVariant`, `Order`, `Product`, `Review`).
- [ ] **3.5.** Đẩy bộ lọc `status` + `Pageable` xuống tầng DB cho `InventoryServiceImpl` & `CouponServiceImpl`, bật Redis Cache (`@Cacheable` / `@CacheEvict`) cho `Category`, `Brand`, `Policy` và chuyển `spring.jpa.show-sql` sang biến môi trường.

---

## Chi tiết phương án triển khai

---

## GIAI ĐOẠN 1: AN TOÀN GIAO DỊCH, THANH TOÁN VNPAY & TỒN KHO

### 1.1. Khắc phục lỗi luồng thanh toán VNPAY (Retry Payment & Đơn đã hủy)
* **File liên quan**:
  * `src/main/java/com/ecommerce/backend/modules/payment/service/VnPayServiceImpl.java`
* **Vấn đề hiện tại**:
  1. `createPaymentUrl()` gán `vnpTxnRef = String.valueOf(orderId)`. VNPAY yêu cầu `vnp_TxnRef` phải duy nhất cho mỗi giao dịch. Khi khách hủy thanh toán (mã `24`) và bấm thanh toán lại (`/api/orders/{id}/pay`), giao dịch mới bị VNPAY từ chối do trùng `vnp_TxnRef`.
  2. `createPaymentUrl()` chưa chặn tạo URL thanh toán nếu đơn hàng đã `CANCELLED` hoặc `paymentMethod != PaymentMethod.VNPAY`.
  3. `processPayment()` vẫn gán `order.setPaymentStatus(PaymentStatus.PAID)` ngay cả khi đơn hàng đã bị hủy (`OrderStatus.CANCELLED`) và hoàn kho trước đó trong lúc chờ khách thanh toán.
* **Phương án sửa**:
  * Sinh `vnpTxnRef` kèm timestamp:
    ```java
    // Trong createPaymentUrl(Long orderId, HttpServletRequest request)
    if (order.getOrderStatus() == OrderStatus.CANCELLED) {
        throw new BadRequestException("Cannot pay for a cancelled order.");
    }
    if (order.getPaymentMethod() != PaymentMethod.VNPAY) {
        throw new BadRequestException("Order does not use VNPAY payment method.");
    }

    String vnpTxnRef = orderId + "_" + System.currentTimeMillis();
    ```
  * Tách `orderId` từ `vnp_TxnRef` trong `verifyCallback(HttpServletRequest request)`:
    ```java
    long orderId;
    long vnpAmount;
    try {
        String txnRef = vnpParams.get("vnp_TxnRef");
        if (txnRef == null || txnRef.isBlank()) {
            return new VerifyResult(VerifyStatus.INVALID_REQUEST, null, vnpParams);
        }
        String orderIdPart = txnRef.contains("_") ? txnRef.split("_")[0] : txnRef;
        orderId = Long.parseLong(orderIdPart);
        vnpAmount = Long.parseLong(vnpParams.get("vnp_Amount"));
    } catch (NumberFormatException e) {
        return new VerifyResult(VerifyStatus.INVALID_REQUEST, null, vnpParams);
    }
    ```
  * Xử lý đơn đã hủy khi IPN/Return thành công trong `processPayment(Order order, Map<String, String> vnpParams)`:
    ```java
    if (success) {
        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            // Đơn đã hủy và đã hoàn kho trước đó -> chuyển sang chờ hoàn tiền thay vì PAID
            order.setPaymentStatus(PaymentStatus.PENDING_REFUND);
        } else {
            order.setPaymentStatus(PaymentStatus.PAID);
        }
        orderRepository.save(order);
    }
    ```
  * Kiểm tra thêm trong `handleIpn()` nếu `order.getPaymentStatus() == PaymentStatus.PENDING_REFUND` thì cũng trả mã đã xử lý phù hợp, tránh ghi đè lại.

---

### 1.2. Bổ sung khóa bi quan (`PESSIMISTIC_WRITE`) chống Race Condition khi Hủy đơn & Điều chỉnh kho
* **File liên quan**:
  * `src/main/java/com/ecommerce/backend/modules/order/service/OrderServiceImpl.java` (`cancelMyOrder`, `restoreInventory`)
  * `src/main/java/com/ecommerce/backend/modules/order/service/OrderAdminServiceImpl.java` (`updateOrderStatus`, `cancelOrder`, `restoreInventory`)
  * `src/main/java/com/ecommerce/backend/modules/inventory/service/InventoryServiceImpl.java` (`adjustStock`)
  * `src/main/java/com/ecommerce/backend/modules/inventory/repository/InventoryRepository.java`
* **Vấn đề hiện tại**:
  * `OrderServiceImpl.cancelMyOrder`, `OrderAdminServiceImpl.cancelOrder`, `OrderAdminServiceImpl.updateOrderStatus` chỉ dùng `orderRepository.findById(orderId)` (không lock) và `restoreInventory` chỉ dùng `inventoryRepository.findByProductVariantId(...)` (không lock).
  * Hai request hủy đồng thời (double-click hoặc user và admin cùng thao tác) có thể cùng đọc `orderStatus = PENDING`, dẫn đến cộng hoàn kho 2 lần (Double Restock).
  * `InventoryServiceImpl.adjustStock` dùng `inventoryRepository.findById(id)` không lock, dễ gây Lost Update nếu xảy ra đồng thời với checkout.
* **Phương án sửa**:
  * Thêm query lock theo `id` vào `InventoryRepository`:
    ```java
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.id = :id")
    Optional<Inventory> findByIdForUpdate(@Param("id") Long id);
    ```
  * Dùng `orderRepository.findByIdForUpdate(orderId)` trong `cancelMyOrder`, `cancelOrder`, `updateOrderStatus`.
  * Trong `restoreInventory`, gom số lượng theo `TreeMap<Long, Integer>` (sắp xếp theo `variantId` tăng dần để đồng nhất thứ tự khóa với `CheckoutServiceImpl.deductInventory`, tránh deadlock) rồi gọi `inventoryRepository.findByProductVariantIdForUpdate(variantId)`:
    ```java
    private void restoreInventory(Order order, String reason, String performedBy) {
        if (order.getItems() == null) return;

        Map<Long, Integer> quantities = new TreeMap<>();
        for (OrderItem item : order.getItems()) {
            if (item != null && item.getProductVariant() != null && item.getProductVariant().getId() != null) {
                quantities.merge(item.getProductVariant().getId(), item.getQuantity(), Integer::sum);
            }
        }

        quantities.forEach((variantId, qty) ->
            inventoryRepository.findByProductVariantIdForUpdate(variantId).ifPresent(inventory -> {
                long currentQuantity = inventory.getQuantity() != null ? inventory.getQuantity() : 0L;
                long newQuantity = currentQuantity + qty;
                inventory.setQuantity(newQuantity);
                inventory.setUpdatedAt(Instant.now());
                inventoryRepository.save(inventory);

                inventoryHistoryRepository.save(InventoryHistory.builder()
                        .inventory(inventory)
                        .action(InventoryAction.ADD)
                        .quantityChanged((long) qty)
                        .finalStock(newQuantity)
                        .reason("Restocked from cancelled order #" + order.getId() + ": " + reason)
                        .performedBy(performedBy)
                        .createdAt(Instant.now())
                        .build());
            })
        );
    }
    ```
  * Trong `InventoryServiceImpl.adjustStock()`, thay `findById(id)` bằng `findByIdForUpdate(id)`.

---

### 1.3. Đồng bộ State Machine & Inventory History trong luồng Trả hàng (`ReturnRequest`)
* **File liên quan**:
  * `src/main/java/com/ecommerce/backend/modules/returnrequest/service/ReturnRequestServiceImpl.java`
  * `src/main/java/com/ecommerce/backend/modules/returnrequest/repository/ReturnRequestRepository.java`
* **Vấn đề hiện tại**:
  1. `createReturnRequest()` không cập nhật `order.setOrderStatus(OrderStatus.RETURN_REQUESTED)` dù `OrderStatus` có định nghĩa `DELIVERED -> RETURN_REQUESTED -> RETURNED | RETURN_REJECTED`.
  2. `processReturnRequest()` không khóa `ReturnRequest` / `Order` / `Inventory` (`ForUpdate`).
  3. Khi `APPROVED`: cộng lại tồn kho trực tiếp qua `variant.getInventory()` nhưng không lưu `InventoryHistory` và không cập nhật `order.setPaymentStatus(PaymentStatus.PENDING_REFUND)` nếu đơn hàng đã thanh toán (`PAID`).
* **Phương án sửa**:
  * Thêm `@Lock(LockModeType.PESSIMISTIC_WRITE)` vào `ReturnRequestRepository.findByIdForUpdate(id)`.
  * Trong `createReturnRequest()`: kiểm tra nếu `orderItem` lấy bằng `findByIdForUpdate`, cập nhật `order.setOrderStatus(OrderStatus.RETURN_REQUESTED)` (khi các item hoặc đơn đủ điều kiện trả hàng) và dọn ảnh đã upload nếu transaction rollback (dùng `TransactionSynchronizationManager` giống `ReviewServiceImpl.uploadReviewImage`).
  * Trong `processReturnRequest()`:
    * Dùng `returnRequestRepository.findByIdForUpdate(id)` và `inventoryRepository.findByProductVariantIdForUpdate(variantId)`.
    * Khi `APPROVED`: cộng kho, lưu `InventoryHistory` với `action = InventoryAction.ADD`, và nếu `order.getPaymentStatus() == PaymentStatus.PAID` thì cập nhật `order.setPaymentStatus(PaymentStatus.PENDING_REFUND)`.

---

### 1.4. Hoàn lượt dùng Coupon khi hủy đơn & Validate giá trị Coupon
* **File liên quan**:
  * `src/main/java/com/ecommerce/backend/modules/coupon/repository/CouponRepository.java`
  * `src/main/java/com/ecommerce/backend/modules/coupon/dto/request/CouponRequest.java`
  * `src/main/java/com/ecommerce/backend/modules/coupon/service/CouponServiceImpl.java`
  * `src/main/java/com/ecommerce/backend/modules/order/service/OrderServiceImpl.java` & `OrderAdminServiceImpl.java`
* **Vấn đề hiện tại**:
  * Khi đơn hàng có dùng coupon bị hủy, `usedCount` của coupon không được giảm lại.
  * `CouponRequest.discountValue` chưa giới hạn tối đa `100` khi `discountType == DiscountType.PERCENTAGE` (có thể nhập `> 100%`). Ngoài ra `CheckoutServiceImpl.applyCouponIfPresent` lưu `couponCode` nguyên chuỗi nhập từ request thay vì `coupon.getCode()` đã chuẩn hóa.
* **Phương án sửa**:
  * Thêm vào `CouponRepository`:
    ```java
    @Modifying
    @Query("UPDATE Coupon c SET c.usedCount = CASE WHEN COALESCE(c.usedCount, 0) > 0 THEN c.usedCount - 1 ELSE 0 END, c.updatedAt = :now WHERE c.code = :code")
    void decrementUsedCountByCode(@Param("code") String code, @Param("now") Instant now);
    ```
  * Gọi `couponRepository.decrementUsedCountByCode(order.getCouponCode(), Instant.now())` khi hủy đơn ở cả `OrderServiceImpl.cancelMyOrder`, `OrderAdminServiceImpl.cancelOrder`, `OrderAdminServiceImpl.updateOrderStatus` (nếu `StringUtils.hasText(order.getCouponCode())`).
  * Trong `CouponServiceImpl.createCoupon` / `updateCoupon`: kiểm tra nếu `request.getDiscountType() == DiscountType.PERCENTAGE && request.getDiscountValue() > 100` -> ném `BadRequestException("Percentage discount cannot exceed 100%.")`.
  * Trong `CheckoutServiceImpl.applyCouponIfPresent`: trả về `coupon.getCode()` thay vì `couponCode` thô từ user.

---

### 1.5. Sửa `@EntityGraph` trong `WishlistRepository` tránh `MultipleBagFetchException`
* **File liên quan**:
  * `src/main/java/com/ecommerce/backend/modules/wishlist/repository/WishlistRepository.java`
* **Vấn đề hiện tại**:
  * `findByUserIdOrderByCreatedAtDesc` cấu hình `@EntityGraph(attributePaths = {"product", "product.productImages", "product.variants", "product.variants.inventory"})`.
  * Cả `Product.productImages` và `Product.variants` đều là `List` (Hibernate Bag), dễ gây `MultipleBagFetchException` hoặc bùng nổ số dòng dữ liệu trả về (tích Đề-các).
* **Phương án sửa**:
  * Chỉ fetch `product` trong `@EntityGraph` và để Hibernate tự động batch load `productImages`, `variants`, `inventory` thông qua cấu hình `hibernate.default_batch_fetch_size=50` đã khai báo sẵn trong `application.properties`:
    ```java
    @EntityGraph(attributePaths = {"product"})
    List<Wishlist> findByUserIdOrderByCreatedAtDesc(Long userId);
    ```

---

## GIAI ĐOẠN 2: BẢO MẬT HỆ THỐNG, XÁC THỰC & PHÂN QUYỀN

### 2.1. Thu hồi JWT ngay lập tức khi Khóa tài khoản, Đổi Role hoặc Đổi Mật khẩu
* **File liên quan**:
  * `src/main/java/com/ecommerce/backend/security/JwtService.java`
  * `src/main/java/com/ecommerce/backend/config/SecurityConfig.java`
  * `src/main/java/com/ecommerce/backend/modules/auth/service/UserServiceImpl.java`
  * `src/main/java/com/ecommerce/backend/modules/auth/service/AuthServiceImpl.java`
* **Vấn đề hiện tại**:
  * JWT có hạn 24 giờ và chỉ bị chặn nếu user chủ động gọi `/api/auth/logout` (lưu nguyên chuỗi token vào Redis `blacklist:<token>`).
  * Khi Admin khóa user (`changeUserStatus` -> `INACTIVE`), đổi role (`changeUserRole`), hoặc user đổi/reset mật khẩu (`changePassword`, `resetPassword`), các JWT cũ vẫn hợp lệ tối đa 24 giờ.
  * `UserServiceImpl.changePassword()` đang thiếu annotation `@Transactional`.
* **Phương án sửa**:
  * Trong `JwtService.generateToken(User user)`: bổ sung `.id(UUID.randomUUID().toString())` (`jti`).
  * Trong `AuthServiceImpl.logout(String token)`: lưu blacklist theo `jwt.getId()` (`"blacklist:" + jti`) thay vì lưu nguyên chuỗi JWT dài để tiết kiệm RAM Redis.
  * Thêm helper ghi mốc thời gian thu hồi toàn bộ token cũ của user lên Redis (TTL = 24 giờ):
    ```java
    redisTemplate.opsForValue().set(
        "user:revoke_before:" + user.getId(),
        String.valueOf(Instant.now().toEpochMilli()),
        24, TimeUnit.HOURS
    );
    ```
    Gọi hàm này sau khi `changeUserStatus` (khi chuyển sang `INACTIVE`), `changeUserRole`, `changePassword` (kèm thêm `@Transactional`), và `resetPassword`.
  * Trong `SecurityConfig.jwtDecoder(StringRedisTemplate redisTemplate)`: kiểm tra cả `"blacklist:" + jti` và `"user:revoke_before:" + subject` so với `token.getIssuedAt()`:
    ```java
    OAuth2TokenValidator<Jwt> revocationValidator = token -> {
        String jti = token.getId();
        if (jti != null && Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + jti))) {
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "This token has been logged out", null));
        }
        String revokeBefore = redisTemplate.opsForValue().get("user:revoke_before:" + token.getSubject());
        if (revokeBefore != null && token.getIssuedAt() != null
                && token.getIssuedAt().toEpochMilli() < Long.parseLong(revokeBefore)) {
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Token has been revoked", null));
        }
        return OAuth2TokenValidatorResult.success();
    };
    ```

---

### 2.2. Rate Limiting (Redis) & Chống User Enumeration
* **File liên quan**:
  * Tạo mới filter: `src/main/java/com/ecommerce/backend/security/RateLimitFilter.java` (hoặc Interceptor)
  * `src/main/java/com/ecommerce/backend/modules/auth/service/AuthServiceImpl.java`
* **Vấn đề hiện tại**:
  1. Không giới hạn tần suất gọi `/api/auth/login` (brute-force), `/api/auth/register`, `/api/auth/resend-verification`, `/api/auth/forgot-password` (spam mail SMTP), và `POST /api/orders/checkout` đối với Guest (cạn kiệt tồn kho & coupon).
  2. `AuthServiceImpl.login()` trả `404` khi không tìm thấy email và `400` khi sai mật khẩu (cho phép dò email đã đăng ký).
  3. `AuthServiceImpl.forgotPassword()` trả `404` nếu email không tồn tại dù thông báo trả về là thông điệp chung chung.
* **Phương án sửa**:
  * Tạo `RateLimitFilter extends OncePerRequestFilter` dùng `StringRedisTemplate` (Fixed Window hoặc Sliding Window với `increment` + `expire`) áp dụng theo IP + endpoint:
    * `/api/auth/login`: tối đa 10 requests / 1 phút / IP.
    * `/api/auth/register`, `/api/auth/resend-verification`, `/api/auth/forgot-password`: tối đa 5 requests / 15 phút / IP.
    * `POST /api/orders/checkout` (khi không có Authorization header - Guest): tối đa 5 requests / 1 giờ / IP.
    * Nếu vượt ngưỡng, trả về `429 Too Many Requests` với JSON chuẩn `ErrorResponse`.
  * Trong `AuthServiceImpl.login()`: ném cùng một loại lỗi `BadRequestException("Invalid email or password.")` khi không tìm thấy email lẫn khi sai mật khẩu.
  * Trong `AuthServiceImpl.forgotPassword()`: chỉ gửi mail nếu tìm thấy user `ACTIVE`; luôn trả về `MessageResponse` thành công chung để không lộ email tồn tại hay không.

---

### 2.3. Siết chặt HTTP Method trong `SecurityConfig` & Bảo mật Secret Key
* **File liên quan**:
  * `src/main/java/com/ecommerce/backend/config/SecurityConfig.java`
  * `src/main/resources/application.properties`
* **Vấn đề hiện tại**:
  1. `.requestMatchers("/api/products/**").permitAll()` (tương tự với `/api/categories/**`, `/api/brands/**`, `/api/inventory/**`, `/api/policies/**`) cho phép tất cả HTTP methods thay vì chỉ `HttpMethod.GET`.
  2. `app.jwt.secret` đang có chuỗi mặc định (`mySecretKeyThatIsAtLeast256BitsLongForHS256Algorithm!!`) trực tiếp trong code và `application.properties`.
* **Phương án sửa**:
  * Chỉ định rõ `HttpMethod.GET` trong `SecurityConfig`:
    ```java
    .requestMatchers(HttpMethod.GET,
            "/api/products/**",
            "/api/categories/**",
            "/api/brands/**",
            "/api/inventory/**",
            "/api/policies/**").permitAll()
    ```
  * Tách cấu hình production (`application-prod.properties`) yêu cầu bắt buộc biến môi trường `${JWT_SECRET}` (không có fallback mặc định), đồng thời cấu hình tắt Swagger UI (`/swagger-ui/**`, `/v3/api-docs/**`) trên môi trường production.

---

### 2.4. Chuẩn hóa Phân trang, Whitelist Sort & Giới hạn đầu vào
* **File liên quan**:
  * `src/main/java/com/ecommerce/backend/modules/order/controller/OrderController.java`
  * `src/main/java/com/ecommerce/backend/modules/product/controller/ProductController.java`
  * Các Admin Controller (`AdminOrderController`, `AdminProductController`, `AdminUserController`, `AdminReviewController`, `AdminReturnRequestController`)
* **Vấn đề hiện tại**:
  * `size` truyền từ client không có giới hạn trần (`size = 1_000_000` gây OOM/DoS DB), `page` không kiểm tra âm.
  * `OrderController.getMyOrders` nhận thẳng `sortBy` từ client đưa vào `Sort.by(sortBy)` -> dễ bị lộ thông tin hoặc lỗi `500` nếu truyền sai tên field hoặc nested property (`user.password`).
  * `ProductController.getProductsForComparison` nhận `List<Long> ids` không giới hạn kích thước.
* **Phương án sửa**:
  * Tạo tiện ích `PaginationUtils`:
    ```java
    public static Pageable createPageable(int page, int size, Sort sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(safePage, safeSize, sort);
    }
    ```
  * Whitelist `sortBy` ở `OrderController.getMyOrders`:
    ```java
    private static final Set<String> ALLOWED_ORDER_SORTS = Set.of("createdAt", "totalPrice", "updatedAt");
    String safeSortBy = ALLOWED_ORDER_SORTS.contains(sortBy) ? sortBy : "createdAt";
    ```
  * Giới hạn `ids.size() <= 5` (hoặc `<= 10`) trong `ProductServiceImpl.getProductsForComparison(List<Long> ids)`.

---

### 2.5. Dọn dẹp file ảnh vật lý khi xóa ảnh sản phẩm
* **File liên quan**:
  * `src/main/java/com/ecommerce/backend/modules/product/service/ProductServiceImpl.java` (`deleteProductImage`)
* **Vấn đề hiện tại**:
  * `deleteProductImage()` xóa record `ProductImage` khỏi DB nhưng không xóa file ảnh tương ứng trên ổ đĩa qua `fileStorageService.deleteFile(...)`.
* **Phương án sửa**:
  * Lưu lại `imageUrl` trước khi `removeIf`, và đăng ký xóa sau khi transaction commit (để an toàn nếu transaction rollback):
    ```java
    ProductImage toDelete = product.getProductImages().stream()
            .filter(img -> img.getId().equals(imageId))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Image not found"));

    String deletedUrl = toDelete.getImageUrl();
    product.getProductImages().remove(toDelete);
    // ... cập nhật primary image ...
    productRepository.save(product);

    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
            fileStorageService.deleteFile(deletedUrl);
        }
    });
    ```

---

## GIAI ĐOẠN 3: TỐI ƯU HIỆU NĂNG, TRUY VẤN DATABASE & CACHING

### 3.1. Xử lý N+1 Query ở `Category` & Full-table Load ở `Brand`
* **File liên quan**:
  * `src/main/java/com/ecommerce/backend/modules/category/repository/CategoryRepository.java`
  * `src/main/java/com/ecommerce/backend/modules/category/service/CategoryServiceImpl.java`
  * `src/main/java/com/ecommerce/backend/modules/category/mapper/CategoryMapper.java`
  * `src/main/java/com/ecommerce/backend/modules/brand/repository/BrandRepository.java`
  * `src/main/java/com/ecommerce/backend/modules/brand/service/BrandServiceImpl.java`
  * `src/main/java/com/ecommerce/backend/modules/product/repository/ProductRepository.java`
* **Vấn đề hiện tại**:
  1. `CategoryMapper.toResponse()` gọi `productRepository.countByCategoryId(category.getId())` cho mỗi danh mục -> sinh $1 + N$ câu SQL `COUNT`.
  2. `BrandRepository.findAllWithBrands()` dùng `@EntityGraph(attributePaths = {"products"})` kéo toàn bộ bảng `products` vào RAM chỉ để `BrandMapper` gọi `brand.getProducts().size()`.
  3. `BrandServiceImpl.deleteBrand()` gọi `brand.getProducts().isEmpty()` làm load toàn bộ sản phẩm của brand lên RAM.
* **Phương án sửa**:
  * Thêm Constructor Query (DTO Projection) vào `CategoryRepository` và `BrandRepository` (giống `findByCategoryIdAndStatuses` hiện có):
    ```java
    // Trong CategoryRepository
    @Query("""
        SELECT new com.ecommerce.backend.modules.category.dto.response.CategoryResponse(
            c.id, c.name, c.status, c.imageUrl, COUNT(p.id), c.createdAt, c.updatedAt)
        FROM Category c
        LEFT JOIN Product p ON p.category.id = c.id
        GROUP BY c.id, c.name, c.status, c.imageUrl, c.createdAt, c.updatedAt
    """)
    List<CategoryResponse> findAllWithProductCount();
    ```
    (Làm tương tự cho `searchCategories(String keyword)` và `BrandRepository.findAllWithProductCount()`).
  * Thêm `boolean existsByBrandId(Long brandId);` vào `ProductRepository` và dùng trong `BrandServiceImpl.deleteBrand(Long id)` thay cho `brand.getProducts().isEmpty()`.
  * Bỏ phụ thuộc `ProductRepository` khỏi `CategoryMapper` (để Mapper là pure mapper) và gắn `@Transactional(readOnly = true)` cho các hàm đọc trong `CategoryServiceImpl` & `BrandServiceImpl`.

---

### 3.2. Giảm truy vấn lặp trong `CheckoutServiceImpl` và `OrderServiceImpl.buyAgain`
* **File liên quan**:
  * `src/main/java/com/ecommerce/backend/modules/order/service/CheckoutServiceImpl.java`
  * `src/main/java/com/ecommerce/backend/modules/order/service/OrderServiceImpl.java`
  * `src/main/java/com/ecommerce/backend/modules/product/repository/ProductVariantRepository.java`
* **Vấn đề hiện tại**:
  * Trong `CheckoutServiceImpl.buildOrderItemsFromCart()`: `CartRepository.findByUserId()` đã dùng `@EntityGraph` fetch sẵn `items.productVariant.inventory`, nhưng dòng 299 vẫn gọi `inventoryRepository.findByProductVariantId(...)` trong vòng lặp, rồi `deductInventory()` lại tiếp tục gọi `findByProductVariantIdForUpdate(...)` lần nữa.
  * Trong `CheckoutServiceImpl.buildOrderItemsFromDirectItems()` và `OrderServiceImpl.buyAgain()`: gọi `findById` + `findByProductVariantId` lặp lại bên trong vòng `for`.
* **Phương án sửa**:
  * Trong `buildOrderItemsFromCart`: đọc tồn kho trực tiếp từ `productVariant.getInventory()` (không query thêm); việc khóa và trừ kho thực hiện 1 lần duy nhất tại `deductInventory`.
  * Trong `buildOrderItemsFromDirectItems` & `buyAgain`: thêm hàm batch fetch vào `ProductVariantRepository`:
    ```java
    @EntityGraph(attributePaths = {"product", "inventory"})
    List<ProductVariant> findAllByIdIn(Collection<Long> ids);
    ```
    Sau đó chuyển thành `Map<Long, ProductVariant>` để tra cứu O(1) thay cho N truy vấn trong vòng lặp.

---

### 3.3. Gửi Email bất đồng bộ (`@Async`) sau khi Transaction Commit
* **File liên quan**:
  * `src/main/java/com/ecommerce/backend/EcommerceBackendApplication.java`
  * `src/main/java/com/ecommerce/backend/common/service/MailService.java`
  * `src/main/java/com/ecommerce/backend/modules/auth/service/AuthServiceImpl.java`
  * `src/main/java/com/ecommerce/backend/modules/order/service/OrderAdminServiceImpl.java`
* **Vấn đề hiện tại**:
  * Gửi mail qua `smtp.gmail.com:587` mất 1.5–5 giây, đang chạy đồng bộ ngay bên trong `@Transactional` (`register`, `updateOrderStatus`, `cancelOrder`), gây giữ DB Connection Pool và Row Lock quá lâu.
  * Nếu gửi mail khi transaction chưa commit mà transaction sau đó bị rollback, khách hàng vẫn nhận nhầm email thông báo.
* **Phương án sửa**:
  * Thêm `@EnableAsync` vào `EcommerceBackendApplication` (hoặc lớp `@Configuration` riêng kèm `ThreadPoolTaskExecutor`).
  * Đánh dấu `@Async` cho các method trong `MailService` kèm `try-catch` log lỗi (tránh nuốt exception im lặng ở background thread).
  * Ở các method `@Transactional` (`register`, `updateOrderStatus`, `cancelOrder`), bọc lời gọi `mailService.send...` trong `TransactionSynchronizationManager.registerSynchronization(... afterCommit() ...)`:
    ```java
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
            mailService.sendOrderCancellationEmail(email, orderId, reason);
        }
    });
    ```

---

### 3.4. Bổ sung Database Index & Unique Constraint trên các Entity
* **File liên quan**:
  * `src/main/java/com/ecommerce/backend/modules/cart/entity/Cart.java`
  * `src/main/java/com/ecommerce/backend/modules/wishlist/entity/Wishlist.java`
  * `src/main/java/com/ecommerce/backend/modules/coupon/entity/Coupon.java`
  * `src/main/java/com/ecommerce/backend/modules/product/entity/ProductVariant.java`
  * `src/main/java/com/ecommerce/backend/modules/order/entity/Order.java`
  * `src/main/java/com/ecommerce/backend/modules/product/entity/Product.java`
  * `src/main/java/com/ecommerce/backend/modules/review/entity/Review.java`
* **Vấn đề hiện tại**:
  * Thiếu `unique = true` trên `Cart.user`, `(user_id, product_id)` của `Wishlist`, `Coupon.code`, `ProductVariant.sku` -> có thể phát sinh bản ghi trùng khi request song song (ví dụ 2 `Cart` cho cùng 1 user làm `findByUserId` ném lỗi 500 `IncorrectResultSizeDataAccessException`).
  * Thiếu `@Index` trên các cột thường xuyên lọc/thống kê (`orders`, `products`, `reviews`).
* **Phương án sửa**:
  * `Cart.java`: `@JoinColumn(name = "user_id", nullable = false, unique = true)` (và xử lý bắt `DataIntegrityViolationException` trong `getOrCreateCart` nếu 2 request tạo giỏ hàng cùng lúc).
  * `Wishlist.java`: `@Table(name = "wishlists", uniqueConstraints = @UniqueConstraint(name = "uk_wishlist_user_product", columnNames = {"user_id", "product_id"}))`.
  * `Coupon.java`: `@Column(nullable = false, unique = true)` cho `code`.
  * `ProductVariant.java`: `@Column(nullable = false, unique = true)` cho `sku`.
  * `Order.java`:
    ```java
    @Table(name = "orders", indexes = {
        @Index(name = "idx_orders_user_status_created", columnList = "user_id, order_status, created_at"),
        @Index(name = "idx_orders_created_status_payment", columnList = "created_at, order_status, payment_status")
    })
    ```
  * `Product.java`:
    ```java
    @Table(name = "products", indexes = {
        @Index(name = "idx_products_status_cat_brand_price", columnList = "status, category_id, brand_id, base_price"),
        @Index(name = "idx_products_name", columnList = "name")
    })
    ```
  * `Review.java`:
    ```java
    @Table(name = "reviews", indexes = {
        @Index(name = "idx_reviews_product_deleted", columnList = "product_id, is_deleted"),
        @Index(name = "idx_reviews_order_item_deleted", columnList = "order_item_id, is_deleted")
    })
    ```

---

### 3.5. Đẩy lọc Inventory xuống SQL, Phân trang Admin Lists & Bật Redis Caching
* **File liên quan**:
  * `src/main/java/com/ecommerce/backend/modules/inventory/repository/InventoryRepository.java`
  * `src/main/java/com/ecommerce/backend/modules/inventory/service/InventoryServiceImpl.java`
  * `src/main/java/com/ecommerce/backend/modules/coupon/service/CouponServiceImpl.java`
  * `src/main/java/com/ecommerce/backend/modules/category/service/CategoryServiceImpl.java`
  * `src/main/java/com/ecommerce/backend/modules/brand/service/BrandServiceImpl.java`
  * `src/main/java/com/ecommerce/backend/modules/policy/service/PolicyServiceImpl.java`
  * `src/main/resources/application.properties`
* **Vấn đề hiện tại**:
  1. `InventoryServiceImpl.getInventoryList()` tải toàn bộ tồn kho lên RAM rồi mới `.filter(...)` theo `status` (`OUT_OF_STOCK`, `LOW_STOCK`, `IN_STOCK`).
  2. `InventoryServiceImpl.getInventoryHistory()` và `CouponServiceImpl.getAllCoupons()` trả toàn bộ bảng không phân trang.
  3. Redis đã cấu hình nhưng chưa bật `@EnableCaching` cho dữ liệu ít thay đổi (`categories`, `brands`, `policies`).
  4. `spring.jpa.show-sql=true` bật cứng trong `application.properties` gây chậm I/O console.
* **Phương án sửa**:
  * Đẩy điều kiện `status` (`OUT_OF_STOCK` -> `quantity <= 0`, `LOW_STOCK` -> `quantity > 0 AND quantity <= 10`, `IN_STOCK` -> `quantity > 10`) vào JPQL `InventoryRepository.findByCriteria(...)` kèm `Pageable`.
  * Chuyển `getInventoryHistory` và `getAllCoupons` sang nhận `Pageable` và trả về `Page<...>`.
  * Tạo `CacheConfig` với `@EnableCaching` và `RedisCacheManager` (TTL mặc định 10–30 phút), gắn `@Cacheable("categories")` / `@Cacheable("brands")` / `@Cacheable("policies")` cho hàm đọc và `@CacheEvict(allEntries = true)` cho các hàm tạo/sửa/xóa.
  * Đổi `spring.jpa.show-sql=${JPA_SHOW_SQL:false}` và `spring.jpa.properties.hibernate.format_sql=${JPA_FORMAT_SQL:false}` trong `application.properties`.
