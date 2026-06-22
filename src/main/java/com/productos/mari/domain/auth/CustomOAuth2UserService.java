package com.productos.mari.domain.auth;

import lombok.extern.slf4j.Slf4j;

import com.productos.mari.domain.notification.NotificationCategory;
import com.productos.mari.domain.user.Role;
import com.productos.mari.domain.user.User;
import com.productos.mari.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import com.productos.mari.domain.infrastructure.location.IpLocationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@org.springframework.transaction.annotation.Transactional
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final com.productos.mari.domain.infrastructure.communication.EmailService emailService;
    private final com.productos.mari.domain.notification.NotificationService notificationService;
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

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        try {
            return processOAuth2User(oauth2User);
        } catch (Exception ex) {
            throw new OAuth2AuthenticationException(ex.getMessage());
        }
    }

    private OAuth2User processOAuth2User(OAuth2User oauth2User) {
        Map<String, Object> attributes = oauth2User.getAttributes();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture");

        if (email == null || email.isEmpty()) {
            throw new RuntimeException("Email not found from OAuth2 provider");
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;
        
        if (userOptional.isPresent()) {
            user = userOptional.get();
            updateExistingUser(user, name, picture);
        } else {
            user = registerNewUser(email, name, picture);
        }

        return oauth2User;
    }

    private void sendWelcomeAcknowledge(User user) {
        try {
            log.info("Enviando bienvenida a: {}", user.getEmail());
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
        } catch (Exception e) {
            log.error("Error al enviar bienvenida OAuth para {}: {}", user.getEmail(), e.getMessage());
        }
    }

    private User registerNewUser(String email, String name, String picture) {
        User user = User.builder()
                .name(name)
                .email(email)
                .password(UUID.randomUUID().toString()) // Random password since they login with Google
                .roles(java.util.Set.of(Role.CLIENT))
                .profilePictureUrl(picture)
                .isVerified(true) // Google emails are already verified
                .enabled(true)
                .location(ipLocationService.getCurrencyFromIp(getClientIP()))
                .status(com.productos.mari.domain.user.UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
        
        User savedUser = userRepository.save(user);
        
        // Bienvenida oficial para usuarios nuevos
        sendWelcomeAcknowledge(savedUser);

        return savedUser;
    }

    private void updateExistingUser(User user, String name, String picture) {
        boolean wasNotVerified = !user.isVerified();
        
        user.setName(name);
        
        // Si el usuario no estaba verificado, Google sirve como verificación inmediata
        if (wasNotVerified) {
            user.setVerified(true);
            user.setStatus(com.productos.mari.domain.user.UserStatus.ACTIVE);
            user.setLastActiveAt(LocalDateTime.now());
            
            // Asegurar ubicación si es nuevo o no la tiene
            if (user.getLocation() == null || user.getLocation().isEmpty()) {
                user.setLocation(ipLocationService.getCurrencyFromIp(getClientIP()));
            }
            
            userRepository.save(user);
            
            // Disparar bienvenida ya que es su primer ingreso exitoso/verificado
            sendWelcomeAcknowledge(user);
        } else {
            // Solo actualizar la foto si es de Google o está vacía. 
            // Si el usuario subió una personalizada (ej: Cloudinary), no la sobreescribimos.
            String currentUrl = user.getProfilePictureUrl();
            if (currentUrl == null || currentUrl.isEmpty() || currentUrl.contains("googleusercontent.com")) {
                user.setProfilePictureUrl(picture);
            }
            
            
            // Asegurar ubicación si no la tiene
            if (user.getLocation() == null || user.getLocation().isEmpty()) {
                user.setLocation(ipLocationService.getCurrencyFromIp(getClientIP()));
            }
            
            user.setLastActiveAt(LocalDateTime.now());
            userRepository.save(user);
        }
    }
}
