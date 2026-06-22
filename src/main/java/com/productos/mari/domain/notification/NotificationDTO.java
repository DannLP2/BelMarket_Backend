package com.productos.mari.domain.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationDTO {
    private Long id;
    private String title;
    private String description;
    private String icon;
    private String category;
    @JsonProperty("isRead")
    private boolean isRead;
    private LocalDateTime createdAt;
    private String link;
    @JsonProperty("isAdminOnly")
    private boolean isAdminOnly;
    private String lastReadByName;
    private String scope;
}
