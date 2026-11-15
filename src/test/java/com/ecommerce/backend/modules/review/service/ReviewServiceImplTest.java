package com.ecommerce.backend.modules.review.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.common.service.LocalFileStorageService;
import com.ecommerce.backend.modules.auth.entity.User;
import com.ecommerce.backend.modules.order.entity.Order;
import com.ecommerce.backend.modules.order.entity.OrderItem;
import com.ecommerce.backend.modules.order.enums.OrderStatus;
import com.ecommerce.backend.modules.order.repository.OrderItemRepository;
import com.ecommerce.backend.modules.product.entity.Product;
import com.ecommerce.backend.modules.product.entity.ProductVariant;
import com.ecommerce.backend.modules.product.repository.ProductRepository;
import com.ecommerce.backend.modules.review.dto.request.ReviewRequest;
import com.ecommerce.backend.modules.review.dto.request.UpdateReviewRequest;
import com.ecommerce.backend.modules.review.entity.Review;
import com.ecommerce.backend.modules.review.repository.ReviewRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReviewServiceImplTest {

    @TempDir
    Path uploadDir;

    private ReviewRepository reviews;
    private ReviewServiceImpl service;
    private ReviewRequest request;

    @BeforeEach
    void setUp() {
        reviews = mock(ReviewRepository.class);
        var products = mock(ProductRepository.class);
        var orderItems = mock(OrderItemRepository.class);
        var storage = new LocalFileStorageService();
        ReflectionTestUtils.setField(storage, "uploadDir", uploadDir.toString());
        ReflectionTestUtils.setField(storage, "baseUrl", "http://localhost:8081");

        var user = new User();
        user.setId(1L);
        var order = new Order();
        order.setUser(user);
        order.setOrderStatus(OrderStatus.DELIVERED);
        var product = new Product();
        product.setId(2L);
        var variant = new ProductVariant();
        variant.setId(3L);
        variant.setProduct(product);
        var item = new OrderItem();
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
    void updatesContentAndKeepsExistingImageWhenRemoveImageIsOmitted() throws Exception {
        var created = existingReviewWithImage();
        var update = new UpdateReviewRequest();
        update.setRating(4.0);
        update.setReviewText("Updated review");

        var result = service.updateReview(1L, created.getId(), update);

        assertEquals(4.0, result.getRating());
        assertEquals("Updated review", result.getReviewText());
        assertEquals(created.getImageUrl(), result.getImageUrl());
        assertTrue(Files.isRegularFile(storedImage(result.getImageUrl())));
    }

    @Test
    void removesExistingImageOnlyAfterCommit() throws Exception {
        var created = existingReviewWithImage();
        var stored = storedImage(created.getImageUrl());
        var update = new UpdateReviewRequest();
        update.setRating(4.0);
        update.setRemoveImage(true);

        var result = service.updateReview(1L, created.getId(), update);

        assertNull(result.getImageUrl());
        assertTrue(Files.isRegularFile(stored));
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
        assertFalse(Files.exists(stored));
    }

    private Review existingReviewWithImage() {
        TransactionSynchronizationManager.initSynchronization();
        when(reviews.save(any(Review.class))).thenAnswer(call -> {
            Review review = call.getArgument(0);
            review.setId(5L);
            return review;
        });
        service.createReview(1L, request, png());
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
}
