package com.productos.mari.domain.category;

import com.productos.mari.domain.category.CategoryDto;
import java.util.List;

public interface CategoryService {
    List<CategoryDto> getAllCategoriesWithCount();
    CategoryDto updateCategory(Long id, CategoryDto categoryDto);
    void deleteCategory(Long id);
    CategoryDto createCategory(CategoryDto categoryDto);
}
