package com.productos.mari.domain.chatbot;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Collections;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> askChatbot(
            @RequestBody Map<String, String> request,
            org.springframework.security.core.Authentication authentication
    ) {
        String userMessage = request.get("message");
        String requestedRole = request.getOrDefault("role", "CLIENT");

        // Server-side Security Hardening: Verify user actually HAS the requested role
        String finalRole = "CLIENT"; // Default to safest role
        
        if (authentication != null && authentication.isAuthenticated()) {
            var authorities = authentication.getAuthorities().stream()
                    .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                    .collect(java.util.stream.Collectors.toSet());

            boolean isAdmin = authorities.contains("ROLE_ADMIN");
            boolean isDeliverer = authorities.contains("ROLE_DELIVERER");

            if ("ADMIN".equalsIgnoreCase(requestedRole) && isAdmin) {
                finalRole = "ADMIN";
            } else if ("DELIVERY".equalsIgnoreCase(requestedRole) && (isAdmin || isDeliverer)) {
                finalRole = "DELIVERY";
            }
        }

        if (userMessage == null || userMessage.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Message is required"));
        }

        try {
            String aiResponse = chatbotService.ask(userMessage, finalRole);
            return ResponseEntity.ok(Collections.singletonMap("response", aiResponse));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Collections.singletonMap("error", "AI Service unavailable"));
        }
    }
}
