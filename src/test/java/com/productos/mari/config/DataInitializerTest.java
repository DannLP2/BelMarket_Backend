package com.productos.mari.config;

import com.productos.mari.domain.notification.NotificationCategory;
import com.productos.mari.domain.notification.NotificationService;
import com.productos.mari.domain.settings.AppSettings;
import com.productos.mari.domain.settings.AppSettingsRepository;
import com.productos.mari.domain.user.Role;
import com.productos.mari.domain.user.User;
import com.productos.mari.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AppSettingsRepository appSettingsRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private DataInitializer dataInitializer;

    private final String ADMIN_EMAIL = "admin@test.com";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(dataInitializer, "adminEmail", ADMIN_EMAIL);
        ReflectionTestUtils.setField(dataInitializer, "adminPassword", "secret");
    }

    @Test
    void shouldCreateAdminUserIfNotExist() {
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedSecret");

        User savedUserMock = User.builder().id(99L).email(ADMIN_EMAIL).build();
        when(userRepository.save(any(User.class))).thenReturn(savedUserMock);

        // For app settings part to pass quietly
        when(appSettingsRepository.findAll()).thenReturn(List.of(new AppSettings()));

        dataInitializer.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertEquals(ADMIN_EMAIL, capturedUser.getEmail());
        assertEquals("encodedSecret", capturedUser.getPassword());
        assertTrue(capturedUser.getRoles().containsAll(List.of(Role.ADMIN, Role.SUPER_ADMIN, Role.CLIENT, Role.DELIVERER)));

        verify(notificationService).createNotification(
                eq(99L),
                contains("Bienvenido"),
                anyString(),
                anyString(),
                anyString(),
                eq(NotificationCategory.SUCCESS),
                eq(true)
        );
    }

    @Test
    void shouldPatchAdminRolesIfPartial() {
        User existingAdmin = User.builder()
                .id(1L)
                .email(ADMIN_EMAIL)
                .roles(new HashSet<>(List.of(Role.ADMIN))) // Only has one role
                .build();

        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(existingAdmin));

        // For app settings part to pass quietly
        when(appSettingsRepository.findAll()).thenReturn(List.of(new AppSettings()));

        dataInitializer.run();

        // Needs to have updated the user
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User updatedUser = userCaptor.getValue();
        assertEquals(4, updatedUser.getRoles().size());
        assertTrue(updatedUser.getRoles().contains(Role.SUPER_ADMIN));
    }

    @Test
    void shouldCreateAppSettingsIfEmpty() {
        // Setup user to skip admin logic
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(
            User.builder().roles(new HashSet<>(List.of(Role.ADMIN, Role.SUPER_ADMIN, Role.CLIENT, Role.DELIVERER))).build()
        ));

        // Return empty list so it falls back to default builder
        when(appSettingsRepository.findAll()).thenReturn(Collections.emptyList());

        dataInitializer.run();

        // Verify the entire new default config was saved
        ArgumentCaptor<AppSettings> settingsCaptor = ArgumentCaptor.forClass(AppSettings.class);
        verify(appSettingsRepository).save(settingsCaptor.capture());
        
        AppSettings savedSettings = settingsCaptor.getValue();
        assertEquals("BelMarket", savedSettings.getStoreName());
        assertEquals("BelMarket | Tu Tienda de Belleza Favorita", savedSettings.getMetaTitle());
        assertNotNull(savedSettings.getStoreLatitude());
        assertNotNull(savedSettings.getStoreLongitude());
    }

    @Test
    void shouldPatchAppSettingsIfOutdated() {
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(
                User.builder().roles(new HashSet<>(List.of(Role.ADMIN, Role.SUPER_ADMIN, Role.CLIENT, Role.DELIVERER))).build()
        ));

        // Create outdated app settings (missing meta title, bad coordinates)
        AppSettings outdatedSettings = new AppSettings();
        outdatedSettings.setId(10L); 
        outdatedSettings.setStoreLatitude(0.0); // Invalid zero coords

        when(appSettingsRepository.findAll()).thenReturn(List.of(outdatedSettings));

        dataInitializer.run();

        // Verify patching updated the object
        ArgumentCaptor<AppSettings> settingsCaptor = ArgumentCaptor.forClass(AppSettings.class);
        verify(appSettingsRepository).save(settingsCaptor.capture());

        AppSettings patchedSettings = settingsCaptor.getValue();
        assertEquals(10L, patchedSettings.getId());
        assertEquals("BelMarket | Tu Tienda de Belleza Favorita", patchedSettings.getMetaTitle()); // Patched
        assertEquals("BelMarket", patchedSettings.getStoreName()); // Patched
        assertTrue(patchedSettings.getStoreLatitude() > 0); // Coordinates patched
    }
}
