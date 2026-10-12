package com.ecommerce.backend.modules.product.controller;

import com.ecommerce.backend.modules.product.dto.request.AdminProductRequest;
import com.ecommerce.backend.modules.product.dto.response.AdminProductDetailResponse;
import com.ecommerce.backend.modules.product.dto.response.AdminProductListResponse;
import com.ecommerce.backend.modules.product.service.ProductService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import com.ecommerce.backend.modules.product.enums.ProductStatus;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
public class AdminProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<Page<AdminProductListResponse>> getProducts(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        return ResponseEntity.ok(productService.getAdminProducts(keyword, status, categoryId, brandId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminProductDetailResponse> getProductDetails(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getAdminProductDetails(id));
    }

    @PostMapping
    public ResponseEntity<String> createProduct(
            @RequestPart("data") String requestJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) throws JsonProcessingException {
        
        ObjectMapper objectMapper = new ObjectMapper();
        AdminProductRequest request = objectMapper.readValue(requestJson, AdminProductRequest.class);
        
        productService.createProduct(request, images);
        return ResponseEntity.ok("Product created successfully.");
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateProduct(
            @PathVariable Long id,
            @RequestBody @Valid AdminProductRequest request) {
        
        productService.updateProduct(id, request);
        return ResponseEntity.ok("Product updated successfully.");
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<String> changeProductStatus(
            @PathVariable Long id,
            @RequestParam ProductStatus status) {

        productService.changeProductStatus(id, status);
        return ResponseEntity.ok("Product status updated to " + status + ".");
    }

    @PostMapping("/{id}/images")
    public ResponseEntity<String> addProductImages(
            @PathVariable Long id,
            @RequestPart("images") List<MultipartFile> images) {

        productService.addProductImages(id, images);
        return ResponseEntity.ok("Images added successfully.");
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<String> deleteProductImage(
            @PathVariable Long id,
            @PathVariable Long imageId) {

        productService.deleteProductImage(id, imageId);
        return ResponseEntity.ok("Image deleted successfully.");
    }

    @PatchMapping("/{id}/images/{imageId}/primary")
    public ResponseEntity<String> setPrimaryImage(
            @PathVariable Long id,
            @PathVariable Long imageId) {

        productService.setPrimaryProductImage(id, imageId);
        return ResponseEntity.ok("Primary image updated successfully.");
    }

    @DeleteMapping("/{id}/variants/{variantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<String> deleteProductVariant(
            @PathVariable Long id,
            @PathVariable Long variantId) {

        productService.deleteProductVariant(id, variantId);
        return ResponseEntity.ok("Product variant removed successfully.");
    }
}
