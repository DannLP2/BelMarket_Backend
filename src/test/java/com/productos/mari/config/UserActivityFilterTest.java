package com.productos.mari.config;

import com.productos.mari.domain.user.User;
import com.productos.mari.domain.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserActivityFilterTest {

    @Mock private UserRepository userRepository;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;
    @Mock private Authentication authentication;
    @Mock private SecurityContext securityContext;

    @InjectMocks
    private UserActivityFilter userActivityFilter;

    private MockedStatic<SecurityContextHolder> mockedSecurityContextHolder;

    @BeforeEach
    void setUp() {
        mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class);
    }

    @AfterEach
    void tearDown() {
        mockedSecurityContextHolder.close();
    }

    @Test
    void doFilterInternal_UpdatesLastActiveAt_WhenAuthenticated() throws Exception {
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("test@test.com");
        when(authentication.getName()).thenReturn("test@test.com");

        User user = new User();
        user.setEmail("test@test.com");
        user.setLastActiveAt(LocalDateTime.now().minusMinutes(1)); // More than 15s ago

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        userActivityFilter.doFilterInternal(request, response, filterChain);

        verify(userRepository).save(argThat(u -> u.getLastActiveAt() != null));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_DoesNothing_WhenAnonymous() throws Exception {
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("anonymousUser");

        userActivityFilter.doFilterInternal(request, response, filterChain);

        verify(userRepository, never()).findByEmail(anyString());
        verify(filterChain).doFilter(request, response);
    }
}
