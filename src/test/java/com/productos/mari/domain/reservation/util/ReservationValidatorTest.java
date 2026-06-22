package com.productos.mari.domain.reservation.util;

import com.productos.mari.domain.auth.SecurityLogRepository;
import com.productos.mari.domain.marketing.OfferService;
import com.productos.mari.domain.notification.NotificationService;
import com.productos.mari.domain.reservation.Reservation;
import com.productos.mari.domain.reservation.ReservationStatus;
import com.productos.mari.domain.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ReservationValidatorTest {

    @Mock private SecurityLogRepository securityLogRepository;
    @Mock private NotificationService notificationService;
    @Mock private OfferService offerService;

    @InjectMocks
    private ReservationValidator validator;

    @Test
    void validateOwnership_Success() {
        User user = User.builder().email("owner@test.com").build();
        Reservation reservation = Reservation.builder().user(user).build();
        
        assertDoesNotThrow(() -> validator.validateOwnership(reservation, "owner@test.com"));
    }

    @Test
    void validateOwnership_Failure() {
        User user = User.builder().email("owner@test.com").build();
        Reservation reservation = Reservation.builder().user(user).build();
        
        assertThrows(IllegalArgumentException.class, () -> validator.validateOwnership(reservation, "other@test.com"));
    }

    @Test
    void validateStatusForEdit_Success() {
        Reservation reservation = Reservation.builder().status(ReservationStatus.PENDING).build();
        assertDoesNotThrow(() -> validator.validateStatusForEdit(reservation));
    }

    @Test
    void validateStatusForEdit_Failure() {
        Reservation reservation = Reservation.builder().status(ReservationStatus.COMPLETED).build();
        assertThrows(IllegalArgumentException.class, () -> validator.validateStatusForEdit(reservation));
    }
}
