package com.productos.mari.domain.auth;

import com.productos.mari.domain.infrastructure.audit.SecurityAuditService;
import com.productos.mari.domain.infrastructure.communication.EmailService;
import com.productos.mari.domain.notification.NotificationService;
import com.productos.mari.domain.user.User;
import com.productos.mari.domain.user.UserRepository;
import com.productos.mari.domain.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private com.productos.mari.domain.infrastructure.media.CloudinaryService cloudinaryService;
    @Mock private EmailService emailService;
    @Mock private NotificationService notificationService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private SecurityAuditService securityAuditService;
    @Mock private AuthMapper authMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .email("user@test.com")
                .name("Test User")
                .password("encoded_pass")
                .isVerified(false)
                .status(UserStatus.PENDING)
                .verificationCode("123456")
                .codeExpiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
    }

    @Test
    void register_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@test.com");
        request.setPassword("password");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_pass");
        when(userRepository.save(any())).thenReturn(mockUser);
        when(authMapper.toResponse(any())).thenReturn(new AuthResponse());

        AuthResponse response = authService.register(request, null);

        assertNotNull(response);
        assertTrue(response.isNeedsVerification());
        verify(emailService).sendVerificationEmail(eq("user@test.com"), any(), anyString());
    }

    @Test
    void register_DuplicateEmail_ThrowsException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(mockUser));

        assertThrows(IllegalArgumentException.class, () -> authService.register(request, null));
    }

    @Test
    void verifyEmail_AlreadyVerified_Throws() {
        mockUser.setVerified(true);
        VerificationRequest request = new VerificationRequest();
        request.setEmail("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(mockUser));

        assertThrows(IllegalArgumentException.class, () -> authService.verifyEmail(request));
    }

    @Test
    void verifyEmail_ExpiredCode_Throws() {
        mockUser.setCodeExpiresAt(LocalDateTime.now().minusMinutes(1));
        VerificationRequest request = new VerificationRequest();
        request.setEmail("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(mockUser));

        assertThrows(IllegalArgumentException.class, () -> authService.verifyEmail(request));
    }

    @Test
    void verifyEmail_WrongCode_Throws() {
        VerificationRequest request = new VerificationRequest();
        request.setEmail("user@test.com");
        request.setCode("wrong");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(mockUser));

        assertThrows(IllegalArgumentException.class, () -> authService.verifyEmail(request));
        verify(securityAuditService).log(eq(SecurityAction.VERIFY_FAILED), any(), any(), any());
    }

    @Test
    void verifyEmail_Success() {
        VerificationRequest request = new VerificationRequest();
        request.setEmail("user@test.com");
        request.setCode("123456");

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(mockUser));
        when(jwtService.generateToken(any())).thenReturn("jwt_token");
        when(refreshTokenService.createRefreshToken(anyLong())).thenReturn(new RefreshToken());
        when(authMapper.toResponse(any())).thenReturn(new AuthResponse());

        AuthResponse response = authService.verifyEmail(request);

        assertNotNull(response);
        verify(emailService).sendWelcomeEmail(any(), any());
        assertTrue(mockUser.isVerified());
        assertEquals(UserStatus.ACTIVE, mockUser.getStatus());
    }

    @Test
    void authenticate_Success() {
        AuthRequest request = new AuthRequest();
        request.setEmail("user@test.com");
        request.setPassword("password");

        mockUser.setVerified(true);
        mockUser.setStatus(UserStatus.ACTIVE);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("password", "encoded_pass")).thenReturn(true);
        when(jwtService.generateToken(any())).thenReturn("jwt_token");
        when(refreshTokenService.createRefreshToken(anyLong())).thenReturn(new RefreshToken());
        when(authMapper.toResponse(any())).thenReturn(new AuthResponse());

        AuthResponse response = authService.authenticate(request);

        assertNotNull(response);
        verify(authenticationManager).authenticate(any());
        verify(securityAuditService).log(eq(SecurityAction.LOGIN_SUCCESS), any(), any(), any());
    }

    @Test
    void authenticate_AccountPending_ResendsEmail() {
        AuthRequest request = new AuthRequest();
        request.setEmail("user@test.com");
        request.setPassword("password");

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("password", "encoded_pass")).thenReturn(true);
        when(authMapper.toResponse(any())).thenReturn(new AuthResponse());

        AuthResponse response = authService.authenticate(request);

        assertTrue(response.isNeedsVerification());
        verify(emailService).sendVerificationEmail(any(), any(), any());
    }

    @Test
    void authenticate_AccountSuspended_Throws() {
        AuthRequest request = new AuthRequest();
        request.setEmail("user@test.com");
        request.setPassword("password");
        mockUser.setStatus(UserStatus.SUSPENDED);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("password", "encoded_pass")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.authenticate(request));
    }

    @Test
    void forgotPassword_Success() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("user@test.com");

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(mockUser));
        when(authMapper.toResponse(any())).thenReturn(new AuthResponse());

        AuthResponse response = authService.forgotPassword(request);
        assertTrue(response.isNeedsVerification());
        verify(emailService).sendPasswordResetEmail(any(), any(), any());
    }

    @Test
    void resetPassword_Success() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("user@test.com");
        request.setCode("123456");
        request.setNewPassword("new_password");

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.encode("new_password")).thenReturn("new_encoded_pass");
        when(authMapper.toResponse(any())).thenReturn(new AuthResponse());

        AuthResponse response = authService.resetPassword(request);

        assertEquals("Tu contraseña ha sido actualizada con éxito. Inicia sesión.", response.getMessage());
        verify(emailService).sendPasswordChangeNotification(any(), any());
    }

    @Test
    void logout_RevokesTokens() {
        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("user@test.com");
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(mockUser));

        authService.logout();

        verify(refreshTokenService).revokeByUserId(1L);
        verify(securityAuditService).log(eq(SecurityAction.LOGOUT), any(), any(), any());
    }
}
