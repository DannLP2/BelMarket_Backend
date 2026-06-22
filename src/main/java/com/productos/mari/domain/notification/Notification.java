package com.productos.mari.domain.notification;
import com.productos.mari.domain.user.User;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false)
    private String icon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationCategory category = NotificationCategory.INFO;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(length = 255)
    private String link;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationScope scope = NotificationScope.PERSONAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_read_by_id")
    private User lastReadBy;

    // Temporal bridge for DTO compatibility
    public boolean isAdminOnly() {
        return this.scope == NotificationScope.ADMIN_SHARED;
    }
}
