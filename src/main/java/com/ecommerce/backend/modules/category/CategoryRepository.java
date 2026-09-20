package com.ecommerce.backend.modules.category;

import com.ecommerce.backend.modules.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByNameContaining(String keyword);

    Boolean existsByName(String categoryName);

    Boolean existsByNameAndIdNot(String nameCategory, Long id);

}
