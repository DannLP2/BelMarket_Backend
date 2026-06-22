package com.productos.mari.domain.auth;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SecurityLogTest {

    @Test
    void onCreate_SetsTimestampWhenNull() {
        SecurityLog log = SecurityLog.builder()
                .email("attacker@evil.com")
                .ipAddress("192.168.1.1")
                .action(SecurityAction.LOGIN_FAILED)
                .details("3 failed attempts")
                .build();

        assertNull(log.getTimestamp());

        // Simulate JPA @PrePersist lifecycle call
        log.onCreate();

        assertNotNull(log.getTimestamp());
        assertTrue(log.getTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void onCreate_DoesNotOverwriteExistingTimestamp() {
        LocalDateTime fixedTime = LocalDateTime.of(2024, 1, 15, 10, 0, 0);
        SecurityLog log = SecurityLog.builder()
                .email("user@test.com")
                .action(SecurityAction.LOGIN_SUCCESS)
                .timestamp(fixedTime)
                .build();

        log.onCreate();

        // Timestamp must remain the original — not overwritten
        assertEquals(fixedTime, log.getTimestamp());
    }

    @Test
    void builder_CreatesLogWithAllFields() {
        LocalDateTime now = LocalDateTime.now();
        SecurityLog log = SecurityLog.builder()
                .id(1L)
                .email("admin@test.com")
                .ipAddress("10.0.0.1")
                .action(SecurityAction.RATE_LIMIT_EXCEEDED)
                .details("Too many requests")
                .userAgent("Mozilla/5.0")
                .timestamp(now)
                .build();

        assertEquals(1L, log.getId());
        assertEquals("admin@test.com", log.getEmail());
        assertEquals("10.0.0.1", log.getIpAddress());
        assertEquals(SecurityAction.RATE_LIMIT_EXCEEDED, log.getAction());
        assertEquals("Too many requests", log.getDetails());
        assertEquals("Mozilla/5.0", log.getUserAgent());
        assertEquals(now, log.getTimestamp());
    }
}
