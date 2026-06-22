package com.productos.mari.config;

import com.productos.mari.domain.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class UserActivityFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() && 
            !authentication.getPrincipal().equals("anonymousUser")) {
            
            String email = authentication.getName();
            userRepository.findByEmail(email).ifPresent(user -> {
                // Update every 15 seconds for hyper-precision
                LocalDateTime now = LocalDateTime.now();
                if (user.getLastActiveAt() == null || user.getLastActiveAt().isBefore(now.minusSeconds(15))) {
                    user.setLastActiveAt(now);
                    userRepository.save(user);
                }
            });
        }

        filterChain.doFilter(request, response);
    }
}
