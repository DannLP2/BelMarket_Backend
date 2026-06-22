package com.productos.mari.domain.notification;

import com.productos.mari.domain.user.Role;
import com.productos.mari.domain.user.User;
import com.productos.mari.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationSseIT {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private String adminEmail = "sse-admin@test.com";
    private String clientEmail = "sse-client@test.com";

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(User.builder()
                .name("Admin")
                .email(adminEmail)
                .password("pass")
                .roles(Set.of(Role.ADMIN))
                .build());

        userRepository.save(User.builder()
                .name("Client")
                .email(clientEmail)
                .password("pass")
                .roles(Set.of(Role.CLIENT))
                .build());
    }

    @Test
    void subscribe_ShouldReturnEmitter() {
        SseEmitter emitter = notificationService.subscribe(adminEmail);
        assertNotNull(emitter);
    }

    @Test
    void subscribe_NonExistentUser_ShouldReturnNull() {
        SseEmitter emitter = notificationService.subscribe("none@test.com");
        assertNull(emitter);
    }

    @Test
    void broadcastNotification_ShouldProcessCorrectly() {
        // We can't easily wait for SSE events in a unit test without complex threading,
        // but we can verify the DB persistence and service logic.
        
        notificationService.broadcastNotification(
                "Global Msg", "Hello All", "info-icon", "/home", 
                NotificationCategory.INFO, false
        );

        long count = notificationRepository.findAll().stream()
                .filter(n -> n.getScope() == NotificationScope.GLOBAL)
                .count();
        
        assertEquals(1, count);
    }

    @Test
    void createNotification_Personal_ShouldPersistForUser() {
        User client = userRepository.findByEmail(clientEmail).orElseThrow();
        
        notificationService.createNotification(
                client.getId(), "Personal Msg", "Only For You", "default-icon", null, 
                NotificationCategory.WARNING, false
        );

        long count = notificationRepository.findAll().stream()
                .filter(n -> n.getScope() == NotificationScope.PERSONAL && n.getUser().getId().equals(client.getId()))
                .count();
        
        assertEquals(1, count);
    }

    @Test
    void markAsRead_AdminShared_ShouldUpdateProperly() {
        notificationService.broadcastNotification(
                "Admin Alert", "System check", "default-icon", null, 
                NotificationCategory.INFO, true
        );
        
        Notification note = notificationRepository.findAll().stream()
                .filter(n -> n.getScope() == NotificationScope.ADMIN_SHARED)
                .findFirst().orElseThrow();
        
        notificationService.markAsRead(note.getId(), adminEmail);
        
        Notification updated = notificationRepository.findById(note.getId()).orElseThrow();
        assertTrue(updated.isRead());
        assertEquals("Admin", updated.getLastReadBy().getName());
    }
}
