package com.productos.mari.domain.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddressDto {
    private Long id;
    private String title;
    private String street;
    private String city;
    private String department;
    private String country;
    private String neighborhood;
    private String apartmentOffice;
    private String receiverName;
    private String receiverPhone;
    private String reference;
    @JsonProperty("isDefault")
    private boolean isDefault;
    private String imageUrl;
    private Double latitude;
    private Double longitude;
}
