package com.productos.mari.domain.auth;

import com.productos.mari.domain.infrastructure.communication.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private OtpRepository otpRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private OtpService otpService;

    private final String EMAIL = "test@example.com";
    private final String CODE = "123456";

    @Test
    void generateAndSendOtp_ShouldDeleteOldAndSaveNew() {
        otpService.generateAndSendOtp(EMAIL, "Test User");

        verify(otpRepository).deleteByEmail(EMAIL);
        verify(otpRepository).save(any(Otp.class));
        verify(emailService).sendVerificationEmail(eq(EMAIL), eq("Test User"), anyString());
    }

    @Test
    void verifyOtp_ShouldReturnTrue_WhenCodeIsCorrectAndNotExpired() {
        Otp otp = Otp.builder()
                .email(EMAIL)
                .code(CODE)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(otpRepository.findByEmailAndCode(EMAIL, CODE)).thenReturn(Optional.of(otp));

        boolean result = otpService.verifyOtp(EMAIL, CODE);

        assertTrue(result);
        verify(otpRepository).delete(otp);
    }

    @Test
    void verifyOtp_ShouldReturnFalse_WhenCodeIsWrong() {
        when(otpRepository.findByEmailAndCode(EMAIL, "wrong")).thenReturn(Optional.empty());

        boolean result = otpService.verifyOtp(EMAIL, "wrong");

        assertFalse(result);
    }

    @Test
    void verifyOtp_ShouldReturnFalse_WhenCodeIsExpired() {
        Otp otp = Otp.builder()
                .email(EMAIL)
                .code(CODE)
                .expiresAt(LocalDateTime.now().minusMinutes(1)) // Expired
                .build();

        when(otpRepository.findByEmailAndCode(EMAIL, CODE)).thenReturn(Optional.of(otp));

        boolean result = otpService.verifyOtp(EMAIL, CODE);

        assertFalse(result);
        verify(otpRepository).delete(otp);
    }
}
