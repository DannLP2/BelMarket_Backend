package com.productos.mari.domain.infrastructure.payment;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import com.productos.mari.domain.product.Product;
import com.productos.mari.domain.reservation.*;
import com.productos.mari.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceImplTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private ReservationService reservationService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Reservation mockReservation;
    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().email("user@test.com").build();
        
        Product product = Product.builder().name("Product 1").stock(10).build();
        ReservationItem item = ReservationItem.builder()
                .product(product)
                .quantity(1)
                .price(new BigDecimal("100.00"))
                .build();

        mockReservation = Reservation.builder()
                .id(1L)
                .user(mockUser)
                .status(ReservationStatus.PENDING)
                .items(List.of(item))
                .total(new BigDecimal("100.00"))
                .build();

        ReflectionTestUtils.setField(paymentService, "mercadoPagoAccessToken", "test-token");
        ReflectionTestUtils.setField(paymentService, "webhookBaseUrl", "http://webhook.com");
        ReflectionTestUtils.setField(paymentService, "clientBaseUrl", "http://client.com");
    }

    @Test
    void createPaymentPreference_Success() throws Exception {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(mockReservation));

        try (MockedConstruction<PreferenceClient> mocked = mockConstruction(PreferenceClient.class,
                (mock, context) -> {
                    Preference pref = mock(Preference.class);
                    when(pref.getSandboxInitPoint()).thenReturn("http://mercadopago.com/pay");
                    when(mock.create(any(PreferenceRequest.class))).thenReturn(pref);
                })) {
            
            String payUrl = paymentService.createPaymentPreference(1L, "user@test.com");
            
            assertEquals("http://mercadopago.com/pay", payUrl);
            verify(reservationRepository).findById(1L);
        }
    }

    @Test
    void createPaymentPreference_NotAuthorized_ThrowsException() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(mockReservation));
        
        assertThrows(IllegalArgumentException.class, () -> 
            paymentService.createPaymentPreference(1L, "wrong@test.com")
        );
    }

    @Test
    void createPaymentPreference_NoStock_ThrowsException() {
        mockReservation.getItems().get(0).getProduct().setStock(0);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(mockReservation));
        
        assertThrows(IllegalArgumentException.class, () -> 
            paymentService.createPaymentPreference(1L, "user@test.com")
        );
    }

    @Test
    void processWebhookNotification_Approved_UpdatesStatus() throws Exception {
        Map<String, Object> payload = Map.of(
            "action", "payment.created",
            "data", Map.of("id", "12345")
        );

        try (MockedConstruction<PaymentClient> mocked = mockConstruction(PaymentClient.class,
                (mock, context) -> {
                    Payment payment = mock(Payment.class);
                    when(payment.getStatus()).thenReturn("approved");
                    when(payment.getExternalReference()).thenReturn("1");
                    when(mock.get(12345L)).thenReturn(payment);
                })) {
            
            when(reservationRepository.findById(1L)).thenReturn(Optional.of(mockReservation));
            
            paymentService.processWebhookNotification(payload);
            
            verify(reservationService).updateReservationStatus(1L, ReservationStatus.CONFIRMED);
        }
    }
}
