package com.productos.mari.domain.infrastructure.media;

import com.productos.mari.domain.marketing.Advertiser;
import com.productos.mari.domain.marketing.AdvertiserRepository;
import com.productos.mari.domain.marketing.Banner;
import com.productos.mari.domain.marketing.BannerRepository;
import com.productos.mari.domain.product.Product;
import com.productos.mari.domain.product.ProductRepository;
import com.productos.mari.domain.reservation.Reservation;
import com.productos.mari.domain.reservation.ReservationItem;
import com.productos.mari.domain.reservation.ReservationItemRepository;
import com.productos.mari.domain.reservation.ReservationRepository;
import com.productos.mari.domain.settings.AppSettings;
import com.productos.mari.domain.settings.AppSettingsRepository;
import com.productos.mari.domain.support.SupportRequest;
import com.productos.mari.domain.support.SupportRequestRepository;
import com.productos.mari.domain.user.Address;
import com.productos.mari.domain.user.AddressRepository;
import com.productos.mari.domain.user.User;
import com.productos.mari.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceImplTest {

    @Mock private CloudinaryService cloudinaryService;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private AppSettingsRepository appSettingsRepository;
    @Mock private BannerRepository bannerRepository;
    @Mock private ReservationItemRepository reservationItemRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private SupportRequestRepository supportRequestRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private AdvertiserRepository advertiserRepository;

    @InjectMocks
    private MediaServiceImpl mediaService;

    @Test
    void shouldDetectMediaInUseByProductAndUserAndSettings() throws Exception {
        // Arrange Cloudinary Return
        MediaDto activeProductMedia = MediaDto.builder().publicId("v1234/product_img").build();
        MediaDto activeUserMedia = MediaDto.builder().publicId("user_avatar").build();
        MediaDto activeSettingsMedia = MediaDto.builder().publicId("logo_global").build();
        when(cloudinaryService.listResources()).thenReturn(List.of(activeProductMedia, activeUserMedia, activeSettingsMedia));

        // Arrange Entities
        Product product = new Product();
        product.setName("Test Product");
        product.setMainImageUrl("https://res.cloudinary.com/test/image/upload/v1234/product_img.jpg");
        when(productRepository.findAll()).thenReturn(List.of(product));

        User user = new User();
        user.setName("Test User");
        user.setProfilePictureUrl("https://res.cloudinary.com/test/image/upload/user_avatar.jpg");
        when(userRepository.findAll()).thenReturn(List.of(user));

        AppSettings settings = new AppSettings();
        settings.setLogoUrl("https://res.cloudinary.com/test/image/upload/logo_global.jpg");
        when(appSettingsRepository.findAll()).thenReturn(List.of(settings));

        // Other empty mocks
        when(bannerRepository.findAll()).thenReturn(Collections.emptyList());
        when(reservationRepository.findAll()).thenReturn(Collections.emptyList());
        when(reservationItemRepository.findAll()).thenReturn(Collections.emptyList());
        when(addressRepository.findAll()).thenReturn(Collections.emptyList());
        when(supportRequestRepository.findAll()).thenReturn(Collections.emptyList());
        when(advertiserRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<MediaDto> result = mediaService.getAllMedia();

        // Assert
        assertEquals(3, result.size());
        
        MediaDto prodMedia = result.stream().filter(m -> m.getPublicId().equals("v1234/product_img")).findFirst().get();
        assertTrue(prodMedia.isInUse());
        assertEquals(1, prodMedia.getUsedIn().size());
        assertEquals("Producto", prodMedia.getUsedIn().get(0).getType());

        MediaDto usrMedia = result.stream().filter(m -> m.getPublicId().equals("user_avatar")).findFirst().get();
        assertTrue(usrMedia.isInUse());
        assertEquals("Usuario", usrMedia.getUsedIn().get(0).getType());

        MediaDto setMedia = result.stream().filter(m -> m.getPublicId().equals("logo_global")).findFirst().get();
        assertTrue(setMedia.isInUse());
        assertEquals("Sistema", setMedia.getUsedIn().get(0).getType());
    }

    @Test
    void shouldDetectOrphanMedia() throws Exception {
        MediaDto orphanMedia = MediaDto.builder().publicId("orphan_img").build();
        when(cloudinaryService.listResources()).thenReturn(List.of(orphanMedia));

        when(productRepository.findAll()).thenReturn(Collections.emptyList());
        when(userRepository.findAll()).thenReturn(Collections.emptyList());
        when(appSettingsRepository.findAll()).thenReturn(Collections.emptyList());
        when(bannerRepository.findAll()).thenReturn(Collections.emptyList());
        when(reservationRepository.findAll()).thenReturn(Collections.emptyList());
        when(reservationItemRepository.findAll()).thenReturn(Collections.emptyList());
        when(addressRepository.findAll()).thenReturn(Collections.emptyList());
        when(supportRequestRepository.findAll()).thenReturn(Collections.emptyList());
        when(advertiserRepository.findAll()).thenReturn(Collections.emptyList());

        List<MediaDto> result = mediaService.getAllMedia();

        assertEquals(1, result.size());
        assertFalse(result.get(0).isInUse(), "Media should be marked as orphan (inUse = false)");
        assertTrue(result.get(0).getUsedIn().isEmpty());
    }

    @Test
    void shouldDeleteSingleMedia() throws Exception {
        mediaService.deleteMedia("my_pic");
        verify(cloudinaryService).deleteFile("my_pic");
    }

    @Test
    void shouldDeleteBulkMedia() throws Exception {
        List<String> ids = List.of("img1", "img2", "img3");
        mediaService.deleteMediaBulk(ids);
        verify(cloudinaryService, times(1)).deleteFile("img1");
        verify(cloudinaryService, times(1)).deleteFile("img2");
        verify(cloudinaryService, times(1)).deleteFile("img3");
    }
}
