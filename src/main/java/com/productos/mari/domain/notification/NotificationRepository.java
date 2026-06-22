package com.productos.mari.domain.notification;

import com.productos.mari.domain.notification.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserEmailOrderByCreatedAtDesc(String email);

    @Query("SELECT n FROM Notification n LEFT JOIN n.user u WHERE " +
           "(u.email = :email AND n.scope = com.productos.mari.domain.notification.NotificationScope.PERSONAL) OR " +
           "(n.scope IN :scopes) " +
           "ORDER BY n.createdAt DESC")
    java.util.List<Notification> findPersonalAndSharedNotifications(String email, java.util.Collection<com.productos.mari.domain.notification.NotificationScope> scopes);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.email = :email AND n.isRead = false AND n.scope = com.productos.mari.domain.notification.NotificationScope.PERSONAL")
    int markAllPersonalAsRead(String email);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.isRead = false AND n.scope = com.productos.mari.domain.notification.NotificationScope.ADMIN_SHARED")
    int markAllAdminSharedAsRead();

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.isRead = false AND n.scope = com.productos.mari.domain.notification.NotificationScope.DELIVERER_SHARED")
    int markAllDelivererSharedAsRead();

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.isRead = false AND n.scope = com.productos.mari.domain.notification.NotificationScope.GLOBAL")
    int markAllGlobalAsRead();

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :timestamp")
    int deleteByCreatedAtBefore(LocalDateTime timestamp);
}
