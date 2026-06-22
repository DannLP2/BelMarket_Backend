package com.productos.mari.domain.auth;

import com.productos.mari.domain.user.User;
import com.productos.mari.domain.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock private JwtService jwtService;
    @Mock private UserDetailsService userDetailsService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private OAuth2AuthenticationSuccessHandler handler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(handler, "frontendUrl", "http://localhost:4200");
    }

    @Test
    void onAuthenticationSuccess_GeneratesTokensAndRedirects() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Authentication authentication = mock(Authentication.class);

        // Principal is an OAuth2User from Google
        OAuth2User oauth2User = mock(OAuth2User.class);
        when(oauth2User.getAttribute("email")).thenReturn("john@gmail.com");
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        // Response is not yet committed
        when(response.isCommitted()).thenReturn(false);

        // UserDetails loaded from DB
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername("john@gmail.com")).thenReturn(userDetails);

        // JWT generated
        when(jwtService.generateToken(userDetails)).thenReturn("mock-access-token");

        // User entity found
        User user = User.builder().id(10L).email("john@gmail.com").build();
        when(userRepository.findByEmail("john@gmail.com")).thenReturn(Optional.of(user));

        // Refresh token created
        RefreshToken refreshToken = RefreshToken.builder().token("mock-refresh-token").build();
        when(refreshTokenService.createRefreshToken(10L)).thenReturn(refreshToken);

        // Intercept the redirect
        RedirectStrategy redirectStrategy = mock(RedirectStrategy.class);
        handler.setRedirectStrategy(redirectStrategy);

        handler.onAuthenticationSuccess(request, response, authentication);

        // Should redirect to /auth/callback with both tokens in URL
        verify(redirectStrategy).sendRedirect(
                eq(request),
                eq(response),
                contains("token=mock-access-token")
        );
        verify(redirectStrategy).sendRedirect(
                eq(request),
                eq(response),
                contains("refreshToken=mock-refresh-token")
        );
    }

    @Test
    void onAuthenticationSuccess_SkipsIfResponseAlreadyCommitted() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Authentication authentication = mock(Authentication.class);

        when(response.isCommitted()).thenReturn(true);

        handler.onAuthenticationSuccess(request, response, authentication);

        // Nothing should happen — response is committed
        verifyNoInteractions(jwtService, userRepository, refreshTokenService);
    }
}
