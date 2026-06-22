package com.productos.mari.domain.infrastructure.communication;

import com.productos.mari.domain.marketing.Offer;
import com.productos.mari.domain.product.Product;
import com.productos.mari.domain.reservation.ReservationDto;
import com.productos.mari.domain.reservation.ReservationStatus;
import com.productos.mari.domain.settings.AppSettings;
import com.productos.mari.domain.settings.AppSettingsService;
import com.productos.mari.domain.support.SupportRequest;
import com.productos.mari.domain.user.Role;
import com.productos.mari.domain.user.User;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Set;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private AppSettingsService appSettingsService;

    @InjectMocks
    private EmailServiceImpl emailService;

    private AppSettings mockSettings;

    @BeforeEach
    void setUp() {
        mockSettings = AppSettings.builder()
                .storeName("BelMarket")
                .address("Store Address")
                .contactEmail("store@test.com")
                .logoUrl("http://logo.com/img.png")
                .build();
        
        when(appSettingsService.getSettings()).thenReturn(mockSettings);
        ReflectionTestUtils.setField(emailService, "fromEmail", "no-reply@test.com");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://test-app.com");
    }

    // ==========================================
    // EXISTING TESTS
    // ==========================================
    @Test
    void sendWelcomeEmail_Success() {
        MimeMessage mockMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mockMessage);

        emailService.sendWelcomeEmail("user@test.com", "John");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendVerificationEmail_Success() {
        MimeMessage mockMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mockMessage);

        emailService.sendVerificationEmail("user@test.com", "John", "123456");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendReservationConfirmation_Success() {
        MimeMessage mockMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mockMessage);
        
        ReservationDto reservation = ReservationDto.builder()
                .id(1L)
                .reference("REF1")
                .total(new BigDecimal("100"))
                .build();

        emailService.sendReservationConfirmation("user@test.com", "John", reservation, new byte[]{1, 2, 3});

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendReservationStatusUpdate_Success() {
        MimeMessage mockMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mockMessage);

        emailService.sendReservationStatusUpdate("user@test.com", "John", "REF1", ReservationStatus.SHIPPED, "DELIV-123");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendRoleUpdateNotification_Success() {
        MimeMessage mockMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mockMessage);

        emailService.sendRoleUpdateNotification("user@test.com", "John", Set.of(Role.ADMIN));

        verify(mailSender).send(any(MimeMessage.class));
    }

    // ==========================================
    // NEW SECURITY & NOTIFICATION TESTS
    // ==========================================

    @Test
    void sendPasswordResetEmail_Success() {
        MimeMessage mockMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mockMessage);

        emailService.sendPasswordResetEmail("user@test.com", "John", "999888");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendPasswordChangeNotification_Success() {
        MimeMessage mockMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mockMessage);

        emailService.sendPasswordChangeNotification("user@test.com", "John");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendAccountStatusNotification_Enabled_Success() {
        MimeMessage mockMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mockMessage);

        emailService.sendAccountStatusNotification("user@test.com", "John", true); // Enabled

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendAccountStatusNotification_Disabled_Success() {
        MimeMessage mockMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mockMessage);

        emailService.sendAccountStatusNotification("user@test.com", "John", false); // Disabled

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendBroadcastEmail_Success() {
        MimeMessage mockMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mockMessage);

        emailService.sendBroadcastEmail("all@test.com", "Gran Venta", "Disfruta el Black Friday");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendSupportNotification_Success() {
        MimeMessage mockMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mockMessage);

        SupportRequest request = new SupportRequest();
        request.setName("John");
        request.setEmail("john@example.com");
        request.setRequestType("Problemas de acceso");
        request.setMessage("No puedo entrar");
        request.setOrderNumber("ORD1");
        request.setAttachmentUrl("http://cdn.com/error.png");

        emailService.sendSupportNotification(request);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendSupportConfirmation_Success() {
        MimeMessage mockMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mockMessage);

        SupportRequest request = new SupportRequest();
        request.setName("John");
        request.setEmail("john@example.com");
        request.setRequestType("Problemas de acceso");
        request.setMessage("Gracias por ayudarme");

        emailService.sendSupportConfirmation(request);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendNewOfferEmail_Success() {
        MimeMessage mockMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mockMessage);

        User user = User.builder().email("john@example.com").name("John").build();
        Product product = Product.builder().name("Shampoo").slug("shampoo").mainImageUrl("http://img.com/a").build();
        Offer offer = Offer.builder()
                .discountType(Offer.DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("20"))
                .build();

        emailService.sendNewOfferEmail(user, product, offer);

        verify(mailSender).send(any(MimeMessage.class));
    }
}
