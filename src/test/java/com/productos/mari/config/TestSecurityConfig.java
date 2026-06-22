package com.productos.mari.config;

import com.productos.mari.domain.auth.JwtAuthenticationFilter;
import com.productos.mari.domain.auth.JwtService;
import org.springframework.http.HttpMethod;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Configuración de seguridad para WebMvcTest.
 * Reutiliza el mismo esquema de reglas del SecurityConfig de producción
 * pero excluyendo los beans de infraestructura (OAuth2, Cloudinary, etc.)
 * que no están disponibles en el slice @WebMvcTest.
 */
@TestConfiguration
@Order(Ordered.HIGHEST_PRECEDENCE)
@EnableMethodSecurity
public class TestSecurityConfig {

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private com.productos.mari.domain.auth.RateLimitFilter rateLimitFilter;

    @MockBean
    private com.productos.mari.domain.user.UserRepository userRepository;

    @Bean
    @Order(1)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        System.out.println("DEBUG: Simplified TestSecurityConfig SecurityFilterChain");
        http
            .csrf(AbstractHttpConfigurer::disable)
            .headers(h -> h.frameOptions(f -> f.disable()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/login", "/auth/register", "/auth/verify-email", "/auth/forgot-password", "/auth/reset-password", "/auth/refresh").permitAll()
                .requestMatchers("/auth/me", "/auth/logout").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/banners", "/api/banners/placement/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/banners/*/track").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/advertisers/active", "/api/advertisers/active/placement/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/advertisers/*/track").permitAll()
                .requestMatchers("/api/products/public/**", "/api/offers/public/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/support").permitAll()
                .requestMatchers("/api/payments/webhook", "/api/ad-requests/public").permitAll()
                .requestMatchers("/api/categories", "/api/brands", "/api/settings").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/*/reviews").permitAll()
                
                // Admin paths
                .requestMatchers("/api/banners/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/advertisers/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/ad-requests/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/support/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/categories/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/brands/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/settings/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/products/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/reservations/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/users/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/reports/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/media/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/notifications/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/mecatronic/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/products/*/reviews/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/products/*/reviews/*/admin").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/products/*/reviews/*/moderate").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/products/*/reviews/*/status").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/products/*/reviews/stats").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/reservations/delivery/**").hasAnyRole("ADMIN", "SUPER_ADMIN", "DELIVERER")
                
                // Authenticated
                .requestMatchers("/api/users/profile/**", "/api/otp/**", "/api/mecatronic/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/products/*/reviews").authenticated()
                .requestMatchers("/api/products/*/reviews/**").authenticated()
                
                .anyRequest().authenticated()
            )
            .exceptionHandling(e -> e.authenticationEntryPoint((request, response, authException) -> {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            }));

        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
        };
    }
}
