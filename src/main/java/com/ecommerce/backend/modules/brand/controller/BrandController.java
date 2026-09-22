package com.ecommerce.backend.modules.brand.controller;

import com.ecommerce.backend.common.utils.FileValidator;
import com.ecommerce.backend.modules.brand.dto.request.BrandRequest;
import com.ecommerce.backend.modules.brand.dto.response.BrandResponse;
import com.ecommerce.backend.modules.brand.service.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @GetMapping
    public ResponseEntity<List<BrandResponse>> getBrands(@RequestParam(required = false) String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            return ResponseEntity.ok(brandService.searchBrands(keyword));
        }
        return ResponseEntity.ok(brandService.getAllBrands());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BrandResponse> createBrand(
            @Valid @ModelAttribute BrandRequest request,
            @RequestPart("logo") MultipartFile logoFile) {

        FileValidator.validateImageFile(logoFile);

        BrandResponse response = brandService.createBrand(request, logoFile);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BrandResponse> updateBrand(
            @PathVariable Long id,
            @Valid @ModelAttribute BrandRequest request,
            @RequestPart(value = "logo", required = false) MultipartFile logoFile) {

        if (logoFile != null && !logoFile.isEmpty()) {
            FileValidator.validateImageFile(logoFile);
        }

        return ResponseEntity.ok(brandService.updateBrand(id, request, logoFile));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBrand(@PathVariable Long id) {
        brandService.deleteBrand(id);
        return ResponseEntity.noContent().build();
    }
}

