package com.productos.mari.domain.infrastructure.admin;

import com.productos.mari.domain.auth.SecurityLogRepository;
import com.productos.mari.domain.infrastructure.media.MediaService;
import com.productos.mari.domain.marketing.OfferService;
import com.productos.mari.domain.product.ProductRepository;
import com.productos.mari.domain.reservation.ReservationItemRepository;
import com.productos.mari.domain.reservation.ReservationRepository;
import com.productos.mari.domain.settings.AppSettingsService;
import com.productos.mari.domain.support.SupportRequestRepository;
import com.productos.mari.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AdminReportingService {

    private final ReservationRepository reservationRepository;
    private final ReservationItemRepository reservationItemRepository;
    private final SupportRequestRepository supportRequestRepository;
    private final SecurityLogRepository securityLogRepository;
    private final MediaService mediaService;
    private final AppSettingsService appSettingsService;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public AdminStatsDto getAdminStats() {
        LocalDateTime now = LocalDateTime.now();
        int year = now.getYear();
        
        // General Metrics
        java.math.BigDecimal totalRevenue = reservationRepository.calculateTotalRevenue();
        java.math.BigDecimal totalProfit = reservationRepository.calculateTotalProfit();
        java.math.BigDecimal totalInvestment = productRepository.calculateTotalInvestment();
        Long totalOrdersCount = reservationRepository.count();
        Long pendingOrders = reservationRepository.countPendingReservations();
        Long totalUsersCount = userRepository.count();
        Long totalProductsCount = productRepository.count();
        Integer lowStockCount = (int) (long) productRepository.countLowStockProducts(10);

        // Revenue Growth
        LocalDateTime startOfCurrentMonth = now.withDayOfMonth(1).withHour(0).withMinute(0);
        LocalDateTime startOfPreviousMonth = startOfCurrentMonth.minusMonths(1);
        LocalDateTime endOfPreviousMonth = startOfCurrentMonth.minusNanos(1);

        Double currentMonthRevenue = reservationRepository.sumRevenueBetween(startOfCurrentMonth, now);
        Double previousMonthRevenue = reservationRepository.sumRevenueBetween(startOfPreviousMonth, endOfPreviousMonth);
        Long currentMonthOrdersCount = reservationRepository.countCompletedBetween(startOfCurrentMonth, now);
        
        if (currentMonthRevenue == null) currentMonthRevenue = 0.0;
        if (previousMonthRevenue == null) previousMonthRevenue = 0.0;
        if (currentMonthOrdersCount == null) currentMonthOrdersCount = 0L;

        double revenueGrowth = 0.0;
        if (previousMonthRevenue > 0) {
            revenueGrowth = ((currentMonthRevenue - previousMonthRevenue) / previousMonthRevenue) * 100;
        } else if (currentMonthRevenue > 0) {
            revenueGrowth = 100.0;
        }

        // Yearly Trend
        List<Object[]> monthlyRows = reservationRepository.findMonthlyRevenueByYear(year);
        Map<Integer, Double> revenueByMonth = new java.util.HashMap<>();
        for (Object[] row : monthlyRows) {
            int month = ((Number) row[0]).intValue();
            double rev = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            revenueByMonth.put(month, rev);
        }
        List<Double> yearlyTrend = IntStream.rangeClosed(1, 12)
                .mapToObj(m -> revenueByMonth.getOrDefault(m, 0.0))
                .toList();

        // Top Products
        List<AdminStatsDto.TopProductDto> topProducts = reservationItemRepository
                .findTopProductsByQuantity(org.springframework.data.domain.PageRequest.of(0, 3))
                .stream()
                .map(row -> new AdminStatsDto.TopProductDto((String) row[0], ((Number) row[1]).longValue(), (String) row[2]))
                .toList();

        // Daily Revenue
        LocalDateTime sevenDaysAgo = now.minusDays(6).withHour(0).withMinute(0).withSecond(0).withNano(0);
        java.time.format.DateTimeFormatter dateKeyFmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<Object[]> dailyRows = reservationRepository.findDailyRevenueBetween(sevenDaysAgo, now);
        Map<String, Double> revenueByDate = new java.util.HashMap<>();
        for (Object[] row : dailyRows) {
            String dateKey = row[0].toString().substring(0, 10);
            double rev = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            revenueByDate.put(dateKey, rev);
        }
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM");
        List<AdminStatsDto.DailyRevenueDto> dailyRevenue = IntStream.range(0, 7)
                .mapToObj(i -> now.minusDays(6 - i))
                .map(date -> new AdminStatsDto.DailyRevenueDto(date.format(fmt), revenueByDate.getOrDefault(date.format(dateKeyFmt), 0.0)))
                .toList();

        Long pendingSupportTicketsCount = supportRequestRepository.countByStatus("PENDING");
        Integer securityAlertsCount = (int) securityLogRepository.countAlertsSince(now.minusHours(48));

        Long totalStorageUsedBytes = 0L;
        try {
            totalStorageUsedBytes = mediaService.getAllMedia().stream()
                    .mapToLong(com.productos.mari.domain.infrastructure.media.MediaDto::getBytes)
                    .sum();
        } catch (Exception e) {
            log.error("Error calculando almacenamiento: " + e.getMessage());
        }

        com.productos.mari.domain.settings.AppSettings settings = appSettingsService.getSettings();

        return AdminStatsDto.builder()
                .totalRevenue(totalRevenue != null ? totalRevenue.doubleValue() : 0.0)
                .monthlyRevenue(currentMonthRevenue)
                .totalProfit(totalProfit != null ? totalProfit.doubleValue() : 0.0)
                .totalInvestment(totalInvestment != null ? totalInvestment.doubleValue() : 0.0)
                .totalOrders(totalOrdersCount)
                .monthlyOrders(currentMonthOrdersCount)
                .pendingOrders(pendingOrders)
                .lowStockCount((long) lowStockCount)
                .totalUsers(totalUsersCount)
                .totalProducts(totalProductsCount)
                .revenueGrowth(revenueGrowth)
                .topProducts(topProducts)
                .last7DaysRevenue(dailyRevenue)
                .yearlyMonthlyRevenue(yearlyTrend)
                .pendingSupportTicketsCount(pendingSupportTicketsCount)
                .securityAlertsCount(securityAlertsCount)
                .totalStorageUsedBytes(totalStorageUsedBytes)
                .isTaxEnabled(settings.getTaxEnabled())
                .taxRate(settings.getTaxRate())
                .shippingDefaultCost(settings.getDefaultShippingCost())
                .freeShippingThreshold(settings.getFreeShippingThreshold())
                .distanceShippingEnabled(settings.getDistanceShippingEnabled())
                .build();
    }
}
