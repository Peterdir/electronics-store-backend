package com.ecommerce.backend.modules.brand;

import com.ecommerce.backend.modules.brand.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {

    List<Brand> findByNameContaining(String keyword);

    Boolean existsByName(String keyword);

    Boolean existsByNameAndIdNot(String nameBrand, Long id);

}
