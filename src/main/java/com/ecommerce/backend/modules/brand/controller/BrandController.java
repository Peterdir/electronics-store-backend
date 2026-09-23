package com.ecommerce.backend.modules.brand.controller;

import com.ecommerce.backend.modules.brand.dto.response.BrandResponse;
import com.ecommerce.backend.modules.brand.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

}

