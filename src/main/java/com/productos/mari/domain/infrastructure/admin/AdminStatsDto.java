package com.productos.mari.domain.infrastructure.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminStatsDto {
    private Double totalRevenue;
    private Double monthlyRevenue;
    private Double totalProfit;
    private Double totalInvestment;
    private Long totalOrders;
    private Long monthlyOrders;
    private Long pendingOrders;
    private Long lowStockCount;
    private Long totalUsers;
    private Long totalProducts;
    private Double revenueGrowth; // % vs previous month

    // New Fields for Enhanced Dashboard
    private Long pendingSupportTicketsCount;
    private Integer securityAlertsCount;
    private Long totalStorageUsedBytes;

    // Configuration Summary
    private Boolean isTaxEnabled;
    private java.math.BigDecimal taxRate;
    private java.math.BigDecimal shippingDefaultCost;
    private java.math.BigDecimal freeShippingThreshold;
    private Boolean distanceShippingEnabled;

    private List<TopProductDto> topProducts;
    private List<DailyRevenueDto> last7DaysRevenue;
    private List<Double> yearlyMonthlyRevenue; // 12 values for the chart

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TopProductDto {
        private String name;
        private Long quantity;
        private String imageUrl;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DailyRevenueDto {
        private String day;
        private Double revenue;
    }
}
