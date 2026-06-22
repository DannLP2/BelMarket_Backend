package com.productos.mari.domain.notification;
import com.productos.mari.domain.user.UserLinkedDeviceRepository;

import com.productos.mari.domain.notification.MassNotificationRequest;
import com.productos.mari.domain.notification.NotificationDTO;
import com.productos.mari.domain.notification.Notification;
import com.productos.mari.domain.notification.NotificationCategory;
import com.productos.mari.domain.notification.NotificationScope;
import com.productos.mari.domain.user.User;
import com.productos.mari.domain.notification.NotificationRepository;
import com.productos.mari.domain.user.UserRepository;
import com.productos.mari.domain.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final com.productos.mari.domain.infrastructure.communication.EmailService emailService;
    private final com.productos.mari.domain.user.UserLinkedDeviceRepository linkedDeviceRepository;

    // Cache sessions to avoid N+1 DB queries during broadcast
    private final Map<String, SseSession> sessions = new ConcurrentHashMap<>();

    // Internal class for SSE Session
    private static class SseSession {
        private final SseEmitter emitter;
        private final Long userId;
        private final Set<com.productos.mari.domain.user.Role> roles;
        private final String email;

        public SseSession(SseEmitter emitter, Long userId, Set<com.productos.mari.domain.user.Role> roles, String email) {
            this.emitter = emitter;
            this.userId = userId;
            this.roles = roles;
            this.email = email;
        }

        public SseEmitter getEmitter() {
            return emitter;
        }

        public Long getUserId() {
            return userId;
        }

        public Set<com.productos.mari.domain.user.Role> getRoles() {
            return roles;
        }

        public String getEmail() {
            return email;
        }
    }

    @Override
    @Transactional
    public void createNotification(Long userId, String title, String description, String icon, String link,
            NotificationCategory category, boolean isAdminOnly) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setDescription(description);
        notification.setIcon(icon);
        notification.setCategory(category != null ? category : NotificationCategory.INFO);
        notification.setRead(false);
        notification.setLink(link);

        if (isAdminOnly) {
            notification.setUser(null);
            notification.setScope(NotificationScope.ADMIN_SHARED);
        } else {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
            notification.setUser(user);
            notification.setScope(NotificationScope.PERSONAL);
        }

        notification = notificationRepository.save(notification);
        dispatchNotification(notification);
    }

    @Override
    @Transactional
    public void createNotificationByEmail(String email, String title, String description, String icon, String link,
            NotificationCategory category, boolean isAdminOnly) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setDescription(description);
        notification.setIcon(icon);
        notification.setCategory(category != null ? category : NotificationCategory.INFO);
        notification.setRead(false);
        notification.setLink(link);

        if (isAdminOnly) {
            notification.setUser(null);
            notification.setScope(NotificationScope.ADMIN_SHARED);
        } else {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + email));
            notification.setUser(user);
            notification.setScope(NotificationScope.PERSONAL);
        }

        notification = notificationRepository.save(notification);
        dispatchNotification(notification);
    }

    @Override
    @Transactional
    public void broadcastNotification(String title, String description, String icon, String link,
            NotificationCategory category, boolean isAdminOnly) {
        broadcastNotificationToScope(title, description, icon, link, category,
                isAdminOnly ? NotificationScope.ADMIN_SHARED : NotificationScope.GLOBAL);
    }

    @Override
    @Transactional
    public void broadcastNotificationToScope(String title, String description, String icon, String link,
            NotificationCategory category, NotificationScope scope) {
        Notification n = new Notification();
        n.setTitle(title);
        n.setDescription(description);
        n.setIcon(icon);
        n.setCategory(category != null ? category : NotificationCategory.INFO);
        n.setRead(false);
        n.setLink(link);
        n.setUser(null);
        n.setScope(scope != null ? scope : NotificationScope.GLOBAL);

        notificationRepository.save(n);
        dispatchNotification(n);
    }

    @Override
    @Transactional
    public void sendMassNotification(MassNotificationRequest request) {
        NotificationCategory category = NotificationCategory.INFO;
        try {
            if (request.getCategory() != null) {
                category = NotificationCategory.valueOf(request.getCategory());
            }
        } catch (IllegalArgumentException e) {
            log.warn("Categoría inválida recibida: {}. Usando INFO.", request.getCategory());
        }
        final NotificationCategory finalCategory = category;

        String audience = request.getAudience() != null ? request.getAudience()
                : (request.isAdminOnly() ? "admins" : "global");

        if ("admins".equals(audience)) {
            broadcastShared(request, finalCategory, NotificationScope.ADMIN_SHARED,
                    u -> u.getRoles().contains(com.productos.mari.domain.user.Role.ADMIN));
        } else if ("clients".equals(audience)) {
            broadcastShared(request, finalCategory, NotificationScope.CLIENT_SHARED,
                    u -> u.getRoles().contains(com.productos.mari.domain.user.Role.CLIENT));
        } else if ("deliverers".equals(audience)) {
            broadcastShared(request, finalCategory, NotificationScope.DELIVERER_SHARED,
                    u -> u.getRoles().contains(com.productos.mari.domain.user.Role.DELIVERER));
        } else if ("specific".equals(audience) && request.getUserIds() != null && !request.getUserIds().isEmpty()) {
            List<User> users = userRepository.findAllById(request.getUserIds());
            List<Notification> notifications = users.stream().map(user -> {
                Notification n = new Notification();
                n.setUser(user);
                n.setTitle(request.getTitle());
                n.setDescription(request.getDescription());
                n.setIcon(request.getIcon());
                n.setCategory(finalCategory);
                n.setRead(false);
                n.setLink(request.getLink());
                n.setScope(NotificationScope.PERSONAL);
                return n;
            }).collect(Collectors.toList());
            notificationRepository.saveAll(notifications);
            notifications.forEach(this::dispatchNotification);
            if (request.isSendEmail()) {
                users.forEach(user -> {
                    if (user.getEmail() != null)
                        emailService.sendBroadcastEmail(user.getEmail(), request.getTitle(), request.getDescription());
                });
            }
        } else {
            Notification n = new Notification();
            n.setUser(null);
            n.setTitle(request.getTitle());
            n.setDescription(request.getDescription());
            n.setIcon(request.getIcon());
            n.setCategory(finalCategory);
            n.setRead(false);
            n.setLink(request.getLink());
            n.setScope(NotificationScope.GLOBAL);
            notificationRepository.save(n);
            dispatchNotification(n);
            if (request.isSendEmail()) {
                userRepository.findAll().forEach(u -> {
                    if (u.getEmail() != null)
                        emailService.sendBroadcastEmail(u.getEmail(), request.getTitle(), request.getDescription());
                });
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDTO> getUserNotifications(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        java.util.Set<NotificationScope> allowedScopes = new java.util.HashSet<>();
        allowedScopes.add(NotificationScope.GLOBAL);

        user.getRoles().forEach(role -> {
            switch (role) {
                case ADMIN, SUPER_ADMIN -> allowedScopes.add(NotificationScope.ADMIN_SHARED);
                case CLIENT -> allowedScopes.add(NotificationScope.CLIENT_SHARED);
                case DELIVERER -> allowedScopes.add(NotificationScope.DELIVERER_SHARED);
            }
        });

        return notificationRepository.findPersonalAndSharedNotifications(email, allowedScopes)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, String email) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notificación no encontrada"));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (notification.getScope() == NotificationScope.PERSONAL) {
            if (!notification.getUser().getEmail().equals(email)) {
                throw new IllegalArgumentException("No autorizado para marcar esta notificación");
            }
        } else if (notification.getScope() == NotificationScope.ADMIN_SHARED) {
            if (!user.getRoles().contains(com.productos.mari.domain.user.Role.ADMIN)) {
                throw new IllegalArgumentException("Solo administradores pueden gestionar esta notificación");
            }
        } else if (notification.getScope() == NotificationScope.DELIVERER_SHARED) {
            if (!user.getRoles().contains(com.productos.mari.domain.user.Role.DELIVERER) &&
                    !user.getRoles().contains(com.productos.mari.domain.user.Role.ADMIN)) {
                throw new IllegalArgumentException("Solo personal de reparto puede gestionar esta notificación");
            }
        }

        notification.setRead(true);
        notification.setLastReadBy(user);
        Notification saved = notificationRepository.save(notification);

        if (saved.getScope() == NotificationScope.ADMIN_SHARED ||
                saved.getScope() == NotificationScope.DELIVERER_SHARED ||
                saved.getScope() == NotificationScope.GLOBAL) {
            broadcastReadUpdate(saved.getId(), user.getName());
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(String email, Boolean isAdminOnly, String scope) {
        if ("DELIVERER_SHARED".equals(scope)) {
            notificationRepository.markAllDelivererSharedAsRead();
            broadcastReadAllUpdate(List.of(NotificationScope.DELIVERER_SHARED));
        } else if ("GLOBAL".equals(scope)) {
            notificationRepository.markAllGlobalAsRead();
            broadcastReadAllUpdate(List.of(NotificationScope.GLOBAL));
        } else if (Boolean.TRUE.equals(isAdminOnly)) {
            notificationRepository.markAllAdminSharedAsRead();
            broadcastReadAllUpdate(List.of(NotificationScope.ADMIN_SHARED));
        } else {
            notificationRepository.markAllPersonalAsRead(email);
        }
    }

    private void broadcastReadUpdate(Long id, String readerName) {
        String payload = String.format("{\"id\":%d, \"lastReadByName\":\"%s\", \"isRead\":true}", id, readerName);
        emitToScope(NotificationScope.ADMIN_SHARED, "READ_UPDATE", payload);
    }

    private void broadcastReadAllUpdate(List<NotificationScope> scopes) {
        String scopesJson = scopes.stream()
                .map(s -> "\"" + s.name() + "\"")
                .collect(Collectors.joining(", ", "[", "]"));
        String payload = String.format("{\"isAdminOnly\":%b, \"scopes\":%s}",
                scopes.contains(NotificationScope.ADMIN_SHARED), scopesJson);

        scopes.forEach(scope -> emitToScope(scope, "READ_ALL_UPDATE", payload));
    }

    private void emitToScope(NotificationScope scope, String eventName, Object data) {
        sessions.values().forEach(session -> {
            boolean shouldReceive = switch (scope) {
                case PERSONAL -> false;
                case ADMIN_SHARED -> session.getRoles().contains(com.productos.mari.domain.user.Role.ADMIN);
                case CLIENT_SHARED -> session.getRoles().contains(com.productos.mari.domain.user.Role.CLIENT);
                case DELIVERER_SHARED -> session.getRoles().contains(com.productos.mari.domain.user.Role.DELIVERER);
                case GLOBAL -> true;
            };
            if (shouldReceive) {
                emitToSession(session, eventName, data);
            }
        });
    }

    private void emitToSession(SseSession session, String eventName, Object data) {
        try {
            session.getEmitter().send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException e) {
            sessions.remove(session.getEmail());
        }
    }

    @Override
    public SseEmitter subscribe(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null)
            return null;

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        try {
            emitter.send(SseEmitter.event().name("INIT").data("Connected"));
        } catch (IOException e) {
            return null;
        }

        SseSession session = new SseSession(emitter, user.getId(), new java.util.HashSet<>(user.getRoles()), email);
        sessions.put(email, session);

        emitter.onCompletion(() -> sessions.remove(email));
        emitter.onTimeout(() -> sessions.remove(email));
        emitter.onError((ex) -> sessions.remove(email));
        return emitter;
    }

    @Transactional
    private void broadcastShared(MassNotificationRequest request, NotificationCategory category,
            NotificationScope scope, java.util.function.Predicate<User> roleFilter) {
        Notification n = new Notification();
        n.setUser(null);
        n.setTitle(request.getTitle());
        n.setDescription(request.getDescription());
        n.setIcon(request.getIcon());
        n.setCategory(category);
        n.setRead(false);
        n.setLink(request.getLink());
        n.setScope(scope);
        notificationRepository.save(n);
        dispatchNotification(n);
    }

    @Override
    public void notifyUserUpdate(Long userId) {
        sessions.values().stream()
                .filter(s -> s.getUserId().equals(userId))
                .forEach(s -> {
                    try {
                        // Send final signal and close connection
                        s.getEmitter().send(SseEmitter.event().name("USER_UPDATE").data("{\"userId\":" + userId + "}"));
                        s.getEmitter().complete();
                    } catch (Exception e) {
                        log.warn("Error closing SSE for updated user {}: {}", userId, e.getMessage());
                    } finally {
                        sessions.remove(s.getEmail());
                    }
                });
    }

    private void dispatchNotification(Notification n) {
        NotificationDTO dto = mapToDTO(n);
        switch (n.getScope()) {
            case PERSONAL -> emitNotification(n.getUser().getEmail(), dto);
            case ADMIN_SHARED -> emitToRole(dto, com.productos.mari.domain.user.Role.ADMIN);
            case CLIENT_SHARED -> emitToRole(dto, com.productos.mari.domain.user.Role.CLIENT);
            case DELIVERER_SHARED -> emitToRole(dto, com.productos.mari.domain.user.Role.DELIVERER);
            case GLOBAL -> broadcastToAll(dto);
        }
    }

    private void emitToRole(NotificationDTO dto, com.productos.mari.domain.user.Role role) {
        sessions.values().stream()
                .filter(s -> s.getRoles().contains(role))
                .forEach(s -> emitToSession(s, "NOTIFICATION", dto));
    }

    private void broadcastToAll(NotificationDTO dto) {
        sessions.values().forEach(s -> emitToSession(s, "NOTIFICATION", dto));
    }

    private void emitNotification(String email, NotificationDTO dto) {
        SseSession session = sessions.get(email);
        if (session != null) {
            emitToSession(session, "NOTIFICATION", dto);
        }
    }

    private NotificationDTO mapToDTO(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setTitle(notification.getTitle());
        dto.setDescription(notification.getDescription());
        dto.setIcon(notification.getIcon());
        dto.setCategory(notification.getCategory().name());
        dto.setRead(notification.isRead());
        dto.setCreatedAt(notification.getCreatedAt());
        dto.setLink(notification.getLink());
        dto.setAdminOnly(notification.isAdminOnly());
        dto.setScope(notification.getScope().name());
        if (notification.getLastReadBy() != null) {
            dto.setLastReadByName(notification.getLastReadBy().getName());
        }
        return dto;
    }

    @Override
    public void broadcastCatalogUpdate(String reason) {
        String payload = String.format("{\"reason\":\"%s\", \"ts\":%d}", reason, System.currentTimeMillis());
        emitToScope(NotificationScope.GLOBAL, "CATALOG_UPDATE", payload);
    }

    @Override
    public void broadcastReservationUpdate(String reason) {
        String payload = String.format("{\"reason\":\"%s\", \"ts\":%d}", reason, System.currentTimeMillis());
        emitToScope(NotificationScope.ADMIN_SHARED, "RESERVATION_UPDATE", payload);
        emitToScope(NotificationScope.DELIVERER_SHARED, "RESERVATION_UPDATE", payload);
    }

    @Override
    public void broadcastSupportUpdate(String reason) {
        String payload = String.format("{\"reason\":\"%s\", \"ts\":%d}", reason, System.currentTimeMillis());
        emitToScope(NotificationScope.ADMIN_SHARED, "SUPPORT_UPDATE", payload);
    }

    @Override
    public void broadcastIotUpdate(Long productId, String reason) {
        // Secure 🛡️: Identify the owner of the device
        final Set<Long> authorizedUserIds = linkedDeviceRepository.findByProductId(productId).stream()
                .map(ld -> ld.getUser().getId())
                .collect(Collectors.toSet());

        String payload = String.format("{\"productId\":%d, \"reason\":\"%s\", \"ts\":%d}", productId, reason,
                System.currentTimeMillis());

        sessions.values().stream()
                .filter(s -> authorizedUserIds.contains(s.getUserId())
                        || s.getRoles().contains(com.productos.mari.domain.user.Role.ADMIN))
                .forEach(s -> emitToSession(s, "IOT_UPDATE", payload));
    }

    @Override
    public void broadcastDeviceListUpdate(Long userId) {
        String payload = String.format("{\"userId\":%d, \"ts\":%d}", userId, System.currentTimeMillis());
        sessions.values().stream()
                .filter(s -> s.getUserId().equals(userId))
                .forEach(s -> emitToSession(s, "DEVICE_LIST_UPDATE", payload));
    }

    @Override
    public void broadcastMarketingUpdate(String reason) {
        String payload = String.format("{\"reason\":\"%s\", \"ts\":%d}", reason, System.currentTimeMillis());
        emitToScope(NotificationScope.GLOBAL, "MARKETING_UPDATE", payload);
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 30000)
    public void heartbeat() {
        if (sessions.isEmpty())
            return;
        sessions.values().forEach(s -> {
            try {
                s.getEmitter().send(SseEmitter.event().name("PING").data(System.currentTimeMillis()));
            } catch (IOException e) {
                sessions.remove(s.getEmail());
            }
        });
    }
}
