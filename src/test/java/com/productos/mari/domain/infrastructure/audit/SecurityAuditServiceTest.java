package com.productos.mari.domain.infrastructure.audit;

import com.productos.mari.domain.auth.SecurityAction;
import com.productos.mari.domain.auth.SecurityLog;
import com.productos.mari.domain.auth.SecurityLogRepository;
import com.productos.mari.domain.notification.NotificationService;
import com.productos.mari.domain.notification.NotificationCategory;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SecurityAuditServiceTest {

    @Mock
    private SecurityLogRepository securityLogRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SecurityAuditService securityAuditService;

    @BeforeEach
    void setUp() {
        // Clear RequestContext before each test
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void log_SavesSecurityLog() {
        securityAuditService.log(SecurityAction.LOGIN_SUCCESS, "1.2.3.4", "user@test.com", "Success");

        verify(securityLogRepository).save(any(SecurityLog.class));
    }

    @Test
    void log_ExtractsIpFromRequestAttributes() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("5.6.7.8");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        
        ServletRequestAttributes attributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attributes);

        securityAuditService.log(SecurityAction.LOGIN_SUCCESS, null, "user@test.com", "Success");

        verify(securityLogRepository).save(argThat(log -> 
            "5.6.7.8".equals(log.getIpAddress()) && "Mozilla/5.0".equals(log.getUserAgent())
        ));
    }

    @Test
    void log_AlertsOnBruteForce() {
        when(securityLogRepository.countByIpAndActionAfter(anyString(), eq(SecurityAction.LOGIN_FAILED), any(LocalDateTime.class)))
                .thenReturn(5L);

        securityAuditService.log(SecurityAction.LOGIN_FAILED, "1.1.1.1", "hacker@test.com", "Failed");

        verify(notificationService).broadcastNotificationToScope(
                contains("Fuerza Bruta"), anyString(), anyString(), anyString(), eq(NotificationCategory.SECURITY), any()
        );
    }

    @Test
    void log_AlertsOnRateLimit() {
        when(securityLogRepository.countByIpAndActionAfter(anyString(), eq(SecurityAction.RATE_LIMIT_EXCEEDED), any(LocalDateTime.class)))
                .thenReturn(3L);

        securityAuditService.log(SecurityAction.RATE_LIMIT_EXCEEDED, "2.2.2.2", null, "Saturated");

        verify(notificationService).broadcastNotificationToScope(
                contains("Spam/DDoS"), anyString(), anyString(), anyString(), eq(NotificationCategory.SECURITY), any()
        );
    }

    @Test
    void purgeOldLogs_CallsRepository() {
        securityAuditService.purgeOldLogs();
        verify(securityLogRepository).deleteByTimestampBefore(any(LocalDateTime.class));
    }
}
