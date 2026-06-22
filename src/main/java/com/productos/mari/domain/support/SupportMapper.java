package com.productos.mari.domain.support;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SupportMapper {
    SupportRequestDto toDto(SupportRequest entity);
    @org.mapstruct.Mapping(target = "attachmentUrl", ignore = true)
    SupportRequest toEntity(SupportRequestDto dto);
}
