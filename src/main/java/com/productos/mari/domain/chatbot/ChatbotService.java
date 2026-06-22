package com.productos.mari.domain.chatbot;

import com.productos.mari.domain.chatbot.strategies.ChatbotRoleStrategy;
import com.productos.mari.domain.settings.AppSettings;
import com.productos.mari.domain.settings.AppSettingsService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ChatbotService {

    private final ChatClient chatClient;
    private final AppSettingsService appSettingsService;
    private final List<ChatbotRoleStrategy> strategies;

    public ChatbotService(ChatClient.Builder builder, 
                          AppSettingsService appSettingsService,
                          List<ChatbotRoleStrategy> strategies) {
        this.chatClient = builder.build();
        this.appSettingsService = appSettingsService;
        this.strategies = strategies;
    }

    public String ask(String message, String role) {
        AppSettings settings = appSettingsService.getSettings();

        // Dynamically find the appropriate strategy based on the role
        ChatbotRoleStrategy strategy = strategies.stream()
                .filter(s -> s.supports(role))
                .findFirst()
                .orElse(strategies.stream()
                        .filter(s -> s.supports("CLIENT"))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("No default Chatbot Strategy found")));

        String systemPrompt = strategy.getSystemPrompt(message, settings);

        try {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(message)
                    .call()
                    .content();
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("429")) {
                log.warn("Mía reached Gemini Quota (429). Returning friendly message to user.");
                return "¡Mía está tomando un breve descanso! ✨ He alcanzado mi límite de consultas por ahora. Hablemos de nuevo en unos minutos.";
            }
            throw e; // Rethrow other errors to be handled by the controller/global handler
        }
    }
}
