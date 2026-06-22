package com.productos.mari.domain.user;

import com.productos.mari.domain.user.AddressDto;
import com.productos.mari.domain.user.AddressRequest;
import com.productos.mari.domain.user.Address;
import com.productos.mari.domain.user.User;
import com.productos.mari.domain.user.AddressRepository;
import com.productos.mari.domain.user.UserRepository;
import com.productos.mari.domain.user.AddressService;
import com.productos.mari.domain.infrastructure.media.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    public List<AddressDto> getUserAddresses(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return addressRepository.findByUser(user).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AddressDto addAddress(String email, AddressRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<Address> existing = addressRepository.findByUser(user);
        boolean isFirst = existing.isEmpty();

        if (request.isDefault() || isFirst) {
            existing.forEach(a -> a.setDefault(false));
            addressRepository.saveAll(existing);
        }

        String imageUrl = null;
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            try {
                imageUrl = cloudinaryService.uploadBase64(request.getImage(), "belmarket/addresses");
            } catch (Exception e) {
                // Log error but proceed without image if upload fails
                System.err.println("Error uploading address image: " + e.getMessage());
            }
        }

        Address address = Address.builder()
                .user(user)
                .title(request.getTitle())
                .street(request.getStreet())
                .city(request.getCity())
                .department(request.getDepartment())
                .country(request.getCountry())
                .neighborhood(request.getNeighborhood())
                .apartmentOffice(request.getApartmentOffice())
                .receiverName(request.getReceiverName())
                .receiverPhone(request.getReceiverPhone())
                .reference(request.getReference())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .imageUrl(imageUrl)
                .isDefault(request.isDefault() || isFirst)
                .build();

        return mapToDto(addressRepository.save(address));
    }

    @Override
    @Transactional
    public AddressDto updateAddress(String email, Long id, AddressRequest request) {
        Address address = getAddressOwnedByUser(email, id);

        address.setTitle(request.getTitle());
        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setDepartment(request.getDepartment());
        address.setCountry(request.getCountry());
        address.setNeighborhood(request.getNeighborhood());
        address.setApartmentOffice(request.getApartmentOffice());
        address.setReceiverName(request.getReceiverName());
        address.setReceiverPhone(request.getReceiverPhone());
        address.setReference(request.getReference());
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            try {
                // Delete old file if exists in Cloudinary
                if (address.getImageUrl() != null) {
                    String publicId = cloudinaryService.extractPublicId(address.getImageUrl());
                    if (publicId != null) cloudinaryService.deleteFile(publicId);
                }
                String newImageUrl = cloudinaryService.uploadBase64(request.getImage(), "belmarket/addresses");
                address.setImageUrl(newImageUrl);
            } catch (Exception e) {
                System.err.println("Error updating address image: " + e.getMessage());
            }
        }

        if (request.isDefault() && !address.isDefault()) {
            setDefaultAddressLogic(address.getUser(), address);
        }

        return mapToDto(addressRepository.save(address));
    }

    @Override
    @Transactional
    public void deleteAddress(String email, Long id) {
        Address address = getAddressOwnedByUser(email, id);

        // Si borra la default y hay más, asignamos default a otra
        User user = address.getUser();
        boolean wasDefault = address.isDefault();
        
        // Borrar foto de Cloudinary antes de borrar de DB para evitar fotos "muertas"
        if (address.getImageUrl() != null) {
            try {
                String publicId = cloudinaryService.extractPublicId(address.getImageUrl());
                if (publicId != null) {
                    cloudinaryService.deleteFile(publicId);
                }
            } catch (Exception e) {
                // Log and continue, we don't want a network error to prevent deleting the record
                System.err.println("Error deleting cloud photo for address: " + e.getMessage());
            }
        }

        addressRepository.delete(address);

        if (wasDefault) {
            List<Address> remaining = addressRepository.findByUser(user);
            if (!remaining.isEmpty()) {
                Address newDefault = remaining.get(0);
                newDefault.setDefault(true);
                addressRepository.save(newDefault);
            }
        }
    }

    @Override
    @Transactional
    public AddressDto setDefaultAddress(String email, Long id) {
        Address address = getAddressOwnedByUser(email, id);
        setDefaultAddressLogic(address.getUser(), address);
        return mapToDto(addressRepository.save(address));
    }

    private void setDefaultAddressLogic(User user, Address newDefault) {
        List<Address> addresses = addressRepository.findByUser(user);
        for (Address a : addresses) {
            a.setDefault(a.getId().equals(newDefault.getId()));
        }
        addressRepository.saveAll(addresses);
    }

    private Address getAddressOwnedByUser(String email, Long id) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dirección no encontrada"));
        if (!address.getUser().getEmail().equals(email)) {
            throw new IllegalArgumentException("No autorizado");
        }
        return address;
    }

    private AddressDto mapToDto(Address address) {
        return AddressDto.builder()
                .id(address.getId())
                .title(address.getTitle())
                .street(address.getStreet())
                .city(address.getCity())
                .department(address.getDepartment())
                .country(address.getCountry())
                .neighborhood(address.getNeighborhood())
                .apartmentOffice(address.getApartmentOffice())
                .receiverName(address.getReceiverName())
                .receiverPhone(address.getReceiverPhone())
                .reference(address.getReference())
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .imageUrl(address.getImageUrl())
                .isDefault(address.isDefault())
                .build();
    }
}
