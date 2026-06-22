package com.productos.mari.domain.user;

import com.productos.mari.domain.user.AddressDto;
import com.productos.mari.domain.user.AddressRequest;

import java.util.List;

public interface AddressService {
    List<AddressDto> getUserAddresses(String email);
    AddressDto addAddress(String email, AddressRequest request);
    AddressDto updateAddress(String email, Long id, AddressRequest request);
    void deleteAddress(String email, Long id);
    AddressDto setDefaultAddress(String email, Long id);
}
