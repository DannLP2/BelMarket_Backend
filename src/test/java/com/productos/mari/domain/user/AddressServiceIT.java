package com.productos.mari.domain.user;

import com.productos.mari.domain.infrastructure.media.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AddressServiceIT {

    @Autowired
    private AddressService addressService;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private CloudinaryService cloudinaryService;

    private String userEmail = "client@test.com";

    @BeforeEach
    void setUp() {
        addressRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(User.builder()
                .name("Client")
                .email(userEmail)
                .password("pass")
                .build());
    }

    @Test
    void addAddress_FirstAddress_ShouldBeDefault() {
        AddressRequest request = AddressRequest.builder()
                .title("Home")
                .street("Main St")
                .city("Popayan")
                .isDefault(false)
                .build();

        AddressDto saved = addressService.addAddress(userEmail, request);

        assertTrue(saved.isDefault());
        assertEquals(1, addressService.getUserAddresses(userEmail).size());
    }

    @Test
    void addAddress_WithDefault_ShouldUnsetPreviousDefault() {
        // Add first address
        addressService.addAddress(userEmail, AddressRequest.builder().title("First").street("S1").city("C1").isDefault(true).build());

        // Add second address as default
        AddressDto second = addressService.addAddress(userEmail, AddressRequest.builder().title("Second").street("S2").city("C2").isDefault(true).build());

        assertTrue(second.isDefault());
        
        List<AddressDto> all = addressService.getUserAddresses(userEmail);
        assertEquals(2, all.size());
        
        AddressDto first = all.stream().filter(a -> a.getTitle().equals("First")).findFirst().orElseThrow();
        assertFalse(first.isDefault());
    }

    @Test
    void deleteAddress_Default_ShouldAssignNextAsDefault() {
        AddressDto a1 = addressService.addAddress(userEmail, AddressRequest.builder().title("A1").street("S1").city("C1").isDefault(true).build());
        AddressDto a2 = addressService.addAddress(userEmail, AddressRequest.builder().title("A2").street("S2").city("C2").isDefault(false).build());

        addressService.deleteAddress(userEmail, a1.getId());

        List<AddressDto> remaining = addressService.getUserAddresses(userEmail);
        assertEquals(1, remaining.size());
        assertTrue(remaining.get(0).isDefault());
        assertEquals("A2", remaining.get(0).getTitle());
    }

    @Test
    void addAddress_WithImage_ShouldHandleCloudinary() throws Exception {
        when(cloudinaryService.uploadBase64(anyString(), anyString())).thenReturn("http://cloudinary.com/address.jpg");

        AddressRequest request = AddressRequest.builder()
                .title("Store")
                .street("S3")
                .city("C3")
                .image("base64data")
                .build();

        AddressDto saved = addressService.addAddress(userEmail, request);

        assertEquals("http://cloudinary.com/address.jpg", saved.getImageUrl());
    }
}
