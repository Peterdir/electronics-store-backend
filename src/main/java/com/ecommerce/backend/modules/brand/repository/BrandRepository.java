package com.ecommerce.backend.modules.brand.repository;

import com.ecommerce.backend.modules.brand.entity.Brand;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {

    @EntityGraph(attributePaths = {"products"})
    @Query("SELECT b FROM Brand b")
    List<Brand> findAllWithBrands();

    List<Brand> findByNameContaining(String keyword);

    Boolean existsByName(String keyword);

    Boolean existsByNameAndIdNot(String nameBrand, Long id);

}
