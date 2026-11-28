package com.ecommerce.backend.modules.review.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.common.exception.DuplicateResourceException;
import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.common.service.LocalFileStorageService;
import com.ecommerce.backend.modules.auth.entity.User;
import com.ecommerce.backend.modules.order.entity.Order;
import com.ecommerce.backend.modules.order.entity.OrderItem;
import com.ecommerce.backend.modules.order.enums.OrderStatus;
import com.ecommerce.backend.modules.order.repository.OrderItemRepository;
import com.ecommerce.backend.modules.product.entity.Product;
import com.ecommerce.backend.modules.product.entity.ProductImage;
import com.ecommerce.backend.modules.product.entity.ProductVariant;
import com.ecommerce.backend.modules.product.repository.ProductRepository;
import com.ecommerce.backend.modules.review.dto.request.ReviewReplyRequest;
import com.ecommerce.backend.modules.review.dto.request.ReviewRequest;
import com.ecommerce.backend.modules.review.dto.request.UpdateReviewRequest;
import com.ecommerce.backend.modules.review.dto.response.AdminReviewResponse;
import com.ecommerce.backend.modules.review.dto.response.PendingReviewResponse;
import com.ecommerce.backend.modules.review.dto.response.ReviewResponse;
import com.ecommerce.backend.modules.review.entity.Review;
import com.ecommerce.backend.modules.review.repository.ReviewRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReviewServiceImplTest {

    @TempDir
    Path uploadDir;

    private ReviewRepository reviews;
    private ProductRepository products;
    private OrderItemRepository orderItems;
    private ReviewServiceImpl service;
    private ReviewRequest request;
    private User user;
    private Order order;
    private Product product;
    private ProductVariant variant;
    private OrderItem item;

    @BeforeEach
    void setUp() {
        reviews = mock(ReviewRepository.class);
        products = mock(ProductRepository.class);
        orderItems = mock(OrderItemRepository.class);
        var storage = new LocalFileStorageService();
        ReflectionTestUtils.setField(storage, "uploadDir", uploadDir.toString());
        ReflectionTestUtils.setField(storage, "baseUrl", "http://localhost:8081");

        user = new User();
        user.setId(1L);
        user.setFullName("Nguyen Van A");
        order = new Order();
        order.setId(10L);
        order.setUser(user);
        order.setOrderStatus(OrderStatus.DELIVERED);
        order.setCreatedAt(Instant.now());
        product = new Product();
        product.setId(2L);
        product.setName("Laptop Pro");
        variant = new ProductVariant();
        variant.setId(3L);
        variant.setProduct(product);
        variant.setAttributes(Map.of("color", "Silver"));
        item = new OrderItem();
        item.setId(4L);
        item.setOrder(order);
        item.setProductVariant(variant);

        when(orderItems.findByIdForUpdate(4L)).thenReturn(Optional.of(item));
        when(reviews.save(any(Review.class))).thenAnswer(call -> call.getArgument(0));
        service = new ReviewServiceImpl(reviews, products, storage, orderItems);

        request = new ReviewRequest();
        request.setOrderItemId(4L);
        request.setProductId(2L);
        request.setRating(5.0);
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void createsReviewWithoutImageAndDerivesVariantFromOrderItem() {
        var result = service.createReview(1L, request, null);

        assertEquals(2L, result.getProductId());
        assertNull(result.getImageUrl());
        verify(reviews).save(argThat(review -> review.getProductVariant().getId().equals(3L)));
    }

    @Test
    void rejectsWrongOwnerAndInvalidImageBeforeCreatingFile() {
        assertThrows(AccessDeniedException.class, () -> service.createReview(9L, request, png()));
        var wrongContents = new MockMultipartFile(
                "image", "image.png", "image/png", "invalid".getBytes(StandardCharsets.UTF_8));
        assertThrows(BadRequestException.class, () -> service.createReview(1L, request, wrongContents));
        assertFalse(Files.exists(uploadDir.resolve("reviews")));
        verify(reviews, never()).save(any(Review.class));
    }

    @Test
    void removesNewImageWhenDatabaseTransactionRollsBack() throws Exception {
        TransactionSynchronizationManager.initSynchronization();
        var result = service.createReview(1L, request, png());
        var stored = uploadDir.resolve(result.getImageUrl().split("/uploads/", 2)[1]);

        assertTrue(Files.isRegularFile(stored));
        TransactionSynchronizationManager.getSynchronizations().forEach(callback ->
                callback.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        assertFalse(Files.exists(stored));
    }

    @Test
    void updatesContentAndKeepsExistingImageWhenRemoveImageIsOmitted() {
        var created = existingReviewWithImage();
        var originalImageUrl = created.getImageUrl();
        var update = new UpdateReviewRequest();
        update.setRating(4.0);
        update.setReviewText("Updated review");

        var result = service.updateReview(1L, created.getId(), update, null);

        assertEquals(4.0, result.getRating());
        assertEquals("Updated review", result.getReviewText());
        assertEquals(originalImageUrl, result.getImageUrl());
        assertTrue(Files.isRegularFile(storedImage(result.getImageUrl())));
    }

    @Test
    void removesExistingImageOnlyAfterCommit() throws Exception {
        var created = existingReviewWithImage();
        var stored = storedImage(created.getImageUrl());
        var update = new UpdateReviewRequest();
        update.setRating(4.0);
        update.setRemoveImage(true);

        var result = service.updateReview(1L, created.getId(), update, null);

        assertNull(result.getImageUrl());
        assertTrue(Files.isRegularFile(stored));
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
        assertFalse(Files.exists(stored));
    }

    @Test
    void replacesImageAndDeletesOldFileAfterCommit() {
        var created = existingReviewWithImage();
        var oldFile = storedImage(created.getImageUrl());
        var update = new UpdateReviewRequest();
        update.setRating(4.0);

        var result = service.updateReview(1L, created.getId(), update, png());

        var newFile = storedImage(result.getImageUrl());
        assertNotEquals(oldFile, newFile);
        assertTrue(Files.isRegularFile(newFile));
        assertTrue(Files.isRegularFile(oldFile));

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
        assertFalse(Files.exists(oldFile));
        assertTrue(Files.isRegularFile(newFile));
    }

    @Test
    void keepsOldFileAndRemovesReplacementOnRollback() {
        var created = existingReviewWithImage();
        var oldFile = storedImage(created.getImageUrl());
        var update = new UpdateReviewRequest();
        update.setRating(4.0);

        var result = service.updateReview(1L, created.getId(), update, png());

        var newFile = storedImage(result.getImageUrl());
        assertTrue(Files.isRegularFile(newFile));
        TransactionSynchronizationManager.getSynchronizations().forEach(callback ->
                callback.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        assertTrue(Files.isRegularFile(oldFile));
        assertFalse(Files.exists(newFile));
    }

    @Test
    void rejectsReplacementWithRemoveImageBeforeUploading() {
        var created = existingReviewWithImage();
        var originalImageUrl = created.getImageUrl();
        var update = new UpdateReviewRequest();
        update.setRating(4.0);
        update.setRemoveImage(true);

        assertThrows(BadRequestException.class,
                () -> service.updateReview(1L, created.getId(), update, png()));
        assertEquals(originalImageUrl, created.getImageUrl());
        assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty());
    }

    @Test
    void rejectsWrongOwnerAndInvalidReplacementBeforeUploading() {
        var created = existingReviewWithImage();
        var originalImageUrl = created.getImageUrl();
        var update = new UpdateReviewRequest();
        update.setRating(4.0);
        var invalidFile = new MockMultipartFile(
                "image", "image.png", "image/png", "invalid".getBytes(StandardCharsets.UTF_8));

        assertThrows(AccessDeniedException.class,
                () -> service.updateReview(9L, created.getId(), update, png()));
        assertThrows(BadRequestException.class,
                () -> service.updateReview(1L, created.getId(), update, invalidFile));
        assertEquals(originalImageUrl, created.getImageUrl());
        assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty());
    }

    private Review existingReviewWithImage() {
        TransactionSynchronizationManager.initSynchronization();
        when(reviews.save(any(Review.class))).thenAnswer(call -> {
            Review review = call.getArgument(0);
            review.setId(5L);
            return review;
        });
        service.createReview(1L, request, png());
        // Use a new synchronization context for the update to model a separate transaction.
        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.initSynchronization();
        var saved = ArgumentCaptor.forClass(Review.class);
        verify(reviews).save(saved.capture());
        var review = saved.getValue();
        when(reviews.findActiveByIdForUpdate(review.getId())).thenReturn(Optional.of(review));
        return review;
    }

    private Path storedImage(String imageUrl) {
        return uploadDir.resolve(imageUrl.split("/uploads/", 2)[1]);
    }

    private MockMultipartFile png() {
        byte[] bytes = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a9ZoAAAAASUVORK5CYII=");
        return new MockMultipartFile("image", "image.png", "image/png", bytes);
    }

    @Nested
    @DisplayName("Get Reviewed History & Pending Reviews Tests")
    class GetReviewedAndPendingTests {

        @Test
        @DisplayName("Should map reviewed history including primary image and deleted product fallback")
        void getReviewedHistory_MapsActiveAndDeletedProducts() {
            ProductImage primary = ProductImage.builder().imageUrl("/img/main.jpg").isPrimary(true).build();
            ProductImage secondary = ProductImage.builder().imageUrl("/img/sub.jpg").isPrimary(false).build();
            product.setProductImages(List.of(secondary, primary));

            Review activeProductReview = Review.builder()
                    .id(11L)
                    .user(user)
                    .product(product)
                    .productVariant(variant)
                    .rating(5.0)
                    .reviewText("Rat tot")
                    .createdAt(Instant.now())
                    .build();
            Review deletedProductReview = Review.builder()
                    .id(12L)
                    .user(user)
                    .product(null)
                    .productVariant(null)
                    .rating(4.0)
                    .reviewText("On")
                    .createdAt(Instant.now())
                    .build();

            when(reviews.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of(activeProductReview, deletedProductReview));

            List<ReviewResponse> result = service.getReviewedHistory(1L);

            assertEquals(2, result.size());
            assertFalse(result.get(0).isProductDeleted());
            assertEquals("/img/main.jpg", result.get(0).getProductImage());
            assertEquals(Map.of("color", "Silver"), result.get(0).getVariantAttributes());

            assertTrue(result.get(1).isProductDeleted());
            assertEquals("Product no longer available", result.get(1).getProductName());
            assertNull(result.get(1).getProductId());
        }

        @Test
        @DisplayName("Should return pending review items for delivered orders")
        void getPendingReviews_ReturnsMappedItems() {
            when(orderItems.findPendingReviewItems(1L, OrderStatus.DELIVERED))
                    .thenReturn(List.of(item));

            List<PendingReviewResponse> pending = service.getPendingReviews(1L);

            assertEquals(1, pending.size());
            assertEquals(4L, pending.get(0).getOrderItemId());
            assertEquals(10L, pending.get(0).getOrderId());
            assertEquals(2L, pending.get(0).getProductId());
            assertEquals(3L, pending.get(0).getProductVariantId());
            assertEquals("Laptop Pro", pending.get(0).getProductName());
        }
    }

    @Nested
    @DisplayName("Create Review Validation & Rating Recalculation Tests")
    class CreateReviewValidationAndRatingTests {

        @ParameterizedTest
        @ValueSource(strings = {"Sản phẩm chửi_thề tệ", "Từ bị cấm", "Xem tại http://spam.vn", "HTTPS://evil.com"})
        @DisplayName("Should reject blacklisted words or links before querying DB")
        void createReview_BlacklistedContent_ThrowsBadRequest(String text) {
            request.setReviewText(text);
            assertThrows(BadRequestException.class, () -> service.createReview(1L, request, null));
            verify(reviews, never()).save(any());
        }

        @Test
        @DisplayName("Should reject non-blank imageUrl in JSON payload")
        void createReview_JsonImageUrlProvided_ThrowsBadRequest() {
            request.setImageUrl("https://cdn.example.com/pic.jpg");
            assertThrows(BadRequestException.class, () -> service.createReview(1L, request, null));
        }

        @Test
        @DisplayName("Should reject missing orderItem, undelivered order, product/variant mismatch, and duplicate review")
        void createReview_ValidationFailures_ThrowExpectedExceptions() {
            when(orderItems.findByIdForUpdate(999L)).thenReturn(Optional.empty());
            request.setOrderItemId(999L);
            assertThrows(ResourceNotFoundException.class, () -> service.createReview(1L, request, null));

            request.setOrderItemId(4L);
            order.setOrderStatus(OrderStatus.SHIPPED);
            assertThrows(BadRequestException.class, () -> service.createReview(1L, request, null));

            order.setOrderStatus(OrderStatus.DELIVERED);
            request.setProductId(99L);
            assertThrows(BadRequestException.class, () -> service.createReview(1L, request, null));

            request.setProductId(2L);
            request.setProductVariantId(88L);
            assertThrows(BadRequestException.class, () -> service.createReview(1L, request, null));

            request.setProductVariantId(3L);
            when(reviews.existsByUserIdAndOrderItemIdAndIsDeletedFalse(1L, 4L)).thenReturn(true);
            assertThrows(DuplicateResourceException.class, () -> service.createReview(1L, request, null));
        }

        @Test
        @DisplayName("Should recalculate product averageRating after creating review")
        void createReview_RecalculatesProductAverageRating() {
            when(reviews.getAverageRatingByProductId(2L)).thenReturn(4.5);
            when(products.findById(2L)).thenReturn(Optional.of(product));

            service.createReview(1L, request, null);

            assertEquals(4.5, product.getAverageRating());
            verify(products).save(product);
        }
    }

    @Nested
    @DisplayName("Update & Delete Review Tests")
    class UpdateAndDeleteReviewTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when updating non-existent review or BadRequest on blacklist")
        void updateReview_NotFoundOrBlacklisted_ThrowsExpectedException() {
            UpdateReviewRequest update = new UpdateReviewRequest();
            update.setRating(4.0);
            when(reviews.findActiveByIdForUpdate(999L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> service.updateReview(1L, 999L, update, null));

            Review existing = Review.builder().id(5L).user(user).product(product).build();
            when(reviews.findActiveByIdForUpdate(5L)).thenReturn(Optional.of(existing));
            update.setReviewText("http://phishing.test");
            assertThrows(BadRequestException.class, () -> service.updateReview(1L, 5L, update, null));
        }

        @Test
        @DisplayName("Should not delete old image if another review references the same imageUrl")
        void updateReview_SharedImageUrl_DoesNotRegisterDeleteSynchronization() {
            var created = existingReviewWithImage();
            when(reviews.existsByImageUrlAndIdNot(created.getImageUrl(), created.getId())).thenReturn(true);

            UpdateReviewRequest update = new UpdateReviewRequest();
            update.setRating(4.0);
            update.setRemoveImage(true);

            service.updateReview(1L, created.getId(), update, null);

            assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty());
        }

        @Test
        @DisplayName("Should soft-delete review and reset product averageRating to 0.0 when no reviews remain")
        void deleteReview_SuccessAndFailures() {
            Review existing = Review.builder().id(5L).user(user).product(product).isDeleted(false).build();
            when(reviews.findActiveByIdForUpdate(5L)).thenReturn(Optional.of(existing));
            when(reviews.getAverageRatingByProductId(2L)).thenReturn(null);
            when(products.findById(2L)).thenReturn(Optional.of(product));

            service.deleteReview(1L, 5L);

            assertTrue(existing.isDeleted());
            assertEquals(0.0, product.getAverageRating());
            verify(reviews).save(existing);
            verify(products).save(product);

            assertThrows(AccessDeniedException.class, () -> service.deleteReview(99L, 5L));
            when(reviews.findActiveByIdForUpdate(999L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> service.deleteReview(1L, 999L));
        }
    }

    @Nested
    @DisplayName("Admin Review Moderation Tests")
    class AdminReviewTests {

        @Test
        @DisplayName("Should return paged AdminReviewResponse for admin search")
        void getReviewsForAdmin_ReturnsMappedPage() {
            Review review = Review.builder()
                    .id(15L)
                    .product(product)
                    .user(user)
                    .rating(5.0)
                    .reviewText("Tuyet voi")
                    .createdAt(Instant.now())
                    .isHidden(false)
                    .adminReply("Cam on ban")
                    .build();
            Pageable pageable = PageRequest.of(0, 10);
            when(reviews.findAllForAdmin(5.0, "Laptop", pageable))
                    .thenReturn(new PageImpl<>(List.of(review), pageable, 1));

            Page<AdminReviewResponse> page = service.getReviewsForAdmin(5.0, "Laptop", pageable);

            assertEquals(1, page.getTotalElements());
            AdminReviewResponse dto = page.getContent().get(0);
            assertEquals("Laptop Pro", dto.getProductName());
            assertEquals("Nguyen Van A", dto.getCustomerName());
            assertEquals("Cam on ban", dto.getAdminReply());
            assertFalse(dto.isHidden());
        }

        @Test
        @DisplayName("Should toggle review visibility and reject missing or deleted reviews")
        void toggleReviewVisibility_SuccessAndFailures() {
            Review active = Review.builder().id(20L).isHidden(false).isDeleted(false).build();
            when(reviews.findById(20L)).thenReturn(Optional.of(active));

            service.toggleReviewVisibility(20L);
            assertTrue(active.isHidden());
            service.toggleReviewVisibility(20L);
            assertFalse(active.isHidden());
            verify(reviews, times(2)).save(active);

            Review deleted = Review.builder().id(21L).isDeleted(true).build();
            when(reviews.findById(21L)).thenReturn(Optional.of(deleted));
            assertThrows(ResourceNotFoundException.class, () -> service.toggleReviewVisibility(21L));

            when(reviews.findById(999L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> service.toggleReviewVisibility(999L));
        }

        @Test
        @DisplayName("Should save admin reply and reject missing or deleted reviews")
        void replyToReview_SuccessAndFailures() {
            Review active = Review.builder().id(30L).isDeleted(false).build();
            when(reviews.findById(30L)).thenReturn(Optional.of(active));

            ReviewReplyRequest reply = new ReviewReplyRequest();
            reply.setReplyText("Shop da tiep nhan y kien cua ban.");
            service.replyToReview(30L, reply);

            assertEquals("Shop da tiep nhan y kien cua ban.", active.getAdminReply());
            verify(reviews).save(active);

            Review deleted = Review.builder().id(31L).isDeleted(true).build();
            when(reviews.findById(31L)).thenReturn(Optional.of(deleted));
            assertThrows(ResourceNotFoundException.class, () -> service.replyToReview(31L, reply));

            when(reviews.findById(999L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> service.replyToReview(999L, reply));
        }
    }
}
