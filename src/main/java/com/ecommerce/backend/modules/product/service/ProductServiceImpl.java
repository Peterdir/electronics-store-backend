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
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Objects;
import java.time.Instant;

import com.ecommerce.backend.modules.product.dto.request.AdminProductRequest;
import com.ecommerce.backend.modules.product.entity.ProductImage;
import com.ecommerce.backend.modules.inventory.entity.Inventory;
import com.ecommerce.backend.modules.category.repository.CategoryRepository;
import com.ecommerce.backend.modules.category.entity.Category;
import com.ecommerce.backend.modules.brand.repository.BrandRepository;
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

        Instant now = Instant.now();
        Product product = Product.builder()
                .name(request.getName())
                .basePrice(request.getBasePrice())
                .description(request.getDescription())
                .specification(request.getSpecification())
                .status(ProductStatus.ACTIVE)
                .category(category)
                .brand(brand)
                .createdAt(now)
                .updatedAt(now)
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
                        .updatedAt(now)
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

        Instant now = Instant.now();
        boolean productChanged = false;
        boolean inventoryChanged = false;

        if (!Objects.equals(product.getName(), request.getName())) {
            product.setName(request.getName());
            productChanged = true;
        }
        if (!isBigDecimalEqual(product.getBasePrice(), request.getBasePrice())) {
            product.setBasePrice(request.getBasePrice());
            productChanged = true;
        }
        if (!Objects.equals(product.getDescription(), request.getDescription())) {
            product.setDescription(request.getDescription());
            productChanged = true;
        }
        if (!isMapEqual(product.getSpecification(), request.getSpecification())) {
            product.setSpecification(request.getSpecification());
            productChanged = true;
        }

        Long existingCategoryId = product.getCategory() != null ? product.getCategory().getId() : null;
        if (!Objects.equals(existingCategoryId, category.getId())) {
            product.setCategory(category);
            productChanged = true;
        }

        Long existingBrandId = product.getBrand() != null ? product.getBrand().getId() : null;
        Long newBrandId = brand != null ? brand.getId() : null;
        if (!Objects.equals(existingBrandId, newBrandId)) {
            product.setBrand(brand);
            productChanged = true;
        }

        if (request.getVariants() != null) {
            for (var vReq : request.getVariants()) {
                var existingVariantOpt = product.getVariants().stream()
                        .filter(v -> vReq.getSku() != null && vReq.getSku().equals(v.getSku()))
                        .findFirst();

                if (existingVariantOpt.isPresent()) {
                    var variant = existingVariantOpt.get();
                    if (!isBigDecimalEqual(variant.getPrice(), vReq.getPrice())) {
                        variant.setPrice(vReq.getPrice());
                        productChanged = true;
                    }
                    if (!isMapEqual(variant.getAttributes(), vReq.getAttributes())) {
                        variant.setAttributes(vReq.getAttributes());
                        productChanged = true;
                    }
                    if (variant.getInventory() != null
                            && !Objects.equals(variant.getInventory().getQuantity(), vReq.getStock())) {
                        variant.getInventory().setQuantity(vReq.getStock());
                        variant.getInventory().setUpdatedAt(now);
                        inventoryChanged = true;
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
                            .updatedAt(now)
                            .productVariant(newVariant)
                            .build();
                    newVariant.setInventory(inventory);
                    product.getVariants().add(newVariant);
                    productChanged = true;
                }
            }
        }

        if (productChanged) {
            product.setUpdatedAt(now);
        }

        if (productChanged || inventoryChanged) {
            productRepository.save(product);
        }
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
        product.setUpdatedAt(Instant.now());

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
    public Page<ProductResponse> searchProducts(
            String keyword,
            Long categoryId,
            Long brandId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {

        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("status"), ProductStatus.ACTIVE));

            if (keyword != null && !keyword.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + keyword.trim().toLowerCase() + "%"));
            }

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            if (brandId != null) {
                predicates.add(cb.equal(root.get("brand").get("id"), brandId));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("basePrice"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("basePrice"), maxPrice));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Product> productPage = productRepository.findAll(spec, pageable);
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

    @Override
    @Transactional(readOnly = true)
    public Page<AdminProductListResponse> getAdminProducts(
            String keyword,
            ProductStatus status,
            Long categoryId,
            Long brandId,
            Pageable pageable) {

        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            if (brandId != null) {
                predicates.add(cb.equal(root.get("brand").get("id"), brandId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Product> page = productRepository.findAll(spec, pageable);
        return page.map(productMapper::toAdminProductListResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminProductDetailResponse getAdminProductDetails(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        return productMapper.toAdminProductDetailResponse(product);
    }

    @Override
    @Transactional
    public void addProductImages(Long productId, List<MultipartFile> images) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (images != null && !images.isEmpty()) {
            int displayOrder = product.getProductImages().size();
            boolean hasPrimary = product.getProductImages().stream()
                    .anyMatch(img -> Boolean.TRUE.equals(img.getIsPrimary()));

            for (MultipartFile file : images) {
                FileValidator.validateImageFile(file);
                String imageUrl = fileStorageService.uploadFile(file, "products");
                product.getProductImages().add(ProductImage.builder()
                        .imageUrl(imageUrl)
                        .displayOrder(displayOrder++)
                        .isPrimary(!hasPrimary && displayOrder == 1)
                        .product(product)
                        .build());
            }
            product.setUpdatedAt(Instant.now());
            productRepository.save(product);
        }
    }

    @Override
    @Transactional
    public void deleteProductImage(Long productId, Long imageId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        boolean removed = product.getProductImages().removeIf(img -> Objects.equals(img.getId(), imageId));
        if (!removed) {
            throw new ResourceNotFoundException("Image not found");
        }

        boolean hasPrimary = product.getProductImages().stream()
                .anyMatch(img -> Boolean.TRUE.equals(img.getIsPrimary()));
        if (!hasPrimary && !product.getProductImages().isEmpty()) {
            product.getProductImages().getFirst().setIsPrimary(true);
        }

        product.setUpdatedAt(Instant.now());
        productRepository.save(product);
    }

    @Override
    @Transactional
    public void setPrimaryProductImage(Long productId, Long imageId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        ProductImage target = product.getProductImages().stream()
                .filter(img -> Objects.equals(img.getId(), imageId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));

        boolean alreadySolePrimary = Boolean.TRUE.equals(target.getIsPrimary())
                && product.getProductImages().stream()
                .filter(img -> img != target)
                .noneMatch(img -> Boolean.TRUE.equals(img.getIsPrimary()));
        if (alreadySolePrimary) {
            return;
        }

        product.getProductImages().forEach(img -> img.setIsPrimary(img == target));
        product.setUpdatedAt(Instant.now());
        productRepository.save(product);
    }

    @Override
    @Transactional
    public void deleteProductVariant(Long productId, Long variantId) {
        changeProductVariantStatus(productId, variantId, ProductStatus.INACTIVE);
    }

    @Override
    @Transactional
    public void changeProductVariantStatus(Long productId, Long variantId, ProductStatus status) {
        if (status != ProductStatus.ACTIVE && status != ProductStatus.INACTIVE) {
            throw new BadRequestException("Variant status must be ACTIVE or INACTIVE.");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        ProductVariant variant = product.getVariants().stream()
                .filter(v -> Objects.equals(v.getId(), variantId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));

        if (variant.getStatus() == status) {
            return;
        }

        variant.setStatus(status);
        product.setUpdatedAt(Instant.now());
        productRepository.save(product);
    }

    private boolean isBigDecimalEqual(BigDecimal left, BigDecimal right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.compareTo(right) == 0;
    }

    private boolean isMapEqual(Map<String, Object> left, Map<String, Object> right) {
        boolean leftEmpty = left == null || left.isEmpty();
        boolean rightEmpty = right == null || right.isEmpty();
        if (leftEmpty && rightEmpty) {
            return true;
        }
        return Objects.equals(left, right);
    }
}
