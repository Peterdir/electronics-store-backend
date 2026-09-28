package com.ecommerce.backend.modules.wishlist.service;

import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.modules.auth.dto.response.MessageResponse;
import com.ecommerce.backend.modules.auth.entity.User;
import com.ecommerce.backend.modules.auth.repository.UserRepository;
import com.ecommerce.backend.modules.inventory.entity.Inventory;
import com.ecommerce.backend.modules.product.entity.Product;
import com.ecommerce.backend.modules.product.entity.ProductImage;
import com.ecommerce.backend.modules.product.entity.ProductVariant;
import com.ecommerce.backend.modules.product.enums.ProductStatus;
import com.ecommerce.backend.modules.product.repository.ProductRepository;
import com.ecommerce.backend.modules.wishlist.dto.response.WishlistItemResponse;
import com.ecommerce.backend.modules.wishlist.entity.Wishlist;
import com.ecommerce.backend.modules.wishlist.repository.WishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishlistServiceImplTest {

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private WishlistServiceImpl wishlistService;

    private User testUser;
    private Product testProduct;
    private Wishlist testWishlist;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        testProduct = Product.builder()
                .id(100L)
                .name("Test Product")
                .basePrice(new BigDecimal("99.99"))
                .status(ProductStatus.ACTIVE)
                .build();

        testWishlist = Wishlist.builder()
                .id(10L)
                .user(testUser)
                .product(testProduct)
                .createdAt(Instant.now())
                .build();
    }

    // ==========================================
    // GET WISHLIST TESTS
    // ==========================================

    @Test
    @DisplayName("Should return wishlist successfully with correct mapping (primary image and in-stock)")
    void getWishlist_Success_WithPrimaryImageAndStock() {
        // Arrange
        ProductImage primaryImage = ProductImage.builder().imageUrl("primary.png").isPrimary(true).build();
        ProductImage secondaryImage = ProductImage.builder().imageUrl("secondary.png").isPrimary(false).build();
        testProduct.setProductImages(List.of(secondaryImage, primaryImage));

        Inventory inventory = Inventory.builder().quantity(10L).build();
        ProductVariant variant = ProductVariant.builder().inventory(inventory).build();
        testProduct.setVariants(List.of(variant));

        when(wishlistRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(testWishlist));

        // Act
        List<WishlistItemResponse> responses = wishlistService.getWishlist(1L);

        // Assert
        assertThat(responses).hasSize(1);
        WishlistItemResponse response = responses.get(0);
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getProductId()).isEqualTo(100L);
        assertThat(response.getProductName()).isEqualTo("Test Product");
        assertThat(response.getPrice()).isEqualTo(new BigDecimal("99.99"));
        assertThat(response.getPrimaryImageUrl()).isEqualTo("primary.png");
        assertThat(response.isInStock()).isTrue();
        assertThat(response.getProductStatus()).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should return wishlist with fallback image (first image) if no primary image exists")
    void getWishlist_Success_FallbackImage() {
        // Arrange
        ProductImage image1 = ProductImage.builder().imageUrl("img1.png").isPrimary(false).build();
        ProductImage image2 = ProductImage.builder().imageUrl("img2.png").isPrimary(false).build();
        testProduct.setProductImages(List.of(image1, image2));
        testProduct.setVariants(Collections.emptyList()); // No stock

        when(wishlistRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(testWishlist));

        // Act
        List<WishlistItemResponse> responses = wishlistService.getWishlist(1L);

        // Assert
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getPrimaryImageUrl()).isEqualTo("img1.png");
        assertThat(responses.get(0).isInStock()).isFalse();
    }

    @Test
    @DisplayName("Should return wishlist with out of stock if quantity is zero")
    void getWishlist_Success_OutOfStock() {
        // Arrange
        Inventory inventory = Inventory.builder().quantity(0L).build();
        ProductVariant variant = ProductVariant.builder().inventory(inventory).build();
        testProduct.setVariants(List.of(variant));

        when(wishlistRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(testWishlist));

        // Act
        List<WishlistItemResponse> responses = wishlistService.getWishlist(1L);

        // Assert
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).isInStock()).isFalse();
    }

    // ==========================================
    // ADD PRODUCT TO WISHLIST TESTS
    // ==========================================

    @Test
    @DisplayName("Should add product to wishlist successfully")
    void addProductToWishlist_Success() {
        // Arrange
        when(wishlistRepository.existsByUserIdAndProductId(1L, 100L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));

        // Act
        MessageResponse response = wishlistService.addProductToWishlist(1L, 100L);

        // Assert
        assertThat(response.getMessage()).isEqualTo("Product added to your wishlist.");
        verify(wishlistRepository, times(1)).save(any(Wishlist.class));
    }

    @Test
    @DisplayName("Should return early message if product is already in wishlist")
    void addProductToWishlist_AlreadyExists() {
        // Arrange
        when(wishlistRepository.existsByUserIdAndProductId(1L, 100L)).thenReturn(true);

        // Act
        MessageResponse response = wishlistService.addProductToWishlist(1L, 100L);

        // Assert
        assertThat(response.getMessage()).isEqualTo("Product is already in your wishlist.");
        verify(userRepository, never()).findById(any());
        verify(wishlistRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user is not found")
    void addProductToWishlist_UserNotFound() {
        // Arrange
        when(wishlistRepository.existsByUserIdAndProductId(1L, 100L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, 
            () -> wishlistService.addProductToWishlist(1L, 100L));
            
        assertThat(exception.getMessage()).isEqualTo("User not found.");
        verify(wishlistRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when product is not found")
    void addProductToWishlist_ProductNotFound() {
        // Arrange
        when(wishlistRepository.existsByUserIdAndProductId(1L, 100L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(100L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, 
            () -> wishlistService.addProductToWishlist(1L, 100L));
            
        assertThat(exception.getMessage()).isEqualTo("Product not found.");
        verify(wishlistRepository, never()).save(any());
    }

    // ==========================================
    // REMOVE PRODUCT FROM WISHLIST TESTS
    // ==========================================

    @Test
    @DisplayName("Should remove product from wishlist successfully")
    void removeProductFromWishlist_Success() {
        // Arrange
        when(wishlistRepository.existsByUserIdAndProductId(1L, 100L)).thenReturn(true);

        // Act
        MessageResponse response = wishlistService.removeProductFromWishlist(1L, 100L);

        // Assert
        assertThat(response.getMessage()).isEqualTo("Product removed from your wishlist.");
        verify(wishlistRepository, times(1)).deleteByUserIdAndProductId(1L, 100L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException if product not in wishlist")
    void removeProductFromWishlist_NotFound() {
        // Arrange
        when(wishlistRepository.existsByUserIdAndProductId(1L, 100L)).thenReturn(false);

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, 
            () -> wishlistService.removeProductFromWishlist(1L, 100L));
            
        assertThat(exception.getMessage()).isEqualTo("Product not found in wishlist.");
        verify(wishlistRepository, never()).deleteByUserIdAndProductId(anyLong(), anyLong());
    }
}
