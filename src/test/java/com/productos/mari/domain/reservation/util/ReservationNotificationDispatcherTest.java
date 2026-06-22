package com.productos.mari.domain.reservation.util;

import com.productos.mari.domain.infrastructure.communication.EmailService;
import com.productos.mari.domain.notification.NotificationCategory;
import com.productos.mari.domain.notification.NotificationScope;
import com.productos.mari.domain.notification.NotificationService;
import com.productos.mari.domain.reservation.Reservation;
import com.productos.mari.domain.reservation.ReservationDto;
import com.productos.mari.domain.reservation.ReservationStatus;
import com.productos.mari.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationNotificationDispatcherTest {

    @Mock private NotificationService notificationService;
    @Mock private EmailService emailService;

    @InjectMocks
    private ReservationNotificationDispatcher dispatcher;

    private Reservation mockReservation;
    private User mockUser;
    private ReservationDto mockDto;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("user@test.com");
        mockUser.setName("User Test");

        mockReservation = new Reservation();
        mockReservation.setId(10L);
        mockReservation.setReference("REF-123");
        mockReservation.setUser(mockUser);
        mockReservation.setDeliveryCode("CODE123");

        mockDto = new ReservationDto();
        mockDto.setTotal(new BigDecimal("15000"));
    }

    @Test
    void dispatchOrderCreated_SendsMultipleNotifications() {
        dispatcher.dispatchOrderCreated(mockReservation, mockDto, mockUser, new byte[0]);

        verify(notificationService).createNotification(eq(1L), anyString(), anyString(), anyString(), anyString(), eq(NotificationCategory.SUCCESS), eq(false));
        verify(notificationService).broadcastNotificationToScope(anyString(), anyString(), anyString(), anyString(), eq(NotificationCategory.ORDER), eq(NotificationScope.ADMIN_SHARED));
        verify(emailService).sendReservationConfirmation(eq("user@test.com"), eq("User Test"), eq(mockDto), any());
        verify(notificationService).broadcastReservationUpdate(contains("NEW_ORDER"));
    }

    @Test
    void dispatchStatusUpdate_Shipped_IncludesDeliveryCode() {
        dispatcher.dispatchStatusUpdate(mockReservation, ReservationStatus.SHIPPED);

        verify(notificationService).createNotification(eq(1L), anyString(), contains("CODE123"), anyString(), anyString(), any(), anyBoolean());
        verify(notificationService).broadcastNotificationToScope(anyString(), anyString(), anyString(), anyString(), eq(NotificationCategory.INFO), eq(NotificationScope.DELIVERER_SHARED));
        verify(emailService).sendReservationStatusUpdate(eq("user@test.com"), eq("User Test"), anyString(), eq(ReservationStatus.SHIPPED), eq("CODE123"));
    }

    @Test
    void dispatchStatusUpdate_Cancelled_NotifiesDeliverers() {
        dispatcher.dispatchStatusUpdate(mockReservation, ReservationStatus.CANCELLED);

        verify(notificationService).broadcastNotificationToScope(contains("Cancelado"), anyString(), anyString(), anyString(), eq(NotificationCategory.ERROR), eq(NotificationScope.DELIVERER_SHARED));
    }

    @Test
    void dispatchStatusUpdate_ReadyForDelivery_CustomMessage() {
        dispatcher.dispatchStatusUpdate(mockReservation, ReservationStatus.READY_FOR_DELIVERY);

        verify(notificationService).createNotification(eq(1L), anyString(), contains("listo y será entregado pronto"), anyString(), anyString(), any(), anyBoolean());
    }
}
