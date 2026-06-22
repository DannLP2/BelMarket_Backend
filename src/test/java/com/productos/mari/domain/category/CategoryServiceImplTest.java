package com.productos.mari.domain.category;

import com.productos.mari.domain.infrastructure.audit.SecurityAuditService;
import com.productos.mari.domain.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CategoryServiceImplTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductRepository productRepository;
    @Mock private SecurityAuditService securityAuditService;
    @Mock private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category mockCategory;

    @BeforeEach
    void setUp() {
        mockCategory = Category.builder().id(1L).name("Electronics").build();
        
        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(auth.getName()).thenReturn("admin@test.com");
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void getAllCategoriesWithCount_ReturnsList() {
        when(categoryRepository.findAllWithProductCount()).thenReturn(List.of(new CategoryDto()));
        List<CategoryDto> result = categoryService.getAllCategoriesWithCount();
        assertFalse(result.isEmpty());
    }

    @Test
    void createCategory_Success() {
        CategoryDto dto = new CategoryDto();
        dto.setName("New Category");

        when(categoryRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(categoryRepository.save(any())).thenReturn(mockCategory);
        when(categoryMapper.toDto(any())).thenReturn(new CategoryDto());

        CategoryDto result = categoryService.createCategory(dto);

        assertNotNull(result);
        verify(securityAuditService).log(any(), any(), anyString(), anyString());
    }

    @Test
    void createCategory_Duplicate_ThrowsException() {
        CategoryDto dto = new CategoryDto();
        dto.setName("Electronics");

        when(categoryRepository.findByNameIgnoreCase("Electronics")).thenReturn(Optional.of(mockCategory));

        assertThrows(RuntimeException.class, () -> categoryService.createCategory(dto));
    }

    @Test
    void updateCategory_Success() {
        CategoryDto dto = new CategoryDto();
        dto.setName("Updated Name");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory));
        when(categoryRepository.save(any())).thenReturn(mockCategory);
        when(categoryMapper.toDto(any())).thenReturn(new CategoryDto());

        CategoryDto result = categoryService.updateCategory(1L, dto);

        assertNotNull(result);
        assertEquals("Updated Name", mockCategory.getName());
    }

    @Test
    void deleteCategory_Success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory));
        when(productRepository.countByCategory(mockCategory)).thenReturn(0L);

        categoryService.deleteCategory(1L);

        verify(categoryRepository).delete(mockCategory);
    }

    @Test
    void deleteCategory_WithProducts_ThrowsException() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory));
        when(productRepository.countByCategory(mockCategory)).thenReturn(5L);

        assertThrows(RuntimeException.class, () -> categoryService.deleteCategory(1L));
    }
}
