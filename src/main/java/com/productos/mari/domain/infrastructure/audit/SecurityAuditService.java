package com.productos.mari.domain.infrastructure.audit;
import com.productos.mari.domain.notification.NotificationService;

import com.productos.mari.domain.notification.NotificationCategory;
import com.productos.mari.domain.auth.SecurityAction;
import com.productos.mari.domain.auth.SecurityLog;
import com.productos.mari.domain.auth.SecurityLogRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Service
@lombok.extern.slf4j.Slf4j
public class SecurityAuditService {

    private final SecurityLogRepository securityLogRepository;
    private final NotificationService notificationService;

    public SecurityAuditService(SecurityLogRepository securityLogRepository, 
                                @org.springframework.context.annotation.Lazy NotificationService notificationService) {
        this.securityLogRepository = securityLogRepository;
        this.notificationService = notificationService;
    }

    @Async
    public void log(SecurityAction action, String ip, String email, String details) {
        String finalIp = ip;
        String userAgent = "Unknown";

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            
            // Si no se pasó IP, intentar obtenerla
            if (finalIp == null || finalIp.isEmpty() || "0.0.0.0".equals(finalIp)) {
                String xfHeader = request.getHeader("X-Forwarded-For");
                if (xfHeader == null || xfHeader.isEmpty()) {
                    finalIp = request.getRemoteAddr();
                } else {
                    finalIp = xfHeader.split(",")[0];
                }
            }

            // Capturar User Agent
            userAgent = request.getHeader("User-Agent");
        }

        SecurityLog log = SecurityLog.builder()
                .action(action)
                .ipAddress(finalIp)
                .email(email)
                .details(details)
                .userAgent(userAgent)
                .build();
        securityLogRepository.save(log);

        // --- DETECCIÓN DE ATAQUES (NUEVO) ---
        if (action == SecurityAction.LOGIN_FAILED) {
            checkAndAlertBruteForce(finalIp, email);
        } else if (action == SecurityAction.RATE_LIMIT_EXCEEDED) {
            checkAndAlertRateLimit(finalIp, details);
        } else if (action == SecurityAction.UNAUTHORIZED_ACCESS) {
            checkAndAlertUnauthorized(finalIp, details);
        }
    }

    private void checkAndAlertRateLimit(String ip, String details) {
        java.time.LocalDateTime fiveMinsAgo = java.time.LocalDateTime.now().minusMinutes(5);
        long violationsByIp = securityLogRepository.countByIpAndActionAfter(ip, SecurityAction.RATE_LIMIT_EXCEEDED, fiveMinsAgo);
        
        // Si ha excedido el límite más de 3 veces en 5 minutos, es un ataque/spam claro
        if (violationsByIp >= 3) {
            notifyAdmins(
                "ALERTA: Spam/DDoS Detectado",
                "IP " + ip + " está saturando el sistema. Log de: " + details,
                "shutter_speed",
                "/admin/security-logs",
                NotificationCategory.SECURITY
            );
        }
    }

    private void checkAndAlertUnauthorized(String ip, String details) {
        notifyAdmins(
            "ALERTA: Acceso No Autorizado",
            "IP " + ip + " intentó acceder a zona restringida: " + details,
            "door_front",
            "/admin/security-logs",
            NotificationCategory.SECURITY
        );
    }

    private void checkAndAlertBruteForce(String ip, String email) {
        java.time.LocalDateTime fifteenMinsAgo = java.time.LocalDateTime.now().minusMinutes(15);
        long failuresByIp = securityLogRepository.countByIpAndActionAfter(ip, SecurityAction.LOGIN_FAILED, fifteenMinsAgo);
        
        if (failuresByIp >= 5) {
            notifyAdmins(
                "ALERTA DE SEGURIDAD: Fuerza Bruta",
                "Se detectaron " + failuresByIp + " intentos fallidos desde la IP: " + ip + (email != null ? " (Usuario: " + email + ")" : ""),
                "security_update_warning",
                "/admin/security-logs",
                NotificationCategory.SECURITY
            );
        }
    }

    private void notifyAdmins(String title, String description, String icon, String link, NotificationCategory category) {
        notificationService.broadcastNotificationToScope(title, description, icon, link, category, com.productos.mari.domain.notification.NotificationScope.ADMIN_SHARED);
    }

    /**
     * Purga automática de logs con más de 90 días de antigüedad.
     * Se ejecuta todos los días a las 3:00 AM.
     */
    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 3 * * *")
    @org.springframework.transaction.annotation.Transactional
    public void purgeOldLogs() {
        java.time.LocalDateTime threshold = java.time.LocalDateTime.now().minusDays(90);
        securityLogRepository.deleteByTimestampBefore(threshold);
        log.info("Auditoría: Se han purgado los registros anteriores a 90 días.");
    }
}
