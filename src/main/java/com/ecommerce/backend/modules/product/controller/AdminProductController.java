package com.ecommerce.backend.modules.product.controller;

import com.ecommerce.backend.modules.product.dto.request.AdminProductRequest;
import com.ecommerce.backend.modules.product.service.ProductService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import com.ecommerce.backend.modules.product.enums.ProductStatus;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

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
    public ResponseEntity<String> changeProductStatus(
            @PathVariable Long id,
            @RequestParam ProductStatus status) {

        productService.changeProductStatus(id, status);
        return ResponseEntity.ok("Product status updated to " + status + ".");
    }
}
