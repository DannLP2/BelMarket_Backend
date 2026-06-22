package com.productos.mari.domain.product;

import com.productos.mari.domain.product.ProductDto;
import com.productos.mari.domain.product.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "brand", source = "brand.name")
    @Mapping(target = "categories", expression = "java(product.getCategories() != null ? product.getCategories().stream().map(com.productos.mari.domain.category.Category::getName).collect(java.util.stream.Collectors.toList()) : java.util.Collections.emptyList())")
    @Mapping(target = "imageUrl", source = "mainImageUrl")
    @Mapping(target = "price", qualifiedByName = "normalizePrice")
    @Mapping(target = "purchasePrice", qualifiedByName = "normalizePrice")
    @Mapping(target = "discountedPrice", ignore = true)
    @Mapping(target = "activeOffer", ignore = true)
    ProductDto toDto(Product product);

    @Mapping(target = "brand", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "mainImageUrl", source = "imageUrl")
    @Mapping(target = "detailLists", ignore = true) // Handled manually in service for bi-directional link
    @Mapping(target = "price", qualifiedByName = "normalizePrice")
    @Mapping(target = "purchasePrice", qualifiedByName = "normalizePrice")
    Product toEntity(ProductDto productDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "brand", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "mainImageUrl", ignore = true)
    @Mapping(target = "galleryImageUrls", ignore = true)
    @Mapping(target = "manuals", ignore = true)
    @Mapping(target = "detailLists", ignore = true)
    @Mapping(target = "price", qualifiedByName = "normalizePrice")
    @Mapping(target = "purchasePrice", qualifiedByName = "normalizePrice")
    @Mapping(target = "stock", ignore = true) // Handled independently due to reservations
    void updateEntityFromDto(ProductDto productDto, @org.mapstruct.MappingTarget Product product);

    @Named("normalizePrice")
    default BigDecimal normalizePrice(BigDecimal price) {
        if (price == null) return BigDecimal.ZERO;
        if (price.compareTo(BigDecimal.valueOf(1000)) < 0 && price.compareTo(BigDecimal.ZERO) > 0) {
            return price.multiply(BigDecimal.valueOf(1000));
        }
        return price;
    }
}
