package com.productos.mari.domain.auth;

import com.productos.mari.domain.infrastructure.admin.SecurityStatsDto;
import com.productos.mari.domain.reservation.ReservationRepository;
import com.productos.mari.domain.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityAdminControllerTest {

    @Mock private SecurityLogRepository securityLogRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private ProductRepository productRepository;

    @InjectMocks
    private SecurityAdminController securityAdminController;

    @Test
    void getLogs_ReturnsPagedResults() {
        Page<SecurityLog> mockPage = new PageImpl<>(Collections.emptyList());
        when(securityLogRepository.searchLogs(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(mockPage);

        ResponseEntity<Page<SecurityLog>> response = securityAdminController.getLogs(
                null, null, null, null, null, Pageable.unpaged());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(securityLogRepository).searchLogs(null, null, null, null, null, Pageable.unpaged());
    }

    @Test
    void getStats_BuildsResponseWithTopIPs() {
        when(securityLogRepository.count()).thenReturn(200L);
        when(securityLogRepository.countLogsSince(any(LocalDateTime.class))).thenReturn(15L);

        // 7 rows: the controller limits to top 5
        List<Object[]> suspiciousRaw = new java.util.ArrayList<>();
        suspiciousRaw.add(new Object[]{"192.168.1.1", 30L});
        suspiciousRaw.add(new Object[]{"10.0.0.1", 25L});
        suspiciousRaw.add(new Object[]{"172.16.0.1", 20L});
        suspiciousRaw.add(new Object[]{"192.168.0.1", 15L});
        suspiciousRaw.add(new Object[]{"8.8.8.8", 10L});
        suspiciousRaw.add(new Object[]{"1.1.1.1", 5L});
        suspiciousRaw.add(new Object[]{"4.4.4.4", 2L});
        when(securityLogRepository.findTopSuspiciousIPs()).thenReturn(suspiciousRaw);

        ResponseEntity<SecurityStatsDto> response = securityAdminController.getStats();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(200L, response.getBody().getTotalLogs());
        assertEquals(15L, response.getBody().getLogsLastHour());
        assertEquals(5, response.getBody().getTopSuspiciousIPs().size()); // capped at 5
        assertEquals("192.168.1.1", response.getBody().getTopSuspiciousIPs().get(0).get("ip"));
    }

    @Test
    void getDelivererAudit_MapsRowsToDto() {
        List<Object[]> delivStats = new java.util.ArrayList<>();
        delivStats.add(new Object[]{1L, "John Doe", "john@test.com", "http://img.com/a.jpg", 10L, 8L, 1L, 80.0, "http://img.com/proof.jpg"});
        when(reservationRepository.findDelivererSummary()).thenReturn(delivStats);

        ResponseEntity<?> response = securityAdminController.getDelivererAudit();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(reservationRepository).findDelivererSummary();
    }

    @Test
    void getCustomerAudit_MapsRowsToDto() {
        List<Object[]> custStats = new java.util.ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        // Row: id, name, email, picture, totalOrders, completed, cancelled, totalSpent, lastOrderDate, lastActiveAt, createdAt
        custStats.add(new Object[]{1L, "Jane", "jane@test.com", null, 5L, 4L, 1L,
                new BigDecimal("1500.00"), now, now, now});
        when(reservationRepository.findUserAuditSummary()).thenReturn(custStats);

        ResponseEntity<?> response = securityAdminController.getCustomerAudit();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(reservationRepository).findUserAuditSummary();
    }

    @Test
    void getProductAudit_MapsRowsWithMarginCalculation() {
        List<Object[]> prodStats = new java.util.ArrayList<>();
        prodStats.add(new Object[]{1L, "Shampoo", "BrandX", "http://img.com/s.jpg", 100,
                new BigDecimal("50.00"), new BigDecimal("120.00"),
                50, new BigDecimal("6000.00"), new BigDecimal("3500.00"), LocalDateTime.now()});
        when(productRepository.findProductAuditSummary()).thenReturn(prodStats);

        ResponseEntity<?> response = securityAdminController.getProductAudit();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(productRepository).findProductAuditSummary();
    }
}
