package com.productos.mari.config;

import com.productos.mari.domain.user.User;
import com.productos.mari.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationConfigTest {

    @Mock private UserRepository userRepository;
    @Mock private AuthenticationConfiguration authConfig;

    @InjectMocks
    private ApplicationConfig applicationConfig;

    @Test
    void userDetailsService_ReturnsValidUser() {
        User user = new User();
        user.setEmail("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        UserDetailsService service = applicationConfig.userDetailsService();
        assertEquals(user, service.loadUserByUsername("test@test.com"));
    }

    @Test
    void userDetailsService_ThrowsException_WhenUserNotFound() {
        when(userRepository.findByEmail("bad@test.com")).thenReturn(Optional.empty());

        UserDetailsService service = applicationConfig.userDetailsService();
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("bad@test.com"));
    }

    @Test
    void authenticationProvider_ReturnsDaoProvider() {
        AuthenticationProvider provider = applicationConfig.authenticationProvider();
        assertTrue(provider instanceof DaoAuthenticationProvider);
    }

    @Test
    void passwordEncoder_ReturnsBCrypt() {
        PasswordEncoder encoder = applicationConfig.passwordEncoder();
        assertNotNull(encoder);
        String pw = "test";
        assertTrue(encoder.matches(pw, encoder.encode(pw)));
    }

    @Test
    void authenticationManager_CallsConfig() throws Exception {
        applicationConfig.authenticationManager(authConfig);
        verify(authConfig).getAuthenticationManager();
    }
}
