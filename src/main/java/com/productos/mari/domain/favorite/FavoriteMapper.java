package com.productos.mari.domain.favorite;

import com.productos.mari.domain.product.ProductMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ProductMapper.class})
public interface FavoriteMapper {

    @Mapping(source = "product.id", target = "productId")
    FavoriteDto toDto(Favorite favorite);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "product", ignore = true)
    Favorite toEntity(FavoriteDto favoriteDto);
}
