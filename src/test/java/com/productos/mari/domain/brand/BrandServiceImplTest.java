package com.productos.mari.domain.brand;

import com.productos.mari.domain.infrastructure.audit.SecurityAuditService;
import com.productos.mari.domain.product.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrandServiceImplTest {

    @Mock private BrandRepository brandRepository;
    @Mock private ProductRepository productRepository;
    @Mock private SecurityAuditService securityAuditService;
    @Mock private BrandMapper brandMapper;

    @InjectMocks private BrandServiceImpl brandService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("admin@test.com", "password"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllBrandsWithCount_shouldDelegatToRepository() {
        when(brandRepository.findAllWithProductCount()).thenReturn(List.of(new BrandDto()));

        List<BrandDto> result = brandService.getAllBrandsWithCount();

        assertEquals(1, result.size());
        verify(brandRepository, times(1)).findAllWithProductCount();
    }

    @Test
    void createBrand_shouldThrowIfNameExists() {
        BrandDto dto = BrandDto.builder().name("Existing Brand").build();
        Brand existing = Brand.builder().id(1L).name("existing brand").build();
        when(brandRepository.findByNameIgnoreCase("Existing Brand")).thenReturn(Optional.of(existing));

        assertThrows(RuntimeException.class, () -> brandService.createBrand(dto));
        verify(brandRepository, never()).save(any());
    }

    @Test
    void createBrand_shouldSaveAndAudit() {
        BrandDto dto = BrandDto.builder().name("New Brand").build();
        Brand saved = Brand.builder().id(1L).name("New Brand").build();
        BrandDto expectedDto = BrandDto.builder().id(1L).name("New Brand").build();

        when(brandRepository.findByNameIgnoreCase("New Brand")).thenReturn(Optional.empty());
        when(brandRepository.save(any(Brand.class))).thenReturn(saved);
        when(brandMapper.toDto(saved)).thenReturn(expectedDto);

        BrandDto result = brandService.createBrand(dto);

        assertEquals("New Brand", result.getName());
        verify(brandRepository, times(1)).save(any(Brand.class));
        verify(securityAuditService, times(1)).log(any(), any(), anyString(), anyString());
    }

    @Test
    void updateBrand_shouldThrowIfNotFound() {
        when(brandRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,
            () -> brandService.updateBrand(99L, BrandDto.builder().name("X").build()));
    }

    @Test
    void updateBrand_shouldUpdateNameAndAudit() {
        Brand brand = Brand.builder().id(1L).name("Old Name").build();
        Brand saved = Brand.builder().id(1L).name("New Name").build();
        BrandDto dto = BrandDto.builder().name("New Name").build();
        BrandDto expectedDto = BrandDto.builder().id(1L).name("New Name").build();

        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(brandRepository.save(brand)).thenReturn(saved);
        when(brandMapper.toDto(saved)).thenReturn(expectedDto);

        BrandDto result = brandService.updateBrand(1L, dto);

        assertEquals("New Name", result.getName());
        verify(securityAuditService, times(1)).log(any(), any(), anyString(), anyString());
    }

    @Test
    void deleteBrand_shouldThrowIfHasProducts() {
        Brand brand = Brand.builder().id(1L).name("Active Brand").build();
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(productRepository.countByBrand(brand)).thenReturn(3L);

        assertThrows(RuntimeException.class, () -> brandService.deleteBrand(1L));
        verify(brandRepository, never()).delete(any());
    }

    @Test
    void deleteBrand_shouldDeleteIfNoProducts() {
        Brand brand = Brand.builder().id(1L).name("Empty Brand").build();
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(productRepository.countByBrand(brand)).thenReturn(0L);

        brandService.deleteBrand(1L);

        verify(brandRepository, times(1)).delete(brand);
        verify(securityAuditService, times(1)).log(any(), any(), anyString(), anyString());
    }
}
