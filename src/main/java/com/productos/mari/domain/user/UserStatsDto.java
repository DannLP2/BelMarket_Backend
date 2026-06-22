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
public class UserStatsDto {
    private Long totalOrders;
    private BigDecimal totalSpent;
    private Double averageOrderValue;
    private LocalDateTime lastOrderDate;
}
