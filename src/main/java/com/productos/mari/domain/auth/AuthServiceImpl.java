package com.productos.mari.domain.auth;
import com.productos.mari.domain.infrastructure.audit.SecurityAuditService;
import com.productos.mari.domain.notification.NotificationService;
import org.springframework.transaction.annotation.Transactional;
import com.productos.mari.domain.infrastructure.location.IpLocationService;

import lombok.extern.slf4j.Slf4j;


import com.productos.mari.domain.notification.NotificationCategory;
import com.productos.mari.domain.auth.RefreshToken;
import com.productos.mari.domain.auth.SecurityAction;
import com.productos.mari.domain.user.User;
import com.productos.mari.domain.user.UserRepository;
import com.productos.mari.domain.auth.JwtService;
import com.productos.mari.domain.auth.AuthMapper;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final com.productos.mari.domain.infrastructure.media.CloudinaryService cloudinaryService;
    private final com.productos.mari.domain.infrastructure.communication.EmailService emailService;
    private final NotificationService notificationService;
    private final RefreshTokenService refreshTokenService;
    private final SecurityAuditService securityAuditService;
    private final AuthMapper authMapper;
    private final IpLocationService ipLocationService;

    private String getClientIP() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) return "0.0.0.0";
        HttpServletRequest request = attributes.getRequest();
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    private String generateVerificationCode() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, org.springframework.web.multipart.MultipartFile image) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("El correo ya está registrado.");
        }

        String profilePictureUrl = null;
        if (image != null && !image.isEmpty()) {
            try {
                profilePictureUrl = cloudinaryService.uploadFile(image, "belmarket/profiles");
            } catch (java.io.IOException e) {
                // No bloqueamos el registro si falla la imagen, pero informamos (o podemos ignorar si es opcional)
                log.error("Error al subir imagen de perfil: " + e.getMessage());
            }
        }

        String code = generateVerificationCode();
        
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .roles(request.getRoles() != null && !request.getRoles().isEmpty() 
                        ? request.getRoles() 
                        : new java.util.HashSet<>(java.util.List.of(com.productos.mari.domain.user.Role.CLIENT)))
                .documentType(request.getDocumentType())
                .documentNumber(request.getDocumentNumber())
                .birthDate(request.getBirthDate())
                .gender(request.getGender())
                .profilePictureUrl(profilePictureUrl)
                .location(request.getLocation() != null && !request.getLocation().isEmpty() 
                        ? request.getLocation() 
                        : ipLocationService.getCurrencyFromIp(getClientIP()))
                .isVerified(false)
                .status(com.productos.mari.domain.user.UserStatus.PENDING)
                .verificationCode(code)
                .codeExpiresAt(LocalDateTime.now().plusMinutes(15))
                .createdAt(LocalDateTime.now()) // Set manually for immediate response
                .build();
        userRepository.save(user);
        
        securityAuditService.log(SecurityAction.REGISTER_SUCCESS, getClientIP(), user.getEmail(), "Registro exitoso de nuevo usuario");
        
        emailService.sendVerificationEmail(user.getEmail(), user.getName(), code);
        
        AuthResponse response = authMapper.toResponse(user);
        response.setNeedsVerification(true);
        response.setMessage("Te hemos enviado un código de verificación a tu correo.");
        
        return response;
    }

    @Override
    @Transactional
    public AuthResponse verifyEmail(VerificationRequest request) {
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        if (user.isVerified()) {
            throw new IllegalArgumentException("La cuenta ya está verificada.");
        }

        if (user.getCodeExpiresAt() == null || user.getCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("El código ha expirado. Por favor solicita uno nuevo.");
        }

        if (!user.getVerificationCode().equals(request.getCode())) {
            securityAuditService.log(SecurityAction.VERIFY_FAILED, getClientIP(), request.getEmail(), "PIN de verificacion incorrecto");
            throw new IllegalArgumentException("El código es incorrecto.");
        }

        // Marcar como verificado y activo
        user.setVerified(true);
        user.setStatus(com.productos.mari.domain.user.UserStatus.ACTIVE);
        user.setVerificationCode(null);
        user.setCodeExpiresAt(null);
        userRepository.save(user);

        // Bienvenida oficial
        emailService.sendWelcomeEmail(user.getEmail(), user.getName());
        notificationService.createNotification(
                user.getId(),
                "¡Bienvenido a BelMarket!",
                "Encuentra de todo en un solo lugar. Descubre nuestro catálogo con lo mejor en moda, cuidado personal, hogar y lo último en tecnología IoT.",
                "waving_hand",
                "/",
                NotificationCategory.SUCCESS,
                false
        );

        var jwtToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        AuthResponse response = authMapper.toResponse(user);
        response.setToken(jwtToken);
        response.setRefreshToken(refreshToken.getToken());
        response.setMessage("Cuenta verificada exitosamente.");
        
        return response;
    }

    @Override
    @Transactional
    public AuthResponse authenticate(AuthRequest request) {
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales incorrectas."));

        // Verificar contraseña manualmente antes de ver si está deshabilitado
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            securityAuditService.log(SecurityAction.LOGIN_FAILED, getClientIP(), request.getEmail(), "Clave incorrecta");
            throw new IllegalArgumentException("Credenciales incorrectas.");
        }

        // Si la contraseña es correcta, verificar el estado de la cuenta
        if (user.getStatus() != com.productos.mari.domain.user.UserStatus.ACTIVE) {
            switch (user.getStatus()) {
                case PENDING:
                    // Re-enviar código de verificación
                    String code = generateVerificationCode();
                    user.setVerificationCode(code);
                    user.setCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
                    userRepository.save(user);
                    
                    emailService.sendVerificationEmail(user.getEmail(), user.getName(), code);
                    
                    AuthResponse response = authMapper.toResponse(user);
                    response.setNeedsVerification(true);
                    response.setMessage("Tu cuenta no ha sido verificada. Te hemos enviado un nuevo código a tu correo.");
                    
                    return response;
                
                case INACTIVE_BY_USER:
                    throw new IllegalArgumentException("Tu cuenta está desactivada por solicitud propia. Contacta con soporte para reactivarla.");
                
                case SUSPENDED:
                    throw new IllegalArgumentException("Tu cuenta ha sido suspendida por un administrador debido a un incumplimiento de normas. Contacta con soporte.");
                
                default:
                    throw new IllegalArgumentException("Tu cuenta no está activa. Contacta con soporte.");
            }
        }

        // Si todo está bien, autenticar con el manager (opcional si ya verificamos)
        // Pero lo hacemos para mantener la coherencia
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var jwtToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());
        
        securityAuditService.log(SecurityAction.LOGIN_SUCCESS, getClientIP(), user.getEmail(), "Sesion iniciada correctamente");
        
        // Actualizar ubicación si no la tiene (migración o fallos previos)
        if (user.getLocation() == null || user.getLocation().isEmpty()) {
            user.setLocation(ipLocationService.getCurrencyFromIp(getClientIP()));
            userRepository.save(user);
        }
        
        AuthResponse response = authMapper.toResponse(user);
        response.setToken(jwtToken);
        response.setRefreshToken(refreshToken.getToken());

        return response;
    }

    @Override
    @Transactional
    public AuthResponse forgotPassword(ForgotPasswordRequest request) {
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Si el correo existe, se ha enviado un código.")); // Prevenir user enumeration

        String code = generateVerificationCode();
        user.setVerificationCode(code);
        user.setCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), code);

        AuthResponse response = authMapper.toResponse(user);
        response.setNeedsVerification(true);
        response.setMessage("Te hemos enviado un código de recuperación a tu correo.");
        
        return response;
    }

    @Override
    @Transactional
    public AuthResponse resetPassword(ResetPasswordRequest request) {
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        if (user.getCodeExpiresAt() == null || user.getCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("El código ha expirado. Por favor solicita uno nuevo.");
        }

        if (!user.getVerificationCode().equals(request.getCode())) {
            throw new IllegalArgumentException("El código es incorrecto.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setVerificationCode(null);
        user.setCodeExpiresAt(null);
        userRepository.save(user);

        // Notificar por Email y Plataforma
        emailService.sendPasswordChangeNotification(user.getEmail(), user.getName());
        notificationService.createNotification(
            user.getId(),
            "Seguridad: Clave Recuperada",
            "Tu contraseña ha sido restaurada con éxito mediante recuperación de cuenta.",
            "lock_reset",
            "/profile",
            NotificationCategory.SECURITY,
            false
        );

        AuthResponse response = authMapper.toResponse(user);
        response.setNeedsVerification(false);
        response.setMessage("Tu contraseña ha sido actualizada con éxito. Inicia sesión.");
        
        return response;
    }

    @Override
    public AuthResponse getMe() {
        var email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        
        AuthResponse response = authMapper.toResponse(user);
        System.out.println("DEBUG [Auth]: Mapping profile for: " + email + ", Location in Entity: " + user.getLocation());
        System.out.println("DEBUG [Auth]: Location in DTO: " + response.getLocation());
        
        return response;
    }

    @Override
    @Transactional
    public void logout() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return;
        }

        var user = userRepository.findByEmail(auth.getName()).orElse(null);
        if (user != null) {
            // Revocamos todos los tokens de refresco del usuario para máxima seguridad al cerrar sesión
            refreshTokenService.revokeByUserId(user.getId());
            securityAuditService.log(SecurityAction.LOGOUT, getClientIP(), user.getEmail(), "Cierre de sesión: Tokens revocados.");
        }
    }
}
