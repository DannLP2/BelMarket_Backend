package com.productos.mari.domain.infrastructure.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductAuditDto {
    private Long id;
    private String name;
    private String brandName;
    private String categoryName;
    private String mainImageUrl;
    
    // Inventory & Pricing
    private Integer stock;
    private BigDecimal purchasePrice;
    private BigDecimal currentPrice;
    private BigDecimal marginPercentage;
    
    // Sales Analytics
    private Integer totalUnitsSold;
    private BigDecimal totalRevenue;
    private BigDecimal totalProfit;
    
    // Status
    private Boolean hasActiveOffer;
    private BigDecimal activeOfferDiscount;
    private String activeOfferType;
    private LocalDateTime createdAt;
}
