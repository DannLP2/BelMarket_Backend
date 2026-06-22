package com.productos.mari.domain.marketing;

import com.productos.mari.domain.marketing.Offer.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OfferDto {
    private Long id;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private Boolean active;
    private String title;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startDate;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endDate;
    private Integer minQuantity;
    private BigDecimal originalPrice;
    private BigDecimal finalPrice;
    private LocalDateTime createdAt;
    private Integer unitsSold;
}
