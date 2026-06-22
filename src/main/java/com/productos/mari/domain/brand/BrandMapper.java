package com.productos.mari.domain.brand;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BrandMapper {
    @Mapping(target = "productCount", ignore = true)
    BrandDto toDto(Brand brand);
    Brand toEntity(BrandDto dto);
}
