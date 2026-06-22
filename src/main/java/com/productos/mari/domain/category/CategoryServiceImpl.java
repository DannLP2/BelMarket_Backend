package com.productos.mari.domain.category;

import com.productos.mari.domain.category.CategoryDto;
import com.productos.mari.domain.category.Category;
import com.productos.mari.domain.category.CategoryRepository;
import com.productos.mari.domain.product.ProductRepository;
import com.productos.mari.domain.category.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final com.productos.mari.domain.infrastructure.audit.SecurityAuditService securityAuditService;

    private final com.productos.mari.domain.category.CategoryMapper categoryMapper;

    @Override
    public List<CategoryDto> getAllCategoriesWithCount() {
        return categoryRepository.findAllWithProductCount();
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long id, CategoryDto categoryDto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
        
        category.setName(categoryDto.getName().trim());
        Category saved = categoryRepository.save(category);

        String currentAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        securityAuditService.log(
            com.productos.mari.domain.auth.SecurityAction.CATEGORY_UPDATED,
            null,
            currentAdmin,
            "Categoría actualizada: " + saved.getName() + " (ID: " + saved.getId() + ")"
        );

        return categoryMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
        
        long count = productRepository.countByCategory(category);
        if (count > 0) {
            throw new RuntimeException("No se puede eliminar la categoría porque tiene " + count + " productos asociados. Reasigna o elimina los productos primero.");
        }
        
        String currentAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        securityAuditService.log(
            com.productos.mari.domain.auth.SecurityAction.CATEGORY_DELETED,
            null,
            currentAdmin,
            "Categoría eliminada: " + category.getName() + " (ID: " + id + ")"
        );

        categoryRepository.delete(category);
    }

    @Override
    @Transactional
    public CategoryDto createCategory(CategoryDto categoryDto) {
        if (categoryRepository.findByNameIgnoreCase(categoryDto.getName().trim()).isPresent()) {
            throw new RuntimeException("La categoría '" + categoryDto.getName() + "' ya existe.");
        }
        Category category = Category.builder()
                .name(categoryDto.getName().trim())
                .build();
        Category saved = categoryRepository.save(category);

        String currentAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        securityAuditService.log(
            com.productos.mari.domain.auth.SecurityAction.CATEGORY_CREATED,
            null,
            currentAdmin,
            "Categoría creada: " + saved.getName() + " (ID: " + saved.getId() + ")"
        );

        return categoryMapper.toDto(saved);
    }
}
