package com.productos.mari.domain.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.productos.mari.domain.auth.SecurityAction;
import com.productos.mari.domain.infrastructure.audit.SecurityAuditService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final SecurityAuditService securityAuditService;
    private final JwtService jwtService;

    // Caches con expulsión automática después de 1 hora de inactividad para evitar fugas de memoria
    private final Cache<String, Bucket> loginBuckets = createCache();
    private final Cache<String, Bucket> registerBuckets = createCache();
    private final Cache<String, Bucket> verifyBuckets = createCache();
    private final Cache<String, Bucket> profilePictureBuckets = createCache();
    private final Cache<String, Bucket> profileUpdateBuckets = createCache();
    private final Cache<String, Bucket> globalBuckets = createCache();

    private Cache<String, Bucket> createCache() {
        return Caffeine.newBuilder()
                .expireAfterAccess(1, TimeUnit.HOURS)
                .maximumSize(10000) // Límite razonable para evitar ataques de agotamiento de memoria
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String ip = getClientIP(request);
        String path = request.getRequestURI();

        Bucket bucket;
        if (path.startsWith("/auth/login") || path.startsWith("/auth/authenticate")) {
            bucket = loginBuckets.get(ip, k -> createNewBucket(5, Duration.ofMinutes(1)));
        } else if (path.startsWith("/auth/register")) {
            bucket = registerBuckets.get(ip, k -> createNewBucket(5, Duration.ofHours(1)));
        } else if (path.startsWith("/auth/verify-email")) {
            bucket = verifyBuckets.get(ip, k -> createNewBucket(10, Duration.ofMinutes(1)));
        } else if (path.equals("/users/profile/picture") && request.getMethod().equalsIgnoreCase("POST")) {
            bucket = profilePictureBuckets.get(ip, k -> createNewBucket(5, Duration.ofMinutes(1)));
        } else if (path.equals("/users/profile") && request.getMethod().equalsIgnoreCase("PUT")) {
            bucket = profileUpdateBuckets.get(ip, k -> createNewBucket(10, Duration.ofMinutes(1)));
        } else {
            bucket = globalBuckets.get(ip, k -> createNewBucket(300, Duration.ofMinutes(1)));
        }

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            String email = extractEmail(request);
            securityAuditService.log(
                SecurityAction.RATE_LIMIT_EXCEEDED, 
                ip, 
                email, 
                "Limite excedido en: " + path
            );
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\": \"Demasiadas peticiones desde esta IP. Por favor, intenta de nuevo en un momento.\"}");
        }
    }

    private Bucket createNewBucket(int capacity, Duration refillDuration) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, refillDuration)
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        // X-Forwarded-For puede contener múltiples IPs: cliente, proxy1, proxy2...
        // Tomamos la primera (la del cliente original)
        String ip = xfHeader.split(",")[0].trim();
        
        // Validación básica de formato IP (evita inyecciones extrañas de cabeceras)
        if (ip.length() > 45) { // Suficiente para IPv6
            return request.getRemoteAddr();
        }
        return ip;
    }

    private String extractEmail(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                return jwtService.extractEmail(token);
            }
            // Intentar desde parámetros (para SSE si aplica)
            String paramToken = request.getParameter("token");
            if (paramToken != null) {
                return jwtService.extractEmail(paramToken);
            }
        } catch (Exception e) {
            // Ignoramos errores de extracción (token inválido o expirado)
            // Ya que el RateLimitFilter no debe bloquear por token inválido, solo por exceso.
        }
        return null;
    }
}
