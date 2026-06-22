package com.productos.mari.domain.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MassNotificationRequest {
    private String title;
    private String description;
    private String icon;
    private String category;
    private boolean sendEmail;
    private String link;
    private java.util.List<Long> userIds;
    /**
     * Audience target: 'global' | 'admins' | 'clients' | 'deliverers' | 'specific'
     * Falls back to isAdminOnly for backward compatibility.
     */
    private String audience;
    @JsonProperty("isAdminOnly")
    private boolean isAdminOnly;
}
