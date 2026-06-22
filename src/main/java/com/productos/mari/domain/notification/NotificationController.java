package com.productos.mari.domain.notification;

import com.productos.mari.domain.notification.MassNotificationRequest;
import com.productos.mari.domain.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.List;
import java.security.Principal;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<com.productos.mari.domain.notification.NotificationDTO>> getUserNotifications(Principal principal) {
        return ResponseEntity.ok(notificationService.getUserNotifications(principal.getName()));
    }

    @GetMapping("/subscribe")
    public SseEmitter subscribe(Principal principal) {
        return notificationService.subscribe(principal.getName());
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, Principal principal) {
        notificationService.markAsRead(id, principal.getName());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@RequestParam(required = false) Boolean isAdminOnly, @RequestParam(required = false) String scope, Principal principal) {
        notificationService.markAllAsRead(principal.getName(), isAdminOnly, scope);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin/mass")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sendMassNotification(@RequestBody MassNotificationRequest request) {
        notificationService.sendMassNotification(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cart-reminder")
    public ResponseEntity<Void> sendCartReminder(@RequestParam(required = false) String productName, Principal principal) {
        String msg = String.format("Aún tienes %s esperando en tu carrito. ¡No pierdas tu reserva!", productName != null ? productName : "productos");
        notificationService.createNotificationByEmail(principal.getName(), "Recordatorio de Carrito", msg, "shopping_cart", "/cart", com.productos.mari.domain.notification.NotificationCategory.INFO, false);
        return ResponseEntity.ok().build();
    }
}
