package com.productos.mari.domain.chatbot.strategies;

import com.productos.mari.domain.settings.AppSettings;

/**
 * Strategy interface for multi-role chatbot prompts.
 * Implementation of this interface are responsible for gathering context and
 * building the system prompt for a specific user role (CLIENT, ADMIN, DELIVERY, etc).
 */
public interface ChatbotRoleStrategy {
    /**
     * Builds the system prompt based on the user's message and store settings.
     * @param message User message (might be needed for dynamic retrieval)
     * @param settings Store settings
     * @return Formatted system prompt
     */
    String getSystemPrompt(String message, AppSettings settings);

    /**
     * Determines if this strategy supports the given role.
     * @param role The role requested from the frontend (security verified)
     * @return true if this strategy handles the role
     */
    boolean supports(String role);
}
