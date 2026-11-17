package com.ecommerce.backend.modules.brand.repository;

import com.ecommerce.backend.modules.brand.entity.Brand;
import com.ecommerce.backend.modules.brand.dto.response.BrandResponse;
import com.ecommerce.backend.modules.brand.enums.BrandStatus;
import com.ecommerce.backend.modules.product.enums.ProductStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {

    @EntityGraph(attributePaths = {"products"})
    @Query("SELECT b FROM Brand b")
    List<Brand> findAllWithBrands();

    @Query("""
            SELECT new com.ecommerce.backend.modules.brand.dto.response.BrandResponse(
                b.id, b.name, b.logo, b.status, COUNT(DISTINCT p.id), b.createdAt, b.updatedAt)
            FROM Brand b
            JOIN b.products p
            WHERE p.category.id = :categoryId
              AND p.status = :productStatus
              AND b.status = :brandStatus
            GROUP BY b.id, b.name, b.logo, b.status, b.createdAt, b.updatedAt
            ORDER BY b.name
    """)
    List<BrandResponse> findByCategoryIdAndStatuses(
            @Param("categoryId") Long categoryId,
            @Param("productStatus") ProductStatus productStatus,
            @Param("brandStatus") BrandStatus brandStatus);

    @Query("""
            SELECT new com.ecommerce.backend.modules.brand.dto.response.BrandResponse(
                b.id, b.name, b.logo, b.status, COUNT(DISTINCT p.id), b.createdAt, b.updatedAt)
            FROM Brand b
            JOIN b.products p
            WHERE p.category.id = :categoryId
              AND p.status = :productStatus
              AND b.status = :brandStatus
              AND LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            GROUP BY b.id, b.name, b.logo, b.status, b.createdAt, b.updatedAt
            ORDER BY b.name
    """)
    List<BrandResponse> findByCategoryIdAndNameContaining(
            @Param("categoryId") Long categoryId,
            @Param("productStatus") ProductStatus productStatus,
            @Param("brandStatus") BrandStatus brandStatus,
            @Param("keyword") String keyword);

    List<Brand> findByNameContaining(String keyword);

    Boolean existsByName(String keyword);

    Boolean existsByNameAndIdNot(String nameBrand, Long id);

}
