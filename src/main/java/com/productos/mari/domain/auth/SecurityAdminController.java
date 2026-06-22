package com.productos.mari.domain.auth;
import com.productos.mari.domain.user.UserLinkedDeviceRepository;
import com.productos.mari.domain.user.UserRepository;

import com.productos.mari.domain.product.ProductRepository;
import com.productos.mari.domain.infrastructure.admin.SecurityStatsDto;
import com.productos.mari.domain.auth.SecurityLog;
import com.productos.mari.domain.auth.SecurityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users/admin/security")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class SecurityAdminController {

    private final SecurityLogRepository securityLogRepository;
    private final com.productos.mari.domain.reservation.ReservationRepository reservationRepository;
    private final com.productos.mari.domain.product.ProductRepository productRepository;

    @GetMapping("/logs")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<org.springframework.data.domain.Page<SecurityLog>> getLogs(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String email,
            @org.springframework.web.bind.annotation.RequestParam(required = false) com.productos.mari.domain.auth.SecurityAction action,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String ip,
            @org.springframework.web.bind.annotation.RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @org.springframework.web.bind.annotation.RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @org.springframework.data.web.PageableDefault(size = 20, sort = "timestamp", direction = org.springframework.data.domain.Sort.Direction.DESC) org.springframework.data.domain.Pageable pageable) {
        
        return ResponseEntity.ok(securityLogRepository.searchLogs(email, action, ip, start, end, pageable));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SecurityStatsDto> getStats() {
        long totalLogs = securityLogRepository.count();
        long logsLastHour = securityLogRepository.countLogsSince(LocalDateTime.now().minusHours(1));
        
        List<Object[]> suspiciousRaw = securityLogRepository.findTopSuspiciousIPs();
        List<Map<String, Object>> topSuspiciousIPs = new ArrayList<>();
        
        // Limit to top 5
        for (int i = 0; i < Math.min(5, suspiciousRaw.size()); i++) {
            Object[] row = suspiciousRaw.get(i);
            Map<String, Object> map = new HashMap<>();
            map.put("ip", row[0]);
            map.put("count", row[1]);
            topSuspiciousIPs.add(map);
        }

        return ResponseEntity.ok(SecurityStatsDto.builder()
                .totalLogs(totalLogs)
                .logsLastHour(logsLastHour)
                .topSuspiciousIPs(topSuspiciousIPs)
                .build());
    }

    @GetMapping("/deliverers")
    public ResponseEntity<List<com.productos.mari.domain.infrastructure.admin.DelivererAuditDto>> getDelivererAudit() {
        List<Object[]> stats = reservationRepository.findDelivererSummary();
        List<com.productos.mari.domain.infrastructure.admin.DelivererAuditDto> dtos = new ArrayList<>();

        for (Object[] row : stats) {
            Long totalAssigned = ((Number) row[4]).longValue();
            Long totalCompleted = ((Number) row[5]).longValue();
            Long totalCancelled = ((Number) row[6]).longValue();
            
            double successRate = totalAssigned > 0 ? (totalCompleted * 100.0 / totalAssigned) : 0;
            
            dtos.add(com.productos.mari.domain.infrastructure.admin.DelivererAuditDto.builder()
                .id(((Number) row[0]).longValue())
                .name((String) row[1])
                .email((String) row[2])
                .profilePictureUrl((String) row[3])
                .totalAssigned(totalAssigned)
                .totalCompleted(totalCompleted)
                .totalCancelled(totalCancelled)
                .successRate(Math.round(successRate * 10.0) / 10.0)
                .lastDeliveryPhoto((String) row[8])
                .build());
        }
        
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/customers")
    public ResponseEntity<List<com.productos.mari.domain.user.UserAuditDto>> getCustomerAudit() {
        List<Object[]> stats = reservationRepository.findUserAuditSummary();
        List<com.productos.mari.domain.user.UserAuditDto> dtos = new ArrayList<>();

        for (Object[] row : stats) {
            Long totalOrders = ((Number) row[4]).longValue();
            Long completedOrders = ((Number) row[5]).longValue();
            Long cancelledOrders = ((Number) row[6]).longValue();
            java.math.BigDecimal totalSpent = (java.math.BigDecimal) row[7];
            
            double successRate = totalOrders > 0 ? (completedOrders * 100.0 / totalOrders) : 0;
            
            dtos.add(com.productos.mari.domain.user.UserAuditDto.builder()
                .id(((Number) row[0]).longValue())
                .name((String) row[1])
                .email((String) row[2])
                .profilePictureUrl((String) row[3])
                .totalOrders(totalOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .totalSpent(totalSpent != null ? totalSpent : java.math.BigDecimal.ZERO)
                .successRate(Math.round(successRate * 10.0) / 10.0)
                .lastOrderDate((LocalDateTime) row[8])
                .lastActiveAt((LocalDateTime) row[9])
                .createdAt((LocalDateTime) row[10])
                .build());
        }
        
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/products")
    public ResponseEntity<List<com.productos.mari.domain.infrastructure.audit.ProductAuditDto>> getProductAudit() {
        List<Object[]> stats = productRepository.findProductAuditSummary();
        List<com.productos.mari.domain.infrastructure.audit.ProductAuditDto> dtos = new ArrayList<>();

        for (Object[] row : stats) {
            java.math.BigDecimal purchasePrice = (java.math.BigDecimal) row[5];
            java.math.BigDecimal currentPrice = (java.math.BigDecimal) row[6];
            
            java.math.BigDecimal margin = java.math.BigDecimal.ZERO;
            if (currentPrice != null && purchasePrice != null && currentPrice.compareTo(java.math.BigDecimal.ZERO) > 0) {
                margin = currentPrice.subtract(purchasePrice)
                                     .divide(currentPrice, 4, java.math.RoundingMode.HALF_UP)
                                     .multiply(new java.math.BigDecimal(100));
            }

            dtos.add(com.productos.mari.domain.infrastructure.audit.ProductAuditDto.builder()
                .id(((Number) row[0]).longValue())
                .name((String) row[1])
                .brandName((String) row[2])
                .mainImageUrl((String) row[3])
                .stock((Integer) row[4])
                .purchasePrice(purchasePrice)
                .currentPrice(currentPrice)
                .marginPercentage(margin)
                .totalUnitsSold(((Number) row[7]).intValue())
                .totalRevenue((java.math.BigDecimal) row[8])
                .totalProfit((java.math.BigDecimal) row[9])
                .createdAt((LocalDateTime) row[10])
                .build());
        }
        
        return ResponseEntity.ok(dtos);
    }
}

