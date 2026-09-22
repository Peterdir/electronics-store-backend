package com.ecommerce.backend.modules.product.controller;

import com.ecommerce.backend.modules.product.dto.response.*;
import com.ecommerce.backend.modules.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailsResponse> getProductDetails(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductDetails(id));
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> searchProducts(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponse> response = productService.searchProducts(keyword, pageable);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/variants")
    public ResponseEntity<VariantResponse> getVariantDetails(
            @PathVariable Long id,
            @RequestParam Map<String, Object> attributes) {

        return ResponseEntity.ok(productService.getVariantDetails(id, attributes));
    }

    @GetMapping("/compare")
    public ResponseEntity<List<ProductCompareResponse>> getProductsForComparison(
            @RequestParam List<Long> ids) {

        return ResponseEntity.ok(productService.getProductsForComparison(ids));
    }

    @GetMapping("/suggest")
    public ResponseEntity<List<SuggestionResponse>> getSearchSuggestions(
            @RequestParam(name = "q", defaultValue = "") String query) {

        return ResponseEntity.ok(productService.getSearchSuggestions(query));
    }
}
