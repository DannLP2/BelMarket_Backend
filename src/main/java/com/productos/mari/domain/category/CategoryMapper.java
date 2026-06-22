package com.productos.mari.domain.category;

import com.productos.mari.domain.category.CategoryDto;
import com.productos.mari.domain.category.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "productCount", constant = "0L")
    CategoryDto toDto(Category category);

    Category toEntity(CategoryDto categoryDto);
}
