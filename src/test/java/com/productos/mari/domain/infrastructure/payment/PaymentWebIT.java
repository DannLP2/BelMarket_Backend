package com.productos.mari.domain.infrastructure.payment;

import com.productos.mari.domain.reservation.Reservation;
import com.productos.mari.domain.reservation.ReservationRepository;
import com.productos.mari.domain.reservation.ReservationStatus;
import com.productos.mari.domain.user.User;
import com.productos.mari.domain.user.UserRepository;
import com.productos.mari.domain.notification.NotificationRepository;
import com.productos.mari.domain.reservation.ReservationItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class PaymentWebIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private ReservationItemRepository reservationItemRepository;

    @MockBean
    private PaymentService paymentService;

    private Long reservationId;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        reservationItemRepository.deleteAll(); // Clean items first
        reservationRepository.deleteAll();
        userRepository.deleteAll();

        User user = userRepository.save(User.builder()
                .name("Buyer")
                .email("buyer@test.com")
                .password("pass")
                .build());

        Reservation reservation = reservationRepository.save(Reservation.builder()
                .user(user)
                .status(ReservationStatus.PENDING)
                .total(new BigDecimal("100.00"))
                .deliveryMethod(com.productos.mari.domain.reservation.DeliveryMethod.PICKUP)
                .build());
        reservationId = reservation.getId();
    }

    @Test
    void handleWebhook_Success() throws Exception {
        String payload = "{\"topic\": \"payment\", \"id\": \"123456\"}";

        mockMvc.perform(post("/api/payments/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk());

        verify(paymentService, times(1)).processWebhookNotification(any());
    }
}
