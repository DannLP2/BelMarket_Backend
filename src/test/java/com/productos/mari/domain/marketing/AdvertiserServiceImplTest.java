package com.productos.mari.domain.marketing;

import com.productos.mari.domain.infrastructure.audit.SecurityAuditService;
import com.productos.mari.domain.infrastructure.media.CloudinaryService;
import com.productos.mari.domain.notification.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdvertiserServiceImplTest {

    @Mock
    private AdvertiserRepository repository;

    @Mock
    private AdMetricRepository adMetricRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private SecurityAuditService securityAuditService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AdvertiserServiceImpl advertiserService;

    @BeforeEach
    void setUp() {
        // Setup mock security context
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@test.com", "password")
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getActiveAdvertisers_shouldReturnMappedDtos() {
        // Arrange
        Advertiser advertiser = Advertiser.builder()
                .id(1L)
                .name("AdTest Company")
                .buttonText("COMPRAR")
                .active(true)
                .placement(AdPlacement.PRODUCT_LIST)
                .viewsCount(0L)
                .clicksCount(0L)
                .build();
        
        when(repository.findByActiveTrueOrderByAdOrderAsc()).thenReturn(List.of(advertiser));

        // Act
        List<AdvertiserDto> result = advertiserService.getActiveAdvertisers();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("AdTest Company", result.get(0).getName());
        verify(repository, times(1)).findByActiveTrueOrderByAdOrderAsc();
    }

    @Test
    void createAdvertiser_shouldThrowExceptionIfNameExists() {
        // Arrange
        AdvertiserDto dto = AdvertiserDto.builder()
                .name("Existing Company")
                .build();
        when(repository.existsByName("Existing Company")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> advertiserService.createAdvertiser(dto));
        
        assertEquals("Ya existe un anunciante con el nombre: Existing Company", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void recordAdMetric_shouldIncrementCountAndSaveHistoricalMetric() {
        // Arrange
        Long advertiserId = 1L;
        String ipAddress = "127.0.0.1";
        
        Advertiser proxyAd = new Advertiser();
        proxyAd.setId(advertiserId);
        
        when(repository.getReferenceById(advertiserId)).thenReturn(proxyAd);

        // Act
        advertiserService.recordAdMetric(advertiserId, AdMetricType.CLICK, ipAddress);

        // Assert
        verify(repository, times(1)).incrementClicksCount(advertiserId);
        verify(repository, never()).incrementViewsCount(anyLong());
        verify(adMetricRepository, times(1)).save(argThat(metric -> 
            metric.getType() == AdMetricType.CLICK &&
            metric.getIpAddress().equals(ipAddress) &&
            metric.getAdvertiser().getId().equals(advertiserId)
        ));
    }
    
    @Test
    void deleteAdvertiser_shouldDeleteImagesAndEntity() throws Exception {
        // Arrange
        Long advertiserId = 1L;
        String logoUrl = "https://res.cloudinary.com/test/image/upload/v1234/logo.png";
        String adUrl = "https://res.cloudinary.com/test/image/upload/v1234/ad.png";
        
        Advertiser advertiser = Advertiser.builder()
                .id(advertiserId)
                .name("Delete Me Corp")
                .logoUrl(logoUrl)
                .adImageUrl(adUrl)
                .build();

        when(repository.findById(advertiserId)).thenReturn(Optional.of(advertiser));
        when(cloudinaryService.extractPublicId(logoUrl)).thenReturn("v1234/logo");
        when(cloudinaryService.extractPublicId(adUrl)).thenReturn("v1234/ad");

        // Act
        advertiserService.deleteAdvertiser(advertiserId);

        // Assert
        verify(cloudinaryService, times(1)).deleteFile("v1234/logo");
        verify(cloudinaryService, times(1)).deleteFile("v1234/ad");
        verify(repository, times(1)).delete(advertiser);
        verify(securityAuditService, times(1)).log(any(), any(), anyString(), anyString());
        verify(notificationService, times(1)).broadcastMarketingUpdate("ADVERTISER_DELETED");
    }

    @Test
    void updateAdvertiser_shouldThrowIfNewNameExists() {
        Long id = 1L;
        AdvertiserDto dto = AdvertiserDto.builder().name("Taken Name").build();
        Advertiser currentAd = Advertiser.builder().id(id).name("Old Name").build();

        when(repository.findById(id)).thenReturn(Optional.of(currentAd));
        when(repository.existsByNameAndIdNot("Taken Name", id)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> advertiserService.updateAdvertiser(id, dto));
    }

    @Test
    void updateAdvertiser_shouldUpdateSuccessfully() throws Exception {
        Long id = 1L;
        String oldLogoUrl = "https://res.cloudinary.com/test/image/upload/v1/old-logo.png";
        AdvertiserDto dto = AdvertiserDto.builder()
            .name("New Name")
            .logoUrl("https://res.cloudinary.com/test/image/upload/v2/new-logo.png")
            .build();
        Advertiser currentAd = Advertiser.builder()
            .id(id)
            .name("Old Name")
            .logoUrl(oldLogoUrl)
            .build();

        when(repository.findById(id)).thenReturn(Optional.of(currentAd));
        when(repository.existsByNameAndIdNot("New Name", id)).thenReturn(false);
        when(cloudinaryService.extractPublicId(anyString())).thenReturn("v1/old-logo");
        when(repository.save(any(Advertiser.class))).thenAnswer(i -> i.getArguments()[0]);

        AdvertiserDto result = advertiserService.updateAdvertiser(id, dto);

        assertEquals("New Name", result.getName());
        verify(cloudinaryService, times(1)).deleteFile("v1/old-logo");
        verify(repository, times(1)).save(currentAd);
        verify(securityAuditService, times(1)).log(any(), any(), anyString(), anyString());
    }

    @Test
    void toggleAdvertiserStatus_shouldInvertActiveFlag() {
        Long id = 1L;
        Advertiser ad = Advertiser.builder().id(id).active(true).name("Toggle Corp").build();
        when(repository.findById(id)).thenReturn(Optional.of(ad));

        advertiserService.toggleAdvertiserStatus(id);

        assertFalse(ad.isActive());
        verify(repository, times(1)).save(ad);
        verify(notificationService, times(1)).broadcastMarketingUpdate("ADVERTISER_STATUS_TOGGLED");
    }

    @Test
    void uploadLogo_shouldCallCloudinary() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(cloudinaryService.uploadFile(file, "belmarket/advertisers")).thenReturn("http://new-url.png");

        String result = advertiserService.uploadLogo(file);

        assertEquals("http://new-url.png", result);
        verify(cloudinaryService, times(1)).uploadFile(file, "belmarket/advertisers");
    }

    @Test
    void reorderAds_shouldUpdateMultipleAds() {
        Advertiser ad1 = Advertiser.builder().id(10L).adOrder(2).build();
        Advertiser ad2 = Advertiser.builder().id(20L).adOrder(3).build();

        when(repository.findById(10L)).thenReturn(Optional.of(ad1));
        when(repository.findById(20L)).thenReturn(Optional.of(ad2));

        advertiserService.reorderAds(List.of(20L, 10L));

        assertEquals(0, ad2.getAdOrder());
        assertEquals(1, ad1.getAdOrder());
        verify(repository, times(2)).save(any(Advertiser.class));
        verify(notificationService, times(1)).broadcastMarketingUpdate("ADVERTISERS_REORDERED");
    }

    @Test
    void getAllAdvertisersByPlacement_shouldDelegate() {
        Advertiser ad = Advertiser.builder().id(1L).name("Sidebar Ad").active(true)
                .placement(AdPlacement.PRODUCT_LIST).viewsCount(0L).clicksCount(0L).build();
        when(repository.findAllByPlacementOrderByAdOrderAsc(AdPlacement.PRODUCT_LIST)).thenReturn(List.of(ad));

        List<AdvertiserDto> result = advertiserService.getAllAdvertisersByPlacement(AdPlacement.PRODUCT_LIST);

        assertEquals(1, result.size());
        verify(repository).findAllByPlacementOrderByAdOrderAsc(AdPlacement.PRODUCT_LIST);
    }

    @Test
    void getActiveAdvertisersByPlacement_shouldDelegate() {
        Advertiser ad = Advertiser.builder().id(1L).name("Active Sidebar")
                .placement(AdPlacement.PRODUCT_DETAIL).active(true).viewsCount(0L).clicksCount(0L).build();
        when(repository.findByActiveTrueAndPlacementOrderByAdOrderAsc(AdPlacement.PRODUCT_DETAIL)).thenReturn(List.of(ad));

        List<AdvertiserDto> result = advertiserService.getActiveAdvertisersByPlacement(AdPlacement.PRODUCT_DETAIL);

        assertEquals(1, result.size());
        verify(repository).findByActiveTrueAndPlacementOrderByAdOrderAsc(AdPlacement.PRODUCT_DETAIL);
    }

    @Test
    void createAdvertiser_shouldSetDefaultButtonTextWhenEmpty() {
        AdvertiserDto dto = AdvertiserDto.builder().name("Empty Button Corp").buttonText("").build();
        Advertiser saved = Advertiser.builder().id(5L).name("Empty Button Corp")
                .buttonText("VER MÁS").viewsCount(0L).clicksCount(0L).active(false).build();

        when(repository.existsByName("Empty Button Corp")).thenReturn(false);
        when(repository.save(any(Advertiser.class))).thenReturn(saved);

        AdvertiserDto result = advertiserService.createAdvertiser(dto);

        assertEquals("VER MÁS", result.getButtonText());
    }

    @Test
    void deleteAdvertiser_shouldDoNothingIfNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        advertiserService.deleteAdvertiser(99L);

        verify(repository, never()).delete(any());
        verify(notificationService, never()).broadcastMarketingUpdate(anyString());
    }

    @Test
    void recordAdMetric_IMPRESSION_shouldIncrementViewsCount() {
        Long adId = 1L;
        Advertiser proxy = new Advertiser();
        proxy.setId(adId);
        when(repository.getReferenceById(adId)).thenReturn(proxy);

        advertiserService.recordAdMetric(adId, AdMetricType.IMPRESSION, "192.168.0.1");

        verify(repository, times(1)).incrementViewsCount(adId);
        verify(repository, never()).incrementClicksCount(anyLong());
        verify(adMetricRepository, times(1)).save(any(AdMetric.class));
    }

    @Test
    void getAllAdvertisers_shouldReturnMappedList() {
        Advertiser ad = Advertiser.builder().id(1L).name("All Ads")
                .active(true).viewsCount(0L).clicksCount(0L).build();
        when(repository.searchAdvertisers(any(), anyBoolean(), any())).thenReturn(List.of(ad));

        List<AdvertiserDto> result = advertiserService.getAllAdvertisers(null, null);

        assertEquals(1, result.size());
        assertEquals("All Ads", result.get(0).getName());
    }

    @Test
    void reorderAds_shouldSkipIfAdNotFound() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        advertiserService.reorderAds(List.of(999L));

        verify(repository, never()).save(any());
        // Notification still broadcasts at the end
        verify(notificationService, times(1)).broadcastMarketingUpdate("ADVERTISERS_REORDERED");
    }
}
