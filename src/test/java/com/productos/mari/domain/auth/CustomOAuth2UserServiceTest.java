package com.productos.mari.domain.auth;

import com.productos.mari.domain.infrastructure.communication.EmailService;
import com.productos.mari.domain.notification.NotificationService;
import com.productos.mari.domain.user.User;
import com.productos.mari.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.productos.mari.domain.infrastructure.location.IpLocationService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;
    @Mock private NotificationService notificationService;
    @Mock private IpLocationService ipLocationService;

    @InjectMocks
    private CustomOAuth2UserService customOAuth2UserService;

    @BeforeEach
    void setUp() {
        lenient().when(ipLocationService.getCurrencyFromIp(anyString())).thenReturn("COL");
    }

    private OAuth2User buildMockOAuth2User(String email, String name, String picture) {
        OAuth2User mockUser = mock(OAuth2User.class);
        when(mockUser.getAttributes()).thenReturn(Map.of(
                "email", email,
                "name", name,
                "picture", picture
        ));
        return mockUser;
    }

    @Test
    void processOAuth2User_NewUser_RegistersAndSendsWelcome() {
        OAuth2User mockOauth2User = buildMockOAuth2User("new@google.com", "New User", "http://photo.jpg");

        when(userRepository.findByEmail("new@google.com")).thenReturn(Optional.empty());
        
        User savedUser = User.builder().id(42L).email("new@google.com").name("New User").build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Call the private processOAuth2User indirectly via reflection or by making the test target it
        // We test via processOAuth2User through a spy avoiding super.loadUser()
        CustomOAuth2UserService spy = spy(customOAuth2UserService);
        
        // Call directly to protected private method via reflection
        try {
            var method = CustomOAuth2UserService.class.getDeclaredMethod("processOAuth2User", OAuth2User.class);
            method.setAccessible(true);
            method.invoke(spy, mockOauth2User);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Reflection call failed: " + e.getCause());
        }

        // Verify new user was saved
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User createdUser = captor.getValue();
        assertEquals("new@google.com", createdUser.getEmail());
        assertTrue(createdUser.isVerified());
        assertTrue(createdUser.isEnabled());

        // Verify welcome email was sent
        verify(emailService).sendWelcomeEmail(eq("new@google.com"), eq("New User"));
        verify(notificationService).createNotification(any(), any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void processOAuth2User_ExistingUser_UpdatesNameAndGooglePicture() {
        OAuth2User mockOauth2User = buildMockOAuth2User("existing@google.com", "Updated Name", "http://new-photo.jpg");

        User existingUser = User.builder()
                .id(7L)
                .email("existing@google.com")
                .name("Old Name")
                .profilePictureUrl("https://lh3.googleusercontent.com/old.jpg") // Google URL — safe to overwrite
                .build();
        when(userRepository.findByEmail("existing@google.com")).thenReturn(Optional.of(existingUser));

        try {
            var method = CustomOAuth2UserService.class.getDeclaredMethod("processOAuth2User", OAuth2User.class);
            method.setAccessible(true);
            method.invoke(customOAuth2UserService, mockOauth2User);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Reflection call failed: " + e.getCause());
        }

        // Name and picture must be updated
        assertEquals("Updated Name", existingUser.getName());
        assertEquals("http://new-photo.jpg", existingUser.getProfilePictureUrl());
        verify(userRepository).save(existingUser);

        // No welcome email for returning users
        verify(emailService, never()).sendWelcomeEmail(any(), any());
    }

    @Test
    void processOAuth2User_ExistingUserWithCustomPhoto_PreservesCustomPicture() {
        OAuth2User mockOauth2User = buildMockOAuth2User("user@google.com", "User", "http://google-photo.jpg");

        User existingUser = User.builder()
                .id(5L)
                .email("user@google.com")
                .name("User")
                .profilePictureUrl("https://res.cloudinary.com/demo/my-custom.jpg") // Cloudinary — preserve
                .build();
        when(userRepository.findByEmail("user@google.com")).thenReturn(Optional.of(existingUser));

        try {
            var method = CustomOAuth2UserService.class.getDeclaredMethod("processOAuth2User", OAuth2User.class);
            method.setAccessible(true);
            method.invoke(customOAuth2UserService, mockOauth2User);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Reflection call failed: " + e.getCause());
        }

        // Custom Cloudinary picture must NOT be overwritten
        assertEquals("https://res.cloudinary.com/demo/my-custom.jpg", existingUser.getProfilePictureUrl());
    }
}
