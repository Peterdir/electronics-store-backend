package com.ecommerce.backend.modules.brand.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.common.exception.DuplicateResourceException;
import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.common.service.FileStorageService;
import com.ecommerce.backend.modules.brand.BrandRepository;
import com.ecommerce.backend.modules.brand.dto.request.BrandRequest;
import com.ecommerce.backend.modules.brand.dto.response.BrandResponse;
import com.ecommerce.backend.modules.brand.entity.Brand;
import com.ecommerce.backend.modules.brand.mapper.BrandMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;
    private final FileStorageService fileStorageService;

    private static final String BRAND_LOGO_FOLDER = "brands";

    @Override
    public List<BrandResponse> getAllBrands() {
        return brandRepository.findAll().stream()
                .map(brandMapper::toResponse)
                .toList();
    }

    @Override
    public List<BrandResponse> searchBrands(String keyword) {
        return brandRepository.findByNameContaining(keyword).stream()
                .map(brandMapper::toResponse)
                .toList();
    }

    @Override
    public BrandResponse createBrand(BrandRequest request, MultipartFile logoFile) {
        if (brandRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("This brand name already exists: " + request.getName());
        }

        Brand brand = brandMapper.toEntity(request);

        String logoUrl = fileStorageService.uploadFile(logoFile, BRAND_LOGO_FOLDER);
        brand.setLogo(logoUrl);

        Brand savedBrand = brandRepository.save(brand);

        return brandMapper.toResponse(savedBrand);
    }

    @Override
    public BrandResponse updateBrand(Long id, BrandRequest request, MultipartFile logoFile) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));

        if (brandRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new DuplicateResourceException("This brand name already exists: " + request.getName());
        }

        brand.setName(request.getName());
        brand.setStatus(request.getStatus());

        if (logoFile != null && !logoFile.isEmpty()) {
            String oldLogoUrl = brand.getLogo();

            String newLogoUrl = fileStorageService.uploadFile(logoFile, BRAND_LOGO_FOLDER);
            brand.setLogo(newLogoUrl);

            if (oldLogoUrl != null && !oldLogoUrl.isBlank()) {
                fileStorageService.deleteFile(oldLogoUrl);
            }
        }

        Brand updatedBrand = brandRepository.save(brand);

        return brandMapper.toResponse(updatedBrand);
    }

    @Override
    public void deleteBrand(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));

        if (brand.getProducts() != null && !brand.getProducts().isEmpty()) {
            throw new BadRequestException("Cannot delete this brand because it contains products");
        }

        if (brand.getLogo() != null && !brand.getLogo().isBlank()) {
            fileStorageService.deleteFile(brand.getLogo());
        }

        brandRepository.delete(brand);
    }
}

