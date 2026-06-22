package com.productos.mari.domain.auth;

import com.productos.mari.domain.user.User;
import com.productos.mari.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpControllerTest {

    @Mock private OtpService otpService;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private OtpController otpController;

    @Test
    void sendOtp_ReturnsSuccessMessage() {
        UserDetails mockPrincipal = mock(UserDetails.class);
        when(mockPrincipal.getUsername()).thenReturn("user@test.com");

        User user = User.builder().email("user@test.com").name("John").build();
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        doNothing().when(otpService).generateAndSendOtp("user@test.com", "John");

        ResponseEntity<Map<String, String>> response = otpController.sendOtp(mockPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Código OTP enviado al correo.", response.getBody().get("message"));
        verify(otpService).generateAndSendOtp("user@test.com", "John");
    }

    @Test
    void verifyOtp_WithValidCode_ReturnsSuccess() {
        UserDetails mockPrincipal = mock(UserDetails.class);
        when(mockPrincipal.getUsername()).thenReturn("user@test.com");
        when(otpService.verifyOtp("user@test.com", "123456")).thenReturn(true);

        ResponseEntity<Map<String, String>> response = otpController.verifyOtp(
                mockPrincipal, Map.of("code", "123456"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Verificación Exitosa.", response.getBody().get("message"));
    }

    @Test
    void verifyOtp_WithInvalidCode_ReturnsBadRequest() {
        UserDetails mockPrincipal = mock(UserDetails.class);
        when(mockPrincipal.getUsername()).thenReturn("user@test.com");
        when(otpService.verifyOtp("user@test.com", "000000")).thenReturn(false);

        ResponseEntity<Map<String, String>> response = otpController.verifyOtp(
                mockPrincipal, Map.of("code", "000000"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("El código es inválido o ha expirado.", response.getBody().get("message"));
    }
}
