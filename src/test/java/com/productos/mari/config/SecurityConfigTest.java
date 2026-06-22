package com.productos.mari.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigTest {

    @Test
    void corsConfigurationSource_SetupCorrectly() {
        SecurityConfig config = new SecurityConfig(null, null, null, null, null);
        ReflectionTestUtils.setField(config, "allowedOrigins", "http://localhost:3000,http://localhost:4200");

        CorsConfigurationSource source = config.corsConfigurationSource();
        assertNotNull(source);
        assertTrue(source instanceof UrlBasedCorsConfigurationSource);
        
        CorsConfiguration corsConfig = ((UrlBasedCorsConfigurationSource) source).getCorsConfigurations().get("/**");
        assertNotNull(corsConfig);
        assertTrue(corsConfig.getAllowedOrigins().contains("http://localhost:3000"));
        assertTrue(corsConfig.getAllowedOrigins().contains("http://localhost:4200"));
        assertTrue(corsConfig.getAllowedMethods().contains("GET"));
        assertTrue(corsConfig.getAllowCredentials());
    }
}
