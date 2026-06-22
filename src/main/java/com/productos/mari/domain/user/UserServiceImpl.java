package com.productos.mari.domain.user;

import lombok.extern.slf4j.Slf4j;

import com.productos.mari.domain.user.UserUpdateRequest;
import com.productos.mari.domain.user.Role;
import com.productos.mari.domain.user.User;
import com.productos.mari.domain.user.UserRepository;
import com.productos.mari.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.productos.mari.domain.infrastructure.media.CloudinaryService;
import com.productos.mari.domain.infrastructure.audit.SecurityAuditService;
import com.productos.mari.domain.infrastructure.communication.EmailService;
import com.productos.mari.domain.notification.NotificationService;
import com.productos.mari.domain.notification.NotificationCategory;
import com.productos.mari.domain.auth.SecurityAction;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional(readOnly = true)
@Slf4j
public class UserServiceImpl implements UserService {

    @org.springframework.beans.factory.annotation.Value("${app.admin.email}")
    private String ownerEmail;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;
    private final com.productos.mari.domain.reservation.ReservationRepository reservationRepository;
    private final SecurityAuditService securityAuditService;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public User updateUser(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
                
        checkHierarchyProtection(user, "modificar perfil/roles");

        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // Prevent changing own role
        if (request.getRoles() != null && !request.getRoles().equals(user.getRoles())) {
            if (user.getEmail().equals(currentEmail)) {
                throw new IllegalArgumentException("No puedes cambiar tus propios roles por seguridad");
            }
            
            // RBAC Check for ADMIN/SUPER_ADMIN 🛡️
            User currentUser = userRepository.findByEmail(currentEmail).orElseThrow();
            boolean isSuperAdmin = currentUser.getRoles().contains(Role.SUPER_ADMIN);
            
            boolean changingAdminRole = request.getRoles().contains(Role.ADMIN) != user.getRoles().contains(Role.ADMIN);
            boolean changingSuperAdminRole = request.getRoles().contains(Role.SUPER_ADMIN) != user.getRoles().contains(Role.SUPER_ADMIN);
            
            if ((changingAdminRole || changingSuperAdminRole) && !isSuperAdmin) {
                throw new IllegalArgumentException("Permisos insuficientes: Solo un Super Administrador puede gestionar roles administrativos.");
            }

            Set<Role> finalRoles = new HashSet<>(request.getRoles());
            finalRoles.add(Role.CLIENT); // Always force CLIENT role 🛡️
            user.setRoles(finalRoles);
            
            // Notificar cambio de roles si fue modificado en updateUser general
            emailService.sendRoleUpdateNotification(user.getEmail(), user.getName(), user.getRoles());
            notificationService.notifyUserUpdate(user.getId());
            notificationService.createNotification(
                user.getId(),
                "Permisos Actualizados",
                "Un administrador ha modificado tus roles de acceso.",
                "verified_user",
                "/profile",
                com.productos.mari.domain.notification.NotificationCategory.INFO,
                false
            );
        }

        if (request.getName() != null) user.setName(request.getName());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getDocumentType() != null) user.setDocumentType(request.getDocumentType());
        if (request.getDocumentNumber() != null) user.setDocumentNumber(request.getDocumentNumber());
        if (request.getBirthDate() != null) user.setBirthDate(request.getBirthDate());
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getLocation() != null) user.setLocation(request.getLocation());
        
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            
            // Notificar por Email y Plataforma
            emailService.sendPasswordChangeNotification(user.getEmail(), user.getName());
            notificationService.createNotification(
                user.getId(),
                "Seguridad: Clave Actualizada",
                "Tu administrador ha actualizado tu contraseña de acceso.",
                "verified_user",
                "/profile",
                NotificationCategory.SECURITY,
                false
            );
        }

        User saved = userRepository.save(user);

        securityAuditService.log(
            com.productos.mari.domain.auth.SecurityAction.SETTING_CHANGED,
            null,
            currentEmail,
            "Perfil del usuario auditado y modificado por administrador: " + user.getEmail() + " (ID: " + user.getId() + ")"
        );

        return saved;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public User updateProfile(UserUpdateRequest request) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Validar contraseña actual si se intenta cambiar la clave o desactivar cuenta
        boolean isChangingPassword = request.getPassword() != null && !request.getPassword().isEmpty();
        boolean isDeactivating = "inactive".equalsIgnoreCase(request.getStatus());

        if (isChangingPassword || isDeactivating) {
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isEmpty()) {
                throw new IllegalArgumentException("Se requiere la contraseña actual para realizar esta acción");
            }
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new IllegalArgumentException("La contraseña actual es incorrecta");
            }
        }

        if (request.getName() != null) user.setName(request.getName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getDocumentType() != null) user.setDocumentType(request.getDocumentType());
        if (request.getDocumentNumber() != null) user.setDocumentNumber(request.getDocumentNumber());
        if (request.getBirthDate() != null) user.setBirthDate(request.getBirthDate());
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getLocation() != null) user.setLocation(request.getLocation());
        if (request.getDefaultDashboard() != null) user.setDefaultDashboard(request.getDefaultDashboard());
        
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            
            // Notificar por Email y Plataforma
            emailService.sendPasswordChangeNotification(user.getEmail(), user.getName());
            notificationService.createNotification(
                user.getId(),
                "Seguridad: Clave Actualizada",
                "Tu contraseña de acceso ha sido cambiada exitosamente.",
                "verified_user",
                "/profile",
                NotificationCategory.SECURITY,
                false
            );
        }

        // Manejar solicitud de desactivación por el usuario
        if ("inactive".equalsIgnoreCase(request.getStatus())) {
            user.setStatus(com.productos.mari.domain.user.UserStatus.INACTIVE_BY_USER);
            user.setEnabled(false);
            emailService.sendAccountStatusNotification(user.getEmail(), user.getName(), false);
        }

        return userRepository.save(user);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public User updateRoles(Long userId, Set<Role> roles) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
                
        checkHierarchyProtection(user, "gestionar roles");
                
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        if (user.getEmail().equals(currentEmail)) {
            throw new IllegalArgumentException("No puedes cambiar tus propios roles por seguridad");
        }
 
        Set<Role> oldRoles = new HashSet<>(user.getRoles());
        
        // RBAC Check for ADMIN/SUPER_ADMIN 🛡️
        User currentUser = userRepository.findByEmail(currentEmail).orElseThrow();
        boolean isSuperAdmin = currentUser.getRoles().contains(Role.SUPER_ADMIN);
        
        boolean changingAdminRole = roles.contains(Role.ADMIN) != user.getRoles().contains(Role.ADMIN);
        boolean changingSuperAdminRole = roles.contains(Role.SUPER_ADMIN) != user.getRoles().contains(Role.SUPER_ADMIN);
        
        if ((changingAdminRole || changingSuperAdminRole) && !isSuperAdmin) {
            throw new IllegalArgumentException("Permisos insuficientes: Solo un Super Administrador puede gestionar roles administrativos.");
        }

        Set<Role> finalRoles = new HashSet<>(roles);
        finalRoles.add(Role.CLIENT); // Always force CLIENT role 🛡️
        user.setRoles(finalRoles);
        
        User saved = userRepository.save(user);
 
        securityAuditService.log(
            SecurityAction.ROLE_CHANGED, 
            null, 
            currentEmail, 
            "Cambio de roles para " + user.getEmail() + ": de " + oldRoles + " a " + user.getRoles()
        );
 
        // 1. Notificación de Plataforma
        notificationService.createNotification(
            user.getId(),
            "Actualización de Permisos 🛡️",
            "Tus roles de acceso han sido actualizados por un administrador.",
            "admin_panel_settings",
            "/profile",
            NotificationCategory.INFO,
            false
        );

        // 2. Notificación por Email
        emailService.sendRoleUpdateNotification(user.getEmail(), user.getName(), user.getRoles());

        // 3. Señal SSE para refresco de sesión en tiempo real
        notificationService.notifyUserUpdate(user.getId());

        return saved;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public User updateProfilePicture(MultipartFile file) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
                
        String oldImageUrl = user.getProfilePictureUrl();
                
        try {
            String newImageUrl = cloudinaryService.uploadFile(file, "belmarket/avatars");
            user.setProfilePictureUrl(newImageUrl);
            User savedUser = userRepository.save(user);
            
            if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
                try {
                    String publicId = cloudinaryService.extractPublicId(oldImageUrl);
                    if (publicId != null) cloudinaryService.deleteFile(publicId);
                } catch (Exception ex) {
                    log.error("Advertencia: No se pudo eliminar foto antigua: " + ex.getMessage());
                }
            }
            
            return savedUser;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error al subir la imagen de perfil: " + e.getMessage());
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public User updateUserProfilePicture(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String oldImageUrl = user.getProfilePictureUrl();

        try {
            String newImageUrl = cloudinaryService.uploadFile(file, "belmarket/avatars");
            user.setProfilePictureUrl(newImageUrl);
            User savedUser = userRepository.save(user);

            // Clean up old photo from Cloudinary
            if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
                try {
                    String publicId = cloudinaryService.extractPublicId(oldImageUrl);
                    if (publicId != null) cloudinaryService.deleteFile(publicId);
                } catch (Exception ex) {
                    log.error("Advertencia: No se pudo eliminar foto antigua del usuario " + userId + ": " + ex.getMessage());
                }
            }

            return savedUser;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error al subir la foto de perfil del usuario: " + e.getMessage());
        }
    }

    private String extractPublicId(String url) {
        if (url == null || url.isEmpty()) return null;
        int lastSlash = url.lastIndexOf('/');
        int lastDot = url.lastIndexOf('.');
        if (lastSlash != -1 && lastDot != -1 && lastDot > lastSlash) {
            return url.substring(lastSlash + 1, lastDot);
        }
        return null;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public User toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
                
        checkHierarchyProtection(user, "bloquear cuenta");
                
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        if (user.getEmail().equals(currentEmail)) {
            throw new IllegalArgumentException("No puedes bloquear/desbloquear tu propia cuenta");
        }

        if (user.getStatus() == com.productos.mari.domain.user.UserStatus.ACTIVE) {
            user.setStatus(com.productos.mari.domain.user.UserStatus.SUSPENDED);
            user.setEnabled(false);
        } else {
            user.setStatus(com.productos.mari.domain.user.UserStatus.ACTIVE);
            user.setEnabled(true);
        }
        
        User saved = userRepository.save(user);
        
        // Force SSE disconnect to lock out suspended users in real-time 🛡️
        notificationService.notifyUserUpdate(user.getId());
        
        boolean isNowEnabled = saved.getStatus() == com.productos.mari.domain.user.UserStatus.ACTIVE;

        // Notify via Email about the block/activation
        emailService.sendAccountStatusNotification(user.getEmail(), user.getName(), isNowEnabled);

        securityAuditService.log(
            isNowEnabled ? SecurityAction.USER_ACTIVATED : SecurityAction.USER_DEACTIVATED,
            null,
            currentEmail,
            (isNowEnabled ? "Cuenta activada" : "Cuenta desactivada/suspendida") + " para " + user.getEmail()
        );

        return saved;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public User updateDefaultDashboard(DashboardType dashboardType) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
                
        user.setDefaultDashboard(dashboardType);
        User saved = userRepository.save(user);
        
        // Notificar al frontend vía SSE para refrescar estado en tiempo real si es necesario
        notificationService.notifyUserUpdate(user.getId());
        
        return saved;
    }

    @Override
    public com.productos.mari.domain.user.UserProfileDto getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
                
        return buildUserProfileResponse(user);
    }

    @Override
    public com.productos.mari.domain.user.UserProfileDto getCurrentUserProfile() {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado o sesión expirada"));
                
        return buildUserProfileResponse(user);
    }

    private com.productos.mari.domain.user.UserProfileDto buildUserProfileResponse(User user) {
        com.productos.mari.domain.user.UserStatsDto stats = reservationRepository.getUserStats(user.getId());
        
        // Ensure stats are not null if user has no orders
        if (stats == null || stats.getTotalOrders() == null) {
            stats = com.productos.mari.domain.user.UserStatsDto.builder()
                    .totalOrders(0L)
                    .totalSpent(java.math.BigDecimal.ZERO)
                    .averageOrderValue(0.0)
                    .lastOrderDate(null)
                    .build();
        }

        List<com.productos.mari.domain.reservation.ReservationDto> recent = reservationRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .limit(10) // Only last 10 for the profile overview
                .map(this::mapReservationToDto)
                .collect(java.util.stream.Collectors.toList());

        return com.productos.mari.domain.user.UserProfileDto.builder()
                .user(user)
                .stats(stats)
                .recentReservations(recent)
                .build();
    }

    private com.productos.mari.domain.reservation.ReservationDto mapReservationToDto(com.productos.mari.domain.reservation.Reservation reservation) {
        return com.productos.mari.domain.reservation.ReservationDto.builder()
                .id(reservation.getId())
                .total(reservation.getTotal())
                .status(reservation.getStatus())
                .createdAt(reservation.getCreatedAt())
                .build();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
                
        checkHierarchyProtection(user, "eliminar permanentemente");
                
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        if (user.getEmail().equals(currentEmail)) {
            throw new IllegalArgumentException("No puedes eliminar tu propia cuenta");
        }
        
        String profilePictureUrl = user.getProfilePictureUrl();
        userRepository.deleteById(userId);
        
        securityAuditService.log(
            com.productos.mari.domain.auth.SecurityAction.USER_DEACTIVATED, // O PRODUCT_DELETED si quieres una genérica
            null,
            currentEmail,
            "Usuario ELIMINADO permanentemente: " + user.getEmail() + " (ID: " + userId + ")"
        );
        
        // Limpieza de foto de perfil
        if (profilePictureUrl != null && !profilePictureUrl.isEmpty() && profilePictureUrl.contains("cloudinary.com")) {
            try {
                String publicId = extractPublicId(profilePictureUrl);
                if (publicId != null) {
                    cloudinaryService.deleteFile(publicId);
                }
            } catch (Exception e) {
                log.error("Advertencia: No se pudo eliminar foto de Cloudinary al borrar usuario: " + e.getMessage());
            }
        }
    }

    /**
     * Protector de Cúpula (Jerarquía Suprema): 
     * 1. Nadie puede tocar la cuenta del Fundador (ID 1 o email oficial), excepto el Fundador mismo.
     * 2. Solo el Fundador puede gestionar (editar, bloquear, cambiar roles o eliminar) a otros SUPER_ADMIN.
     */
    private void checkHierarchyProtection(User targetUser, String action) {
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentEmail).orElseThrow();
        
        boolean targetIsOwner = Long.valueOf(1).equals(targetUser.getId()) || ownerEmail.equalsIgnoreCase(targetUser.getEmail());
        boolean targetIsSuperAdmin = targetUser.getRoles().contains(Role.SUPER_ADMIN);
        boolean actorIsOwner = Long.valueOf(1).equals(currentUser.getId()) || ownerEmail.equalsIgnoreCase(currentUser.getEmail());

        // Regla 1: Protección del Fundador
        if (targetIsOwner && !actorIsOwner) {
            throw new SecurityException("¡Seguridad BelMarket! No tienes privilegios para " + action + " del Propietario Fundador.");
        }

        // Regla 2: Jerarquía Suprema de Super Administradores
        if (targetIsSuperAdmin && !actorIsOwner) {
            // Un Super Admin no puede gestionar a otro Super Admin (solo el dueño puede hacerlo)
            if (!currentEmail.equalsIgnoreCase(targetUser.getEmail())) {
                throw new SecurityException("¡Jerarquía Protegida! Solo el Fundador Principal puede " + action + " de otros Super Administradores.");
            }
        }
    }
}
