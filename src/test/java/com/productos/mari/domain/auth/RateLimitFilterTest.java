package com.productos.mari.domain.auth;

import com.productos.mari.domain.infrastructure.audit.SecurityAuditService;
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
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock private SecurityAuditService securityAuditService;
    @Mock private JwtService jwtService;
    
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @InjectMocks
    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void setUp() throws IOException {
        PrintWriter printWriter = new PrintWriter(new StringWriter());
        lenient().when(response.getWriter()).thenReturn(printWriter);
    }

    @Test
    void shouldExtractIpFromXForwardedFor() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/products");
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.100, 10.0.0.1");

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldExtractIpFromRemoteAddr() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/products");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldAllowRequestsUnderLimit() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.5");

        // The login limit is 5. We do 3 requests.
        for (int i = 0; i < 3; i++) {
            rateLimitFilter.doFilterInternal(request, response, filterChain);
        }

        verify(filterChain, times(3)).doFilter(request, response);
    }

    @Test
    void shouldBlockLoginRequestsOverLimit() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.99");
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getParameter("token")).thenReturn(null);

        // The login limit is 5. We try to do 6 requests.
        for (int i = 0; i < 6; i++) {
            rateLimitFilter.doFilterInternal(request, response, filterChain);
        }

        // It should have allowed exactly 5 calls down the chain
        verify(filterChain, times(5)).doFilter(request, response);
        
        // The 6th call should be blocked and log a security audit event
        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(securityAuditService).log(eq(SecurityAction.RATE_LIMIT_EXCEEDED), eq("10.0.0.99"), isNull(), anyString());
    }

    @Test
    void shouldAttemptEmailExtractionWhenBlocked() throws ServletException, IOException {
        String token = "dummy-jwt-token";
        String email = "hacker@example.com";
        when(request.getRequestURI()).thenReturn("/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.88");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractEmail(token)).thenReturn(email);

        // Loop until blocked (6 times)
        for (int i = 0; i < 6; i++) {
            rateLimitFilter.doFilterInternal(request, response, filterChain);
        }

        // Verify that the audit log includes the extracted email
        verify(securityAuditService).log(eq(SecurityAction.RATE_LIMIT_EXCEEDED), eq("10.0.0.88"), eq(email), anyString());
    }

    @Test
    void shouldApplyDifferentLimitsPerEndpoint() throws ServletException, IOException {
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.77");
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getParameter("token")).thenReturn(null);

        // Exhaust login endpoint limit
        when(request.getRequestURI()).thenReturn("/auth/login");
        for (int i = 0; i < 6; i++) {
            rateLimitFilter.doFilterInternal(request, response, filterChain);
        }
        verify(response, times(1)).setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); // 1 block

        // However, a request to a global endpoint should still pass because it uses a different bucket
        when(request.getRequestURI()).thenReturn("/api/products/123");
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Total successful calls should be 5 (from login) + 1 (from global) = 6
        verify(filterChain, times(6)).doFilter(request, response);
    }
}
