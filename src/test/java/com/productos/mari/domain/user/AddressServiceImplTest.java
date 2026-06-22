package com.productos.mari.domain.user;

import com.productos.mari.domain.infrastructure.media.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    @Mock private AddressRepository addressRepository;
    @Mock private UserRepository userRepository;
    @Mock private CloudinaryService cloudinaryService;

    @InjectMocks
    private AddressServiceImpl addressService;

    private User mockUser;
    private Address mockAddress;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@test.com");

        mockAddress = Address.builder()
                .id(10L)
                .user(mockUser)
                .title("Home")
                .isDefault(true)
                .imageUrl("http://old-img.jpg")
                .build();
    }

    @Test
    void getUserAddresses_Success() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockUser));
        when(addressRepository.findByUser(mockUser)).thenReturn(List.of(mockAddress));

        List<AddressDto> result = addressService.getUserAddresses("test@test.com");

        assertEquals(1, result.size());
        assertEquals("Home", result.get(0).getTitle());
    }

    @Test
    void addAddress_FirstAddress_SetsDefaultAutomatically() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockUser));
        when(addressRepository.findByUser(mockUser)).thenReturn(new ArrayList<>()); // Empty
        when(addressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AddressRequest req = new AddressRequest();
        req.setTitle("New Address");
        req.setDefault(false); // Should be overridden to true if first

        AddressDto result = addressService.addAddress("test@test.com", req);

        assertTrue(result.isDefault());
        verify(addressRepository).save(argThat(Address::isDefault));
    }

    @Test
    void addAddress_WithImage() throws Exception {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockUser));
        when(addressRepository.findByUser(mockUser)).thenReturn(List.of(mockAddress));
        when(cloudinaryService.uploadBase64(anyString(), anyString())).thenReturn("http://new-img.jpg");
        when(addressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AddressRequest req = new AddressRequest();
        req.setImage("base64data");

        AddressDto result = addressService.addAddress("test@test.com", req);

        assertEquals("http://new-img.jpg", result.getImageUrl());
    }

    @Test
    void updateAddress_OwnershipCheck_AndImageCleanup() throws Exception {
        when(addressRepository.findById(10L)).thenReturn(Optional.of(mockAddress));
        when(cloudinaryService.extractPublicId(anyString())).thenReturn("old_id");
        when(addressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AddressRequest req = new AddressRequest();
        req.setTitle("Updated Title");
        req.setImage("newBase64");

        AddressDto result = addressService.updateAddress("test@test.com", 10L, req);

        assertEquals("Updated Title", result.getTitle());
        verify(cloudinaryService).deleteFile("old_id");
    }

    @Test
    void deleteAddress_ReassignsDefault_IfNecessary() throws Exception {
        Address addr2 = Address.builder().id(11L).user(mockUser).isDefault(false).build();
        
        when(addressRepository.findById(10L)).thenReturn(Optional.of(mockAddress)); // default
        when(addressRepository.findByUser(mockUser)).thenReturn(List.of(addr2)); // only addr2 remains in list after deletion flow
        when(cloudinaryService.extractPublicId(anyString())).thenReturn("old_id");

        addressService.deleteAddress("test@test.com", 10L);

        verify(addressRepository).delete(mockAddress);
        verify(addressRepository).save(argThat(a -> a.getId().equals(11L) && a.isDefault()));
    }

    @Test
    void deleteAddress_ThrowsIfUnauthorized() {
        mockAddress.getUser().setEmail("other@test.com");
        when(addressRepository.findById(10L)).thenReturn(Optional.of(mockAddress));

        assertThrows(IllegalArgumentException.class, () -> addressService.deleteAddress("test@test.com", 10L));
    }
}
