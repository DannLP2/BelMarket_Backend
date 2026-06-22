package com.productos.mari.domain.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private UserDetailsService userDetailsService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldSkipFilterForPublicEndpoints() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/auth/login");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).extractEmail(anyString());
    }

    @Test
    void shouldAuthenticateWithValidBearerToken() throws ServletException, IOException {
        String jwt = "valid-token";
        String email = "test@example.com";
        UserDetails userDetails = new User(email, "password", Collections.emptyList());

        when(request.getRequestURI()).thenReturn("/api/products");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
        when(jwtService.extractEmail(jwt)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.isTokenValid(jwt, userDetails)).thenReturn(true);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assert (SecurityContextHolder.getContext().getAuthentication() != null);
        assert (SecurityContextHolder.getContext().getAuthentication().getName().equals(email));
    }

    @Test
    void shouldAuthenticateWithQueryParameterToken() throws ServletException, IOException {
        String jwt = "url-token";
        String email = "param@example.com";
        UserDetails userDetails = new User(email, "password", Collections.emptyList());

        when(request.getRequestURI()).thenReturn("/api/updates");
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getParameter("token")).thenReturn(jwt);
        when(jwtService.extractEmail(jwt)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.isTokenValid(jwt, userDetails)).thenReturn(true);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assert (SecurityContextHolder.getContext().getAuthentication() != null);
    }

    @Test
    void shouldDoFilterWhenNoJwtPresent() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/products");
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getParameter("token")).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assert (SecurityContextHolder.getContext().getAuthentication() == null);
    }

    @Test
    void shouldHandleExpiredJwtExceptionGracefully() throws ServletException, IOException {
        String jwt = "expired-token";
        when(request.getRequestURI()).thenReturn("/api/products");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
        when(jwtService.extractEmail(jwt)).thenThrow(new io.jsonwebtoken.ExpiredJwtException(null, null, "expired"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assert (SecurityContextHolder.getContext().getAuthentication() == null);
    }

    @Test
    void shouldHandleJwtExceptionGracefully() throws ServletException, IOException {
        String jwt = "invalid-token";
        when(request.getRequestURI()).thenReturn("/api/products");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
        when(jwtService.extractEmail(jwt)).thenThrow(new io.jsonwebtoken.JwtException("invalid"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assert (SecurityContextHolder.getContext().getAuthentication() == null);
    }
}
