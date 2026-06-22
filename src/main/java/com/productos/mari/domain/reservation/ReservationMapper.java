package com.productos.mari.domain.reservation;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ReservationMapper {

    @Mapping(target = "userId", source = "reservation.user.id")
    @Mapping(target = "customerName", source = "reservation.user.name")
    @Mapping(target = "customerPhone", source = "reservation.user.phone")
    @Mapping(target = "customerEmail", source = "reservation.user.email")
    @Mapping(target = "customerProfilePictureUrl", source = "reservation.user.profilePictureUrl")
    @Mapping(target = "delivererId", source = "reservation.deliverer.id")
    @Mapping(target = "delivererName", source = "reservation.deliverer.name")
    @Mapping(target = "total", ignore = true)
    @Mapping(target = "shippingCost", ignore = true)
    @Mapping(target = "taxAmount", ignore = true)
    @Mapping(target = "reference", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "exchangeRate", ignore = true)
    ReservationDto toDto(Reservation reservation);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "deliverer", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "total", ignore = true)
    @Mapping(target = "shippingCost", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "version", ignore = true)
    Reservation toEntity(ReservationDto dto);
}
