package com.ecommerce.backend.modules.product.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.modules.inventory.entity.Inventory;
import com.ecommerce.backend.modules.product.entity.Product;
import com.ecommerce.backend.modules.product.entity.ProductVariant;
import com.ecommerce.backend.modules.product.enums.ProductStatus;
import com.ecommerce.backend.modules.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductVariantStatusServiceTest {
    @Mock private ProductRepository productRepository;
    @InjectMocks private ProductServiceImpl service;
    private static final Instant BEFORE = Instant.parse("2026-01-01T00:00:00Z");
    private Product product;
    private ProductVariant variant;
    private ProductVariant other;

    @BeforeEach
    void setUp() {
        variant = ProductVariant.builder().id(11L).sku("SKU-A")
                .price(new BigDecimal("100"))
                .inventory(Inventory.builder().quantity(7L).updatedAt(BEFORE).build())
                .status(ProductStatus.INACTIVE).build();
        other = ProductVariant.builder().id(12L).status(ProductStatus.INACTIVE).build();
        product = Product.builder().id(1L).status(ProductStatus.ACTIVE).updatedAt(BEFORE)
                .variants(new ArrayList<>(List.of(variant, other))).build();
    }

    @Test
    void restoreOnlySelectedVariantWithoutChangingStockPriceOrId() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        service.changeProductVariantStatus(1L, 11L, ProductStatus.ACTIVE);
        assertEquals(ProductStatus.ACTIVE, variant.getStatus());
        assertEquals(ProductStatus.INACTIVE, other.getStatus());
        assertEquals(11L, variant.getId());
        assertEquals("SKU-A", variant.getSku());
        assertEquals(new BigDecimal("100"), variant.getPrice());
        assertEquals(7L, variant.getInventory().getQuantity());
        assertEquals(BEFORE, variant.getInventory().getUpdatedAt());
        assertTrue(product.getUpdatedAt().isAfter(BEFORE));
        verify(productRepository).save(product);
    }

    @Test
    void parentVisibilityDoesNotOverwriteVariantStatuses() {
        variant.setStatus(ProductStatus.ACTIVE);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        service.changeProductStatus(1L, ProductStatus.HIDDEN);
        assertEquals(ProductStatus.ACTIVE, variant.getStatus());
        assertEquals(ProductStatus.INACTIVE, other.getStatus());
        service.changeProductStatus(1L, ProductStatus.ACTIVE);
        assertEquals(ProductStatus.ACTIVE, variant.getStatus());
        assertEquals(ProductStatus.INACTIVE, other.getStatus());
    }

    @Test
    void restoreVariantKeepsParentHiddenAndSupportsLegacyHiddenVariant() {
        product.setStatus(ProductStatus.HIDDEN);
        variant.setStatus(ProductStatus.HIDDEN);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        service.changeProductVariantStatus(1L, 11L, ProductStatus.ACTIVE);
        assertEquals(ProductStatus.HIDDEN, product.getStatus());
        assertEquals(ProductStatus.ACTIVE, variant.getStatus());
    }

    @Test
    void identicalStatusDoesNotTouchTimestampOrSave() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        service.changeProductVariantStatus(1L, 11L, ProductStatus.INACTIVE);
        assertEquals(BEFORE, product.getUpdatedAt());
        verify(productRepository, never()).save(any());
    }

    @Test
    void deleteEndpointStillDeactivatesOnlyItsVariant() {
        variant.setStatus(ProductStatus.ACTIVE);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        service.deleteProductVariant(1L, 11L);
        assertEquals(ProductStatus.INACTIVE, variant.getStatus());
        assertEquals(7L, variant.getInventory().getQuantity());
        assertTrue(product.getUpdatedAt().isAfter(BEFORE));
    }

    @Test
    void variantFromAnotherProductIsNotModified() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        assertThrows(ResourceNotFoundException.class,
                () -> service.changeProductVariantStatus(1L, 99L, ProductStatus.ACTIVE));
        verify(productRepository, never()).save(any());
    }

    @Test
    void missingProductIsRejected() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.changeProductVariantStatus(99L, 11L, ProductStatus.ACTIVE));
    }

    @Test
    void unsupportedOrMissingStatusIsRejectedBeforeLoadingProduct() {
        assertThrows(BadRequestException.class,
                () -> service.changeProductVariantStatus(1L, 11L, ProductStatus.HIDDEN));
        assertThrows(BadRequestException.class,
                () -> service.changeProductVariantStatus(1L, 11L, null));
        verifyNoInteractions(productRepository);
    }
}