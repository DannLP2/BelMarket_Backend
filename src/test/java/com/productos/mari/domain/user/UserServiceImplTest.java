package com.productos.mari.domain.user;

import com.productos.mari.domain.infrastructure.audit.SecurityAuditService;
import com.productos.mari.domain.infrastructure.communication.EmailService;
import com.productos.mari.domain.infrastructure.media.CloudinaryService;
import com.productos.mari.domain.notification.NotificationService;
import com.productos.mari.domain.user.UserStatsDto;
import com.productos.mari.domain.reservation.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private CloudinaryService cloudinaryService;
    @Mock private ReservationRepository reservationRepository;
    @Mock private SecurityAuditService securityAuditService;
    @Mock private EmailService emailService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private UserServiceImpl userService;

    private User mockUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "ownerEmail", "owner@test.com");

        mockUser = User.builder()
                .id(2L)
                .email("user@test.com")
                .name("Test User")
                .roles(Set.of(Role.CLIENT))
                .password("encoded_pass")
                .status(UserStatus.ACTIVE)
                .build();

        adminUser = User.builder()
                .id(1L)
                .email("owner@test.com")
                .name("Admin")
                .roles(Set.of(Role.SUPER_ADMIN, Role.CLIENT))
                .build();

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("owner@test.com");
        SecurityContextHolder.setContext(securityContext);
        
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(adminUser));
    }

    @Test
    void getAllUsers_ReturnsList() {
        when(userRepository.findAll()).thenReturn(List.of(mockUser));
        List<User> users = userService.getAllUsers();
        assertEquals(1, users.size());
    }

    @Test
    void updateUser_Success() {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setName("New Name");
        request.setRoles(Set.of(Role.ADMIN));

        when(userRepository.findById(2L)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any())).thenReturn(mockUser);

        User result = userService.updateUser(2L, request);

        assertEquals("New Name", result.getName());
        assertTrue(result.getRoles().contains(Role.ADMIN));
        verify(emailService).sendRoleUpdateNotification(any(), any(), any());
    }

    @Test
    void updateUser_CannotChangeOwnRoles() {
        when(SecurityContextHolder.getContext().getAuthentication().getName()).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(mockUser));
        UserUpdateRequest request = new UserUpdateRequest();
        request.setRoles(Set.of(Role.ADMIN));

        when(userRepository.findById(2L)).thenReturn(Optional.of(mockUser));

        assertThrows(IllegalArgumentException.class, () -> userService.updateUser(2L, request));
    }

    @Test
    void updateProfile_Success() {
        when(SecurityContextHolder.getContext().getAuthentication().getName()).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(mockUser));
        
        UserUpdateRequest request = new UserUpdateRequest();
        request.setName("Profile Name");

        when(userRepository.save(any())).thenReturn(mockUser);

        User result = userService.updateProfile(request);

        assertEquals("Profile Name", result.getName());
    }

    @Test
    void toggleUserStatus_Success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.toggleUserStatus(2L);

        assertEquals(UserStatus.SUSPENDED, result.getStatus());
        assertFalse(result.isEnabled());
        verify(emailService).sendAccountStatusNotification(eq("user@test.com"), any(), eq(false));
    }

    @Test
    void checkHierarchyProtection_ProtectsOwner() {
        // Mock current user as someone else
        User otherAdmin = User.builder().id(3L).email("other@test.com").roles(Set.of(Role.ADMIN)).build();
        when(SecurityContextHolder.getContext().getAuthentication().getName()).thenReturn("other@test.com");
        when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(otherAdmin));

        // Attempt to modify owner (adminUser is owner because ID=1)
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        
        UserUpdateRequest request = new UserUpdateRequest();
        request.setName("Hacker");

        assertThrows(SecurityException.class, () -> userService.updateUser(1L, request));
    }

    @Test
    void getCurrentUserProfile_ReturnsProfileWithStats() {
        when(SecurityContextHolder.getContext().getAuthentication().getName()).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(mockUser));
        
        com.productos.mari.domain.user.UserStatsDto stats = com.productos.mari.domain.user.UserStatsDto.builder()
                .totalOrders(5L)
                .totalSpent(new BigDecimal("500.00"))
                .build();
        when(reservationRepository.getUserStats(2L)).thenReturn(stats);

        UserProfileDto profile = userService.getCurrentUserProfile();

        assertNotNull(profile);
        assertEquals(5L, profile.getStats().getTotalOrders());
        assertEquals(mockUser, profile.getUser());
    }

    @Test
    void deleteUser_Success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(mockUser));

        userService.deleteUser(2L);

        verify(userRepository).deleteById(2L);
        verify(securityAuditService).log(any(), any(), any(), contains("ELIMINADO"));
    }

    @Test
    void updateUser_InsufficientPermissions_ToManageAdminRole() {
        // Actor is a regular ADMIN (not owner, not SUPER_ADMIN)
        User adminActor = User.builder().id(3L).email("admin@test.com").roles(Set.of(Role.ADMIN)).build();
        when(SecurityContextHolder.getContext().getAuthentication().getName()).thenReturn("admin@test.com");
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminActor));

        // Target user is a CLIENT
        User target = User.builder().id(2L).email("client@test.com").roles(Set.of(Role.CLIENT)).build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        // Attempting to grant ADMIN role to target
        UserUpdateRequest request = new UserUpdateRequest();
        request.setRoles(Set.of(Role.ADMIN));

        assertThrows(IllegalArgumentException.class, () -> userService.updateUser(2L, request));
    }

    @Test
    void updateProfile_RequiresCurrentPassword_ForSensitiveActions() {
        when(SecurityContextHolder.getContext().getAuthentication().getName()).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(mockUser));

        UserUpdateRequest request = new UserUpdateRequest();
        request.setPassword("new_pass");
        // No current password provided

        assertThrows(IllegalArgumentException.class, () -> userService.updateProfile(request));
    }

    @Test
    void updateProfile_IncorrectCurrentPassword_ThrowsException() {
        when(SecurityContextHolder.getContext().getAuthentication().getName()).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(mockUser));

        UserUpdateRequest request = new UserUpdateRequest();
        request.setPassword("new_pass");
        request.setCurrentPassword("wrong_pass");

        when(passwordEncoder.matches("wrong_pass", "encoded_pass")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> userService.updateProfile(request));
    }

    @Test
    void checkHierarchyProtection_ProtectsSuperAdmin() {
        // Actor is a regular ADMIN
        User adminActor = User.builder().id(3L).email("admin@test.com").roles(Set.of(Role.ADMIN)).build();
        when(SecurityContextHolder.getContext().getAuthentication().getName()).thenReturn("admin@test.com");
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminActor));

        // Target is a SUPER_ADMIN (not owner)
        User superAdminTarget = User.builder().id(4L).email("sa@test.com").roles(Set.of(Role.SUPER_ADMIN)).build();
        when(userRepository.findById(4L)).thenReturn(Optional.of(superAdminTarget));

        UserUpdateRequest request = new UserUpdateRequest();
        request.setName("New Name");

        assertThrows(SecurityException.class, () -> userService.updateUser(4L, request));
    }
}
