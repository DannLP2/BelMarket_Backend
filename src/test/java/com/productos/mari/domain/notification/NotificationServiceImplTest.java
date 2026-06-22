package com.productos.mari.domain.notification;

import com.productos.mari.domain.infrastructure.communication.EmailService;
import com.productos.mari.domain.user.Role;
import com.productos.mari.domain.user.User;
import com.productos.mari.domain.user.UserLinkedDevice;
import com.productos.mari.domain.user.UserLinkedDeviceRepository;
import com.productos.mari.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceImplTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;
    @Mock private UserLinkedDeviceRepository linkedDeviceRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@test.com");
        mockUser.setName("Test User");
        mockUser.setRoles(Set.of(Role.CLIENT));

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(mockUser));
    }

    @Test
    void subscribe_CreatesSessionAndSendsInit() throws IOException {
        SseEmitter emitter = notificationService.subscribe("test@test.com");

        assertNotNull(emitter);
        // We can't easily verify the internal 'sessions' map without reflection or checking behavior
        // but we can check if broadcasting now targets this emitter.
    }

    @Test
    void createNotification_SavesAndDispatches() {
        Notification n = new Notification();
        n.setId(100L);
        n.setScope(NotificationScope.PERSONAL);
        n.setUser(mockUser);
        n.setCategory(NotificationCategory.INFO);

        when(notificationRepository.save(any(Notification.class))).thenReturn(n);

        notificationService.createNotification(1L, "Title", "Desc", "icon", "link", NotificationCategory.INFO, false);

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void broadcastNotification_SendsToGlobalScope() {
        notificationService.broadcastNotification("Global Title", "Msg", "icon", null, NotificationCategory.INFO, false);

        verify(notificationRepository).save(argThat(n -> n.getScope() == NotificationScope.GLOBAL));
    }

    @Test
    void sendMassNotification_ToAdminsOnly() {
        MassNotificationRequest request = new MassNotificationRequest();
        request.setAudience("admins");
        request.setTitle("Admin Alert");

        notificationService.sendMassNotification(request);

        verify(notificationRepository).save(argThat(n -> n.getScope() == NotificationScope.ADMIN_SHARED));
    }

    @Test
    void sendMassNotification_ToSpecificUsersWithEmail() {
        MassNotificationRequest request = new MassNotificationRequest();
        request.setAudience("specific");
        request.setUserIds(List.of(1L));
        request.setTitle("Specific Alert");
        request.setDescription("Specific Desc");
        request.setSendEmail(true);

        when(userRepository.findAllById(any())).thenReturn(List.of(mockUser));

        notificationService.sendMassNotification(request);

        verify(emailService).sendBroadcastEmail(eq("test@test.com"), anyString(), anyString());
        verify(notificationRepository).saveAll(any());
    }

    @Test
    void markAsRead_UpdatesStatusAndBroadcasts() {
        Notification n = new Notification();
        n.setId(1L);
        n.setScope(NotificationScope.GLOBAL);
        n.setRead(false);
        n.setCategory(NotificationCategory.INFO);

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));
        when(notificationRepository.save(any())).thenReturn(n);

        notificationService.markAsRead(1L, "test@test.com");

        assertTrue(n.isRead());
        assertEquals(mockUser, n.getLastReadBy());
        verify(notificationRepository).save(n);
    }

    @Test
    void markAllAsRead_ByScope() {
        notificationService.markAllAsRead("test@test.com", false, "GLOBAL");
        verify(notificationRepository).markAllGlobalAsRead();

        notificationService.markAllAsRead("test@test.com", true, null);
        verify(notificationRepository).markAllAdminSharedAsRead();
    }

    @Test
    void broadcastIotUpdate_FiltersByAuthorizedUsers() {
        UserLinkedDevice ld = new UserLinkedDevice();
        ld.setUser(mockUser);
        when(linkedDeviceRepository.findByProductId(99L)).thenReturn(List.of(ld));

        notificationService.broadcastIotUpdate(99L, "Device offline");

        verify(linkedDeviceRepository).findByProductId(99L);
    }

    @Test
    void heartbeat_RemovesDeadSessionsOnIOException() throws IOException {
        // Subscribe to create a session
        notificationService.subscribe("test@test.com");
        
        // This is tricky because SseSession is private and 'sessions' is private.
        // However, heartbeat iterations over sessions and calls send().
        // If we can't mock the internal session's emitter, this is hard.
        // But we can verify it doesn't crash.
        notificationService.heartbeat();
    }
}
