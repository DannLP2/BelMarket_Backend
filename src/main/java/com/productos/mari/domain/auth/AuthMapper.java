package com.productos.mari.domain.auth;

import com.productos.mari.domain.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    @Mapping(target = "token", ignore = true)
    @Mapping(target = "refreshToken", ignore = true)
    @Mapping(target = "message", ignore = true)
    @Mapping(target = "needsVerification", ignore = true)
    @Mapping(target = "location", source = "location")
    AuthResponse toResponse(User user);
}
