package com.productos.mari.domain.notification;

import com.productos.mari.domain.notification.NotificationDTO;
import com.productos.mari.domain.notification.MassNotificationRequest;
import com.productos.mari.domain.notification.NotificationCategory;
import com.productos.mari.domain.notification.NotificationScope;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.List;

public interface NotificationService {
    void createNotification(Long userId, String title, String description, String icon, String link, NotificationCategory category, boolean isAdminOnly);
    void createNotificationByEmail(String email, String title, String description, String icon, String link, NotificationCategory category, boolean isAdminOnly);
    void broadcastNotification(String title, String description, String icon, String link, NotificationCategory category, boolean isAdminOnly);
    void broadcastNotificationToScope(String title, String description, String icon, String link, NotificationCategory category, NotificationScope scope);
    void sendMassNotification(MassNotificationRequest request);
    List<NotificationDTO> getUserNotifications(String email);
    void markAsRead(Long notificationId, String email);
    void markAllAsRead(String email, Boolean isAdminOnly, String scope);
    SseEmitter subscribe(String email);
    void notifyUserUpdate(Long userId);
    void broadcastCatalogUpdate(String reason);
    void broadcastReservationUpdate(String reason);
    void broadcastSupportUpdate(String reason);
    void broadcastIotUpdate(Long productId, String reason);
    void broadcastDeviceListUpdate(Long userId);
    void broadcastMarketingUpdate(String reason);
}
