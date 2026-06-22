package com.productos.mari.domain.settings;

import com.productos.mari.domain.infrastructure.audit.SecurityAuditService;
import com.productos.mari.domain.infrastructure.media.CloudinaryService;
import com.productos.mari.domain.infrastructure.media.FileUploadValidationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppSettingsServiceImplTest {

    @Mock private AppSettingsRepository repository;
    @Mock private CloudinaryService cloudinaryService;
    @Mock private FileUploadValidationService fileValidationService;
    @Mock private SecurityAuditService securityAuditService;

    @InjectMocks private AppSettingsServiceImpl settingsService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("admin@test.com", "password"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getSettings_shouldThrowIfNoneFound() {
        when(repository.findAll()).thenReturn(List.of());
        assertThrows(RuntimeException.class, () -> settingsService.getSettings());
    }

    @Test
    void getSettings_shouldReturnWithSyncedFileLimits() {
        AppSettings settings = AppSettings.builder().id(1L).storeName("BelMarket").build();
        when(repository.findAll()).thenReturn(List.of(settings));
        when(fileValidationService.getMaxImageSizeMb()).thenReturn(5);
        when(fileValidationService.getMaxPdfSizeMb()).thenReturn(10);

        AppSettings result = settingsService.getSettings();

        assertNotNull(result);
        assertEquals(5, result.getMaxImageSizeMb());
        assertEquals(10, result.getMaxPdfSizeMb());
    }

    @Test
    void updateSettings_shouldDeleteOldCloudinaryImageIfLogoChanged() throws Exception {
        String oldLogo = "https://res.cloudinary.com/test/image/upload/v1/old-logo.png";
        String newLogo = "https://res.cloudinary.com/test/image/upload/v2/new-logo.png";

        AppSettings current = AppSettings.builder().id(1L).logoUrl(oldLogo).build();
        AppSettings incoming = AppSettings.builder().logoUrl(newLogo).build();

        when(repository.findAll()).thenReturn(List.of(current));
        when(cloudinaryService.extractPublicId(oldLogo)).thenReturn("v1/old-logo");
        when(repository.save(current)).thenReturn(current);

        settingsService.updateSettings(incoming);

        verify(cloudinaryService, times(1)).extractPublicId(oldLogo);
        verify(cloudinaryService, times(1)).deleteFile("v1/old-logo");
        verify(securityAuditService, times(1)).log(any(), any(), anyString(), anyString());
    }

    @Test
    void updateSettings_shouldNotDeleteImageIfUrlUnchanged() throws Exception {
        String sameUrl = "https://res.cloudinary.com/test/image/upload/v1/logo.png";
        AppSettings current = AppSettings.builder().id(1L).logoUrl(sameUrl).build();
        AppSettings incoming = AppSettings.builder().logoUrl(sameUrl).build();

        when(repository.findAll()).thenReturn(List.of(current));
        when(repository.save(current)).thenReturn(current);

        settingsService.updateSettings(incoming);

        verify(cloudinaryService, never()).deleteFile(anyString());
    }
}
