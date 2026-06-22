package com.productos.mari.domain.review;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(target = "productId", source = "review.product.id")
    @Mapping(target = "productName", source = "review.product.name")
    @Mapping(target = "userName", source = "review.user.name")
    @Mapping(target = "userId", source = "review.user.id")
    @Mapping(target = "userPictureUrl", source = "review.user.profilePictureUrl")
    ReviewDto toDto(Review review);
    
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Review toEntity(ReviewDto dto);
}
