package com.productos.mari.domain.reservation.util;

import com.productos.mari.domain.infrastructure.communication.EmailService;
import com.productos.mari.domain.notification.NotificationCategory;
import com.productos.mari.domain.notification.NotificationScope;
import com.productos.mari.domain.notification.NotificationService;
import com.productos.mari.domain.reservation.Reservation;
import com.productos.mari.domain.reservation.ReservationDto;
import com.productos.mari.domain.reservation.ReservationStatus;
import com.productos.mari.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservationNotificationDispatcher {

    private final NotificationService notificationService;
    private final EmailService emailService;

    public void dispatchOrderCreated(Reservation reservation, ReservationDto dto, User user, byte[] pdfReceipt) {
        // 1. Notificación al Usuario
        notificationService.createNotification(
                user.getId(),
                "¡Reserva Registrada!",
                "Tu pedido #" + reservation.getReference() + " está en proceso satisfactoriamente.",
                "shopping_bag",
                "/my-reservations?ref=" + reservation.getReference(),
                NotificationCategory.SUCCESS,
                false);

        // 2. Notificación al Administrador
        notificationService.broadcastNotificationToScope(
                "NUEVO PEDIDO: #" + reservation.getReference(),
                "El usuario " + user.getName() + " ha realizado una nueva reserva por "
                        + String.format("%,.0f", dto.getTotal()) + " " + dto.getDisplayCurrency(),
                "receipt_long",
                "/admin/reservations?ref=" + reservation.getReference(),
                NotificationCategory.ORDER,
                NotificationScope.ADMIN_SHARED);

        // 3. Email con PDF
        emailService.sendReservationConfirmation(user.getEmail(), user.getName(), dto, pdfReceipt);

        notificationService.broadcastReservationUpdate("NEW_ORDER:" + reservation.getReference());
    }

    public void dispatchStatusUpdate(Reservation reservation, ReservationStatus status) {
        if (status == ReservationStatus.PENDING) return;

        String esStatus = getStatusLabel(status);
        String icon = getStatusIcon(status);

        String customMessage = "Tu reserva ha cambiado a estado: " + esStatus;
        if (status == ReservationStatus.READY_FOR_DELIVERY) {
            customMessage = "✅ ¡Tu pedido está listo y será entregado pronto! Seguimos en preparación final.";
        } else if (status == ReservationStatus.SHIPPED && reservation.getDeliveryCode() != null) {
            customMessage = "🚀 ¡Tu pedido va en camino! Código de seguridad para entrega: " + reservation.getDeliveryCode();
        } else if (status == ReservationStatus.READY_FOR_PICKUP && reservation.getDeliveryCode() != null) {
            customMessage = "🏪 ¡Pedido listo para recoger! Tu código de retiro es: " + reservation.getDeliveryCode();
        }

        notificationService.createNotification(
                reservation.getUser().getId(),
                "Actualización de Pedido #" + reservation.getReference(),
                customMessage,
                icon,
                "/my-reservations?ref=" + reservation.getReference(),
                (status == ReservationStatus.COMPLETED || status == ReservationStatus.CONFIRMED)
                        ? NotificationCategory.SUCCESS
                        : NotificationCategory.INFO,
                false);

        dispatchScopeNotifications(reservation, status);

        // Email update
        emailService.sendReservationStatusUpdate(
                reservation.getUser().getEmail(),
                reservation.getUser().getName(),
                reservation.getReference() != null ? reservation.getReference() : reservation.getId().toString(),
                status,
                reservation.getDeliveryCode());

        notificationService.broadcastReservationUpdate("STATUS_CHANGED:" + reservation.getReference() + ":" + status);
    }

    private void dispatchScopeNotifications(Reservation reservation, ReservationStatus status) {
        if (status == ReservationStatus.READY_FOR_DELIVERY) {
            notificationService.broadcastNotificationToScope(
                    "📦 Pedido Listo para Despachar",
                    "El pedido #" + reservation.getReference() + " de " + reservation.getUser().getName() + " está listo.",
                    "inventory_2",
                    "/delivery/dashboard",
                    NotificationCategory.WARNING,
                    NotificationScope.DELIVERER_SHARED);
        } else if (status == ReservationStatus.SHIPPED) {
            notificationService.broadcastNotificationToScope(
                    "🚚 Pedido en Ruta",
                    "El pedido #" + reservation.getReference() + " ya salió a entrega.",
                    "local_shipping",
                    "/delivery/dashboard",
                    NotificationCategory.INFO,
                    NotificationScope.DELIVERER_SHARED);
        } else if (status == ReservationStatus.CANCELLED) {
            notificationService.broadcastNotificationToScope(
                    "❌ Pedido Cancelado",
                    "El pedido #" + reservation.getReference() + " ha sido cancelado.",
                    "cancel",
                    "/delivery/dashboard",
                    NotificationCategory.ERROR,
                    NotificationScope.DELIVERER_SHARED);
        }
    }

    private String getStatusLabel(ReservationStatus status) {
        return switch (status) {
            case CONFIRMED -> "CONFIRMADO";
            case PREPARING, READY_FOR_DELIVERY -> "EN PREPARACIÓN";
            case SHIPPED -> "EN CAMINO";
            case READY_FOR_PICKUP -> "LISTO PARA RECOGER";
            case COMPLETED -> "COMPLETADO";
            case CANCELLED -> "CANCELADO";
            default -> status.name();
        };
    }

    private String getStatusIcon(ReservationStatus status) {
        return switch (status) {
            case CONFIRMED -> "check_circle";
            case PREPARING, READY_FOR_DELIVERY -> "inventory_2";
            case SHIPPED -> "local_shipping";
            case READY_FOR_PICKUP -> "storefront";
            case COMPLETED -> "done_all";
            case CANCELLED -> "cancel";
            default -> "info";
        };
    }
}
