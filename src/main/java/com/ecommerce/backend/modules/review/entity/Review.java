package com.ecommerce.backend.modules.review.entity;

import com.ecommerce.backend.common.utils.Tsid;
import com.ecommerce.backend.modules.auth.entity.User;
import com.ecommerce.backend.modules.order.entity.OrderItem;
import com.ecommerce.backend.modules.product.entity.Product;
import com.ecommerce.backend.modules.product.entity.ProductVariant;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "reviews", indexes = {
        @Index(name = "idx_review_product", columnList = "product_id"), // Đánh index để query trên Product
        @Index(name = "idx_review_user", columnList = "user_id") // Đánh index để query trên My Review
})
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Review {

    @Id
    @Tsid
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @Column(nullable = false)
    private Double rating;

    @Column(columnDefinition = "TEXT")
    private String reviewText;

    private String imageUrl;

    @Builder.Default
    private boolean isDeleted = false;

    @Builder.Default
    private boolean isHidden = false;

    @Column(columnDefinition = "TEXT")
    private String adminReply;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Xác minh review cho sản phẩm cụ thể nào
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;
}
