package com.ecommerce.backend.modules.category.repository;

import com.ecommerce.backend.modules.category.entity.Category;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @EntityGraph(attributePaths = {"products"})
    @Query("SELECT c FROM Category c")
    List<Category> findAllWithProducts();

    List<Category> findByNameContaining(String keyword);

    Boolean existsByName(String categoryName);

    Boolean existsByNameAndIdNot(String nameCategory, Long id);

}
