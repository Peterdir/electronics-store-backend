package com.ecommerce.backend.modules.brand.service;

import com.ecommerce.backend.modules.brand.dto.request.BrandRequest;
import com.ecommerce.backend.modules.brand.dto.response.BrandResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BrandService {

    List<BrandResponse> getAllBrands();

    List<BrandResponse> searchBrands(String keyword);

    BrandResponse createBrand(BrandRequest request, MultipartFile logoFile);

    BrandResponse updateBrand(Long id, BrandRequest request, MultipartFile logoFile);

    void deleteBrand(Long id);
}
