package com.productos.mari.domain.infrastructure.admin;

import com.productos.mari.domain.auth.SecurityLogRepository;
import com.productos.mari.domain.infrastructure.media.MediaService;
import com.productos.mari.domain.marketing.OfferService;
import com.productos.mari.domain.product.ProductRepository;
import com.productos.mari.domain.reservation.ReservationItemRepository;
import com.productos.mari.domain.reservation.ReservationRepository;
import com.productos.mari.domain.settings.AppSettings;
import com.productos.mari.domain.settings.AppSettingsService;
import com.productos.mari.domain.support.SupportRequestRepository;
import com.productos.mari.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminReportingServiceTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private ReservationItemRepository reservationItemRepository;
    @Mock private SupportRequestRepository supportRequestRepository;
    @Mock private SecurityLogRepository securityLogRepository;
    @Mock private MediaService mediaService;
    @Mock private AppSettingsService appSettingsService;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private AdminReportingService adminReportingService;

    @BeforeEach
    void setUp() {
        AppSettings settings = new AppSettings();
        settings.setTaxEnabled(true);
        settings.setTaxRate(new BigDecimal("19"));
        settings.setFreeShippingThreshold(new BigDecimal("100000"));
        settings.setDistanceShippingEnabled(true);
        when(appSettingsService.getSettings()).thenReturn(settings);
    }

    @Test
    void getAdminStats_AggregatesAllDataCorrectly() throws Exception {
        // Mock general metrics
        when(reservationRepository.calculateTotalRevenue()).thenReturn(new BigDecimal("1000000"));
        when(reservationRepository.calculateTotalProfit()).thenReturn(new BigDecimal("200000"));
        when(productRepository.calculateTotalInvestment()).thenReturn(new BigDecimal("500000"));
        when(reservationRepository.count()).thenReturn(50L);
        when(reservationRepository.countPendingReservations()).thenReturn(5L);
        when(userRepository.count()).thenReturn(100L);
        when(productRepository.count()).thenReturn(30L);
        when(productRepository.countLowStockProducts(10)).thenReturn(3L);

        // Mock growth metrics
        when(reservationRepository.sumRevenueBetween(any(), any())).thenReturn(150000.0);
        when(reservationRepository.countCompletedBetween(any(), any())).thenReturn(10L);

        // Mock trends
        when(reservationRepository.findMonthlyRevenueByYear(anyInt())).thenReturn(Collections.emptyList());
        when(reservationRepository.findDailyRevenueBetween(any(), any())).thenReturn(Collections.emptyList());

        // Mock others
        when(supportRequestRepository.countByStatus("PENDING")).thenReturn(2L);
        when(securityLogRepository.countAlertsSince(any())).thenReturn(1L);
        when(mediaService.getAllMedia()).thenReturn(Collections.emptyList());

        AdminStatsDto stats = adminReportingService.getAdminStats();

        assertNotNull(stats);
        assertEquals(1000000.0, stats.getTotalRevenue());
        assertEquals(50L, stats.getTotalOrders());
        assertEquals(100L, stats.getTotalUsers());
        assertEquals(30L, stats.getTotalProducts());
        assertEquals(3L, stats.getLowStockCount());
        assertTrue(stats.getIsTaxEnabled());
    }

    @Test
    void getAdminStats_CalculatesGrowthCorrectly() throws Exception {
        // Mock current month revenue 200, previous 100 -> 100% growth
        when(reservationRepository.sumRevenueBetween(any(), any()))
                .thenReturn(200.0) // current
                .thenReturn(100.0); // previous

        when(reservationRepository.findMonthlyRevenueByYear(anyInt())).thenReturn(Collections.emptyList());
        when(reservationRepository.findDailyRevenueBetween(any(), any())).thenReturn(Collections.emptyList());
        when(mediaService.getAllMedia()).thenReturn(Collections.emptyList());

        AdminStatsDto stats = adminReportingService.getAdminStats();

        assertEquals(100.0, stats.getRevenueGrowth());
    }
}
