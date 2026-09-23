package com.ecommerce.backend.modules.product.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.modules.product.dto.response.*;
import com.ecommerce.backend.modules.product.entity.Product;
import com.ecommerce.backend.modules.product.entity.ProductVariant;
import com.ecommerce.backend.modules.product.enums.ProductStatus;
import com.ecommerce.backend.modules.product.mapper.ProductMapper;
import com.ecommerce.backend.modules.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.time.Instant;

import com.ecommerce.backend.modules.product.dto.request.AdminProductRequest;
import com.ecommerce.backend.modules.product.entity.ProductImage;
import com.ecommerce.backend.modules.inventory.entity.Inventory;
import com.ecommerce.backend.modules.category.CategoryRepository;
import com.ecommerce.backend.modules.category.entity.Category;
import com.ecommerce.backend.modules.brand.BrandRepository;
import com.ecommerce.backend.modules.brand.entity.Brand;
import com.ecommerce.backend.common.utils.FileValidator;
import com.ecommerce.backend.common.service.FileStorageService;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public void createProduct(AdminProductRequest request, List<MultipartFile> images) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Brand brand = null;
        if (request.getBrandId() != null) {
            brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new ResourceNotFoundException("Brand not found"));
        }

        Product product = Product.builder()
                .name(request.getName())
                .basePrice(request.getBasePrice())
                .description(request.getDescription())
                .specification(request.getSpecification())
                .status(ProductStatus.ACTIVE)
                .category(category)
                .brand(brand)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        if (request.getVariants() != null && !request.getVariants().isEmpty()) {
            List<ProductVariant> variants = request.getVariants().stream().map(vReq -> {
                ProductVariant variant = ProductVariant.builder()
                        .sku(vReq.getSku())
                        .price(vReq.getPrice())
                        .attributes(vReq.getAttributes())
                        .status(ProductStatus.ACTIVE)
                        .product(product)
                        .build();

                Inventory inventory = Inventory.builder()
                        .quantity(vReq.getStock())
                        .updatedAt(Instant.now())
                        .productVariant(variant)
                        .build();

                variant.setInventory(inventory);
                return variant;
            }).toList();

            product.setVariants(variants);
        }

        if (images != null && !images.isEmpty()) {
            List<ProductImage> productImages = new ArrayList<>();
            int displayOrder = 0;
            for (MultipartFile file : images) {
                FileValidator.validateImageFile(file);
                String imageUrl = fileStorageService.uploadFile(file, "products");
                productImages.add(ProductImage.builder()
                        .imageUrl(imageUrl)
                        .displayOrder(displayOrder++)
                        .isPrimary(displayOrder == 1)
                        .product(product)
                        .build());
            }
            product.setProductImages(productImages);
        }

        productRepository.save(product);
    }

    @Override
    @Transactional
    public void updateProduct(Long id, AdminProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Brand brand = null;
        if (request.getBrandId() != null) {
            brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new ResourceNotFoundException("Brand not found"));
        }

        product.setName(request.getName());
        product.setBasePrice(request.getBasePrice());
        product.setDescription(request.getDescription());
        product.setSpecification(request.getSpecification());
        product.setCategory(category);
        product.setBrand(brand);
        product.setUpdatedAt(Instant.now());

        if (request.getVariants() != null) {
            for (var vReq : request.getVariants()) {
                var existingVariantOpt = product.getVariants().stream()
                        .filter(v -> vReq.getSku() != null && vReq.getSku().equals(v.getSku()))
                        .findFirst();

                if (existingVariantOpt.isPresent()) {
                    var variant = existingVariantOpt.get();
                    variant.setPrice(vReq.getPrice());
                    variant.setAttributes(vReq.getAttributes());
                    if (variant.getInventory() != null) {
                        variant.getInventory().setQuantity(vReq.getStock());
                        variant.getInventory().setUpdatedAt(Instant.now());
                    }
                } else {
                    ProductVariant newVariant = ProductVariant.builder()
                            .sku(vReq.getSku())
                            .price(vReq.getPrice())
                            .attributes(vReq.getAttributes())
                            .status(ProductStatus.ACTIVE)
                            .product(product)
                            .build();
                    Inventory inventory = Inventory.builder()
                            .quantity(vReq.getStock())
                            .updatedAt(Instant.now())
                            .productVariant(newVariant)
                            .build();
                    newVariant.setInventory(inventory);
                    product.getVariants().add(newVariant);
                }
            }
        }

        productRepository.save(product);
    }

    @Override
    @Transactional
    public void changeProductStatus(Long id, ProductStatus status) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (status == product.getStatus()) {
            throw new BadRequestException("Product is already in " + status + " status.");
        }

        product.setStatus(status);
        product.getVariants().forEach(v -> v.setStatus(status));

        productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailsResponse getProductDetails(Long id) {
        Product product = productRepository.findByIdAndStatus(id, ProductStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Long totalStock = product.getVariants().stream()
                .map(variant -> variant.getInventory() != null ? variant.getInventory().getQuantity() : 0L)
                .reduce(0L, Long::sum);

        return productMapper.toProductDetailsResponse(product, totalStock);
    }

    @Override
    @Transactional(readOnly = true)
    public VariantResponse getVariantDetails(Long productId, Map<String, Object> attributes) {
        Product product = productRepository.findByIdAndStatus(productId, ProductStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        ProductVariant matchedVariant = product.getVariants().stream()
                .filter(variant -> variant.getStatus() == ProductStatus.ACTIVE)
                .filter(variant -> matchAttributes(variant.getAttributes(), attributes))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));

        return productMapper.toVariantResponse(matchedVariant);
    }

    private boolean matchAttributes(Map<String, Object> variantAttributes, Map<String, Object> queryAttributes) {
        if (variantAttributes == null || queryAttributes == null || queryAttributes.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, Object> entry : queryAttributes.entrySet()) {
            Object variantValue = variantAttributes.get(entry.getKey());
            if (variantValue == null || !String.valueOf(variantValue).equals(String.valueOf(entry.getValue()))) {
                return false;
            }
        }
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(String keyword, Pageable pageable) {
        String searchKeyword = (keyword != null) ? keyword.trim() : "";

        Page<Product> productPage = productRepository.findByNameContainingIgnoreCaseAndStatus(
                searchKeyword,
                ProductStatus.ACTIVE,
                pageable
        );

        return productPage.map(productMapper::toProductResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductCompareResponse> getProductsForComparison(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<Product> products = productRepository.findAllByIdIn(ids);

        return products.stream()
                .map(productMapper::toProductCompareResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SuggestionResponse> getSearchSuggestions(String keyword) {
        String searchKeyword = (keyword != null) ? keyword.trim() : "";
        if (searchKeyword.isEmpty()) {
            return List.of();
        }

        List<Product> topProducts = productRepository.findTop5ByNameContaining(searchKeyword);
        return topProducts.stream()
                .map(productMapper::toSuggestionResponse)
                .toList();
    }
}
