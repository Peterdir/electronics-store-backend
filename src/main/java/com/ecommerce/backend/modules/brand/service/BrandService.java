package com.ecommerce.backend.modules.brand.service;

import com.ecommerce.backend.modules.brand.dto.request.BrandRequest;
import com.ecommerce.backend.modules.brand.dto.response.BrandResponse;

import java.util.List;

public interface BrandService {

    List<BrandResponse> getAllBrands();

    List<BrandResponse> searchBrands(String keyword);

    BrandResponse createBrand(BrandRequest request);

    BrandResponse updateBrand(Long id, BrandRequest request);

    void deleteBrand(Long id);
}
