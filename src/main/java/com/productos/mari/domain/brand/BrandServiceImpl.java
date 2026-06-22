package com.productos.mari.domain.brand;

import com.productos.mari.domain.brand.BrandDto;
import com.productos.mari.domain.brand.Brand;
import com.productos.mari.domain.brand.BrandRepository;
import com.productos.mari.domain.product.ProductRepository;
import com.productos.mari.domain.brand.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final com.productos.mari.domain.infrastructure.audit.SecurityAuditService securityAuditService;
    private final BrandMapper brandMapper;

    @Override
    public List<BrandDto> getAllBrandsWithCount() {
        return brandRepository.findAllWithProductCount();
    }

    @Override
    @Transactional
    public BrandDto updateBrand(Long id, BrandDto brandDto) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Marca no encontrada"));
        
        brand.setName(brandDto.getName().trim());
        Brand saved = brandRepository.save(brand);

        String currentAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        securityAuditService.log(
            com.productos.mari.domain.auth.SecurityAction.BRAND_UPDATED,
            null,
            currentAdmin,
            "Marca actualizada: " + saved.getName() + " (ID: " + saved.getId() + ")"
        );

        return brandMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteBrand(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Marca no encontrada"));
        
        long count = productRepository.countByBrand(brand);
        if (count > 0) {
            throw new RuntimeException("No se puede eliminar la marca porque tiene " + count + " productos asociados.");
        }
        
        String currentAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        securityAuditService.log(
            com.productos.mari.domain.auth.SecurityAction.BRAND_DELETED,
            null,
            currentAdmin,
            "Marca eliminada: " + brand.getName() + " (ID: " + id + ")"
        );

        brandRepository.delete(brand);
    }

    @Override
    @Transactional
    public BrandDto createBrand(BrandDto brandDto) {
        if (brandRepository.findByNameIgnoreCase(brandDto.getName().trim()).isPresent()) {
            throw new RuntimeException("La marca '" + brandDto.getName() + "' ya existe.");
        }
        Brand brand = Brand.builder()
                .name(brandDto.getName().trim())
                .build();
        Brand saved = brandRepository.save(brand);

        String currentAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        securityAuditService.log(
            com.productos.mari.domain.auth.SecurityAction.BRAND_CREATED,
            null,
            currentAdmin,
            "Marca creada: " + saved.getName() + " (ID: " + saved.getId() + ")"
        );

        return brandMapper.toDto(saved);
    }

    // Manual mapping removed in favor of MapStruct
}
