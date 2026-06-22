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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BannerServiceImplTest {

    @Mock
    private BannerRepository bannerRepository;

    @Mock
    private BannerMetricRepository metricRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private SecurityAuditService securityAuditService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BannerServiceImpl bannerService;

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
    void getActiveBanners_shouldReturnMappedDtos() {
        // Arrange
        Banner banner = Banner.builder()
                .id(1L)
                .title("Test Banner")
                .buttonText("VER MÁS")
                .active(true)
                .placement(BannerPlacement.HOME_CAROUSEL)
                .viewsCount(0L)
                .clicksCount(0L)
                .build();
        
        when(bannerRepository.findActiveByPlacementAndDate(eq(BannerPlacement.HOME_CAROUSEL), any(LocalDateTime.class)))
                .thenReturn(List.of(banner));

        // Act
        List<BannerDto> result = bannerService.getActiveBanners();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Banner", result.get(0).getTitle());
        verify(bannerRepository, times(1)).findActiveByPlacementAndDate(any(), any());
    }

    @Test
    void createBanner_shouldSetDefaultButtonTextAndLogAudit() {
        // Arrange
        BannerDto dto = BannerDto.builder()
                .title("New Banner")
                .buttonText("") // Empty so it should use default
                .active(true)
                .build();
                
        Banner savedBanner = Banner.builder()
                .id(1L)
                .title("New Banner")
                .buttonText("VER MÁS")
                .active(true)
                .viewsCount(0L)
                .clicksCount(0L)
                .build();

        when(bannerRepository.save(any(Banner.class))).thenReturn(savedBanner);

        // Act
        BannerDto result = bannerService.createBanner(dto);

        // Assert
        assertEquals("VER MÁS", result.getButtonText());
        verify(bannerRepository, times(1)).save(any(Banner.class));
        verify(securityAuditService, times(1)).log(any(), any(), eq("admin@test.com"), anyString());
        verify(notificationService, times(1)).broadcastMarketingUpdate("BANNER_CREATED");
    }

    @Test
    void recordMetric_oughtToIncrementCountAndSaveHistoricalMetric() {
        // Arrange
        Long bannerId = 1L;
        String ipAddress = "192.168.1.1";
        
        Banner proxyBanner = new Banner();
        proxyBanner.setId(bannerId);
        
        when(bannerRepository.getReferenceById(bannerId)).thenReturn(proxyBanner);

        // Act
        bannerService.recordMetric(bannerId, AdMetricType.IMPRESSION, ipAddress);

        // Assert
        verify(bannerRepository, times(1)).incrementViewsCount(bannerId);
        verify(bannerRepository, never()).incrementClicksCount(anyLong());
        verify(metricRepository, times(1)).save(argThat(metric -> 
            metric.getType() == AdMetricType.IMPRESSION &&
            metric.getIpAddress().equals(ipAddress) &&
            metric.getBanner().getId().equals(bannerId)
        ));
    }
    
    @Test
    void deleteBanner_shouldDeleteImageAndEntity() throws Exception {
        // Arrange
        Long bannerId = 1L;
        String imageUrl = "https://res.cloudinary.com/belmarket/image/upload/v1234/banner.png";
        Banner banner = Banner.builder()
                .id(bannerId)
                .title("Delete Me")
                .imageUrl(imageUrl)
                .build();

        when(bannerRepository.findById(bannerId)).thenReturn(Optional.of(banner));
        when(cloudinaryService.extractPublicId(imageUrl)).thenReturn("v1234/banner");

        // Act
        bannerService.deleteBanner(bannerId);

        // Assert
        verify(cloudinaryService, times(1)).deleteFile("v1234/banner");
        verify(bannerRepository, times(1)).delete(banner);
        verify(securityAuditService, times(1)).log(any(), any(), anyString(), anyString());
        verify(notificationService, times(1)).broadcastMarketingUpdate("BANNER_DELETED");
    }

    @Test
    void getAllBanners_shouldReturnAllMappedDtos() {
        // Arrange
        Banner banner = Banner.builder()
                .id(1L)
                .title("Any Banner")
                .build();
        when(bannerRepository.findAllByOrderByBannerOrderAsc()).thenReturn(List.of(banner));

        // Act
        List<BannerDto> result = bannerService.getAllBanners();

        // Assert
        assertEquals(1, result.size());
        verify(bannerRepository, times(1)).findAllByOrderByBannerOrderAsc();
    }

    @Test
    void updateBanner_shouldReplaceImageIfChanged() throws Exception {
        // Arrange
        Long id = 1L;
        String oldUrl = "https://res.cloudinary.com/bel/image/upload/v1/old.png";
        String newUrl = "https://res.cloudinary.com/bel/image/upload/v2/new.png";
        
        Banner currentBanner = Banner.builder()
                .id(id)
                .title("Old Title")
                .imageUrl(oldUrl)
                .build();
                
        BannerDto dto = BannerDto.builder()
                .title("New Title")
                .buttonText("CLICK ME")
                .imageUrl(newUrl)
                .build();

        when(bannerRepository.findById(id)).thenReturn(Optional.of(currentBanner));
        when(cloudinaryService.extractPublicId(oldUrl)).thenReturn("v1/old");
        when(bannerRepository.save(any(Banner.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        BannerDto result = bannerService.updateBanner(id, dto);

        // Assert
        assertEquals("New Title", result.getTitle());
        assertEquals(newUrl, result.getImageUrl());
        verify(cloudinaryService, times(1)).deleteFile("v1/old");
        verify(bannerRepository, times(1)).save(currentBanner);
        verify(securityAuditService, times(1)).log(any(), any(), anyString(), anyString());
        verify(notificationService, times(1)).broadcastMarketingUpdate("BANNER_UPDATED");
    }

    @Test
    void toggleBannerStatus_shouldSwitchActiveFlag() {
        // Arrange
        Long id = 1L;
        Banner banner = Banner.builder()
                .id(id)
                .active(false)
                .build();
                
        when(bannerRepository.findById(id)).thenReturn(Optional.of(banner));

        // Act
        bannerService.toggleBannerStatus(id);

        // Assert
        assertTrue(banner.isActive());
        verify(bannerRepository, times(1)).save(banner);
        verify(notificationService, times(1)).broadcastMarketingUpdate("BANNER_STATUS_TOGGLED");
    }

    @Test
    void getActiveBannersByPlacement_shouldReturnForGivenPlacement() {
        Banner banner = Banner.builder()
                .id(2L)
                .title("Sidebar Banner")
                .placement(BannerPlacement.SIDEBAR)
                .viewsCount(0L)
                .clicksCount(0L)
                .build();
        when(bannerRepository.findActiveByPlacementAndDate(eq(BannerPlacement.SIDEBAR), any(LocalDateTime.class)))
                .thenReturn(List.of(banner));

        List<BannerDto> result = bannerService.getActiveBannersByPlacement(BannerPlacement.SIDEBAR);

        assertEquals(1, result.size());
        verify(bannerRepository, times(1)).findActiveByPlacementAndDate(eq(BannerPlacement.SIDEBAR), any());
    }

    @Test
    void deleteBanner_shouldDoNothingWhenNotFound() {
        when(bannerRepository.findById(99L)).thenReturn(Optional.empty());

        bannerService.deleteBanner(99L);

        verify(bannerRepository, never()).delete(any());
        verify(notificationService, never()).broadcastMarketingUpdate(anyString());
    }

    @Test
    void updateBanner_shouldNotDeleteImageIfUnchanged() throws Exception {
        Long id = 1L;
        String sameUrl = "https://res.cloudinary.com/bel/image/upload/v1/same.png";

        Banner currentBanner = Banner.builder()
                .id(id)
                .title("Old")
                .imageUrl(sameUrl)
                .build();

        BannerDto dto = BannerDto.builder()
                .title("New Title")
                .buttonText("GO")
                .imageUrl(sameUrl)  // Same URL — no delete
                .build();

        when(bannerRepository.findById(id)).thenReturn(Optional.of(currentBanner));
        when(bannerRepository.save(any(Banner.class))).thenAnswer(i -> i.getArguments()[0]);

        BannerDto result = bannerService.updateBanner(id, dto);

        assertEquals("New Title", result.getTitle());
        verify(cloudinaryService, never()).deleteFile(anyString());
    }

    @Test
    void recordMetric_Click_shouldIncrementClicksCount() {
        Long bannerId = 1L;
        String ip = "10.0.0.1";
        Banner proxy = new Banner();
        proxy.setId(bannerId);
        when(bannerRepository.getReferenceById(bannerId)).thenReturn(proxy);

        bannerService.recordMetric(bannerId, AdMetricType.CLICK, ip);

        verify(bannerRepository, times(1)).incrementClicksCount(bannerId);
        verify(bannerRepository, never()).incrementViewsCount(anyLong());
        verify(metricRepository, times(1)).save(any(BannerMetric.class));
    }

    @Test
    void createBanner_shouldNotOverrideButtonTextIfAlreadySet() {
        BannerDto dto = BannerDto.builder()
                .title("Custom Banner")
                .buttonText("CUSTOM TEXT")
                .active(true)
                .build();

        Banner savedBanner = Banner.builder()
                .id(1L)
                .title("Custom Banner")
                .buttonText("CUSTOM TEXT")
                .active(true)
                .viewsCount(0L)
                .clicksCount(0L)
                .build();

        when(bannerRepository.save(any(Banner.class))).thenReturn(savedBanner);

        BannerDto result = bannerService.createBanner(dto);

        assertEquals("CUSTOM TEXT", result.getButtonText());
    }

    @Test
    void deleteBanner_shouldHandleNonCloudinaryImageGracefully() throws Exception {
        Long bannerId = 1L;
        String nonCloudinaryUrl = "https://some-other-cdn.com/image.png";
        Banner banner = Banner.builder()
                .id(bannerId)
                .title("Non-Cloud Banner")
                .imageUrl(nonCloudinaryUrl)
                .build();

        when(bannerRepository.findById(bannerId)).thenReturn(Optional.of(banner));

        bannerService.deleteBanner(bannerId);

        // Should still delete from DB even if no cloudinary cleanup
        verify(cloudinaryService, never()).deleteFile(anyString());
        verify(bannerRepository, times(1)).delete(banner);
    }
}
