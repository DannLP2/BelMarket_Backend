package com.productos.mari.domain.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAuditDto {
    private Long id;
    private String name;
    private String email;
    private String profilePictureUrl;
    private Long totalOrders;
    private Long completedOrders;
    private Long cancelledOrders;
    private BigDecimal totalSpent;
    private Double successRate;
    private LocalDateTime lastOrderDate;
    private LocalDateTime lastActiveAt;
    private LocalDateTime createdAt;
}
