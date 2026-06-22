package com.productos.mari.domain.reservation.util;

import com.productos.mari.domain.auth.SecurityAction;
import com.productos.mari.domain.auth.SecurityLog;
import com.productos.mari.domain.auth.SecurityLogRepository;
import com.productos.mari.domain.marketing.OfferService;
import com.productos.mari.domain.notification.NotificationCategory;
import com.productos.mari.domain.notification.NotificationService;
import com.productos.mari.domain.product.Product;
import com.productos.mari.domain.reservation.Reservation;
import com.productos.mari.domain.reservation.ReservationItemDto;
import com.productos.mari.domain.reservation.ReservationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReservationValidator {

    private final SecurityLogRepository securityLogRepository;
    private final NotificationService notificationService;
    private final OfferService offerService;

    public void validatePrice(Product product, ReservationItemDto itemDto, String email) {
        // SERVER-SIDE PRICE VALIDATION
        BigDecimal calculatedPrice = offerService.calculateFinalPriceForProduct(product, itemDto.getQuantity());

        // SECURITY CHECK: Fraud detection
        if (itemDto.getPrice() != null && itemDto.getPrice().setScale(0, java.math.RoundingMode.HALF_UP)
                .compareTo(calculatedPrice.setScale(0, java.math.RoundingMode.HALF_UP)) != 0) {
            
            // Log SECURITY ALERT silently to DB
            securityLogRepository.save(SecurityLog.builder()
                    .action(SecurityAction.PRICE_MANIPULATION_DETECTED)
                    .email(email)
                    .details(String.format("Precio manipulado en Producto %s: Web enviaba $%s, Real era $%s",
                            product.getId(), itemDto.getPrice(), calculatedPrice))
                    .userAgent("System Auditor")
                    .build());

            // Alert Admin via notification
            notificationService.broadcastNotification(
                    "ALERTA DE SEGURIDAD: Fraude de Precio",
                    "Se detectó un intento de pago con precio alterado por el usuario: " + email,
                    "report_problem",
                    "/admin/security-logs",
                    NotificationCategory.SECURITY,
                    true);

            throw new IllegalArgumentException("PRECIO_DESACTUALIZADO");
        }
    }

    public void validateStatusForEdit(Reservation reservation) {
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("Solo se pueden editar reservas pendientes");
        }
    }

    public void validateOwnership(Reservation reservation, String email) {
        if (!reservation.getUser().getEmail().equals(email)) {
            throw new IllegalArgumentException("No autorizado");
        }
    }
}
