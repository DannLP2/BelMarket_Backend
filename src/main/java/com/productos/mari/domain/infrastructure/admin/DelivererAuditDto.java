package com.productos.mari.domain.infrastructure.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DelivererAuditDto {
    private Long id;
    private String name;
    private String email;
    private String profilePictureUrl;
    private Long totalAssigned;
    private Long totalCompleted;
    private Long totalCancelled;
    private Double successRate;
    private Double avgCompletionTimeMinutes;
    private String lastDeliveryPhoto;
    private LocalDateTime lastActiveAt;
}
