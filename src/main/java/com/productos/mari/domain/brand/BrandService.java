package com.productos.mari.domain.brand;

import com.productos.mari.domain.brand.BrandDto;
import java.util.List;

public interface BrandService {
    List<BrandDto> getAllBrandsWithCount();
    BrandDto updateBrand(Long id, BrandDto brandDto);
    void deleteBrand(Long id);
    BrandDto createBrand(BrandDto brandDto);
}
