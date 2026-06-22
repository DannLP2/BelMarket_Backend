package com.productos.mari.domain.reservation.util;

import com.productos.mari.domain.reservation.Reservation;
import com.productos.mari.domain.reservation.ReservationRepository;
import com.productos.mari.domain.reservation.ReservationStatus;
import com.productos.mari.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationPurgeService {

    private final ReservationRepository reservationRepository;
    private final ProductService productService;
    private final ReservationNotificationDispatcher notificationDispatcher;

    /**
     * Tarea Programada: Cancela automáticamente reservas PENDING de más de 24 horas
     * Se ejecuta cada hora (3600000 ms)
     */
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void autoPurgeExpiredReservations() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        List<Reservation> expired = reservationRepository.findByStatus(ReservationStatus.PENDING).stream()
                .filter(r -> r.getCreatedAt().isBefore(threshold))
                .collect(Collectors.toList());

        for (Reservation r : expired) {
            log.info("Auto-Purge: Cancelling reservation #{} due to 24h timeout.", r.getReference());
            
            // Restore stock
            for (com.productos.mari.domain.reservation.ReservationItem item : r.getItems()) {
                productService.incrementStock(item.getProduct().getId(), item.getQuantity());
            }
            
            r.setStatus(ReservationStatus.CANCELLED);
            reservationRepository.save(r);

            // Notify via Dispatcher
            notificationDispatcher.dispatchStatusUpdate(r, ReservationStatus.CANCELLED);
        }
    }
}
