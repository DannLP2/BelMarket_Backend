package com.productos.mari.domain.category;

import com.productos.mari.domain.category.Category;
import com.productos.mari.domain.category.CategoryDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByNameIgnoreCase(String name);

    @Query("SELECT new com.productos.mari.domain.category.CategoryDto(c.id, c.name, " +
           "(SELECT COUNT(p) FROM Product p WHERE c MEMBER OF p.categories)) " +
           "FROM Category c GROUP BY c.id, c.name ORDER BY c.name ASC")
    List<CategoryDto> findAllWithProductCount();
}
