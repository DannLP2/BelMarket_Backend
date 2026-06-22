package com.productos.mari.domain.infrastructure.admin;

import lombok.*;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityStatsDto {
    private long totalLogs;
    private long logsLastHour;
    private List<Map<String, Object>> topSuspiciousIPs;
}
