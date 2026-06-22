package com.productos.mari.domain.reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReservationItemDto {
    private Long id;
    private Long productId;
    private String productSlug;
    private String productName;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal purchasePrice;
    private Boolean priceModified;
    private BigDecimal originalPrice;
    private String imageUrl;
    
    // Snapshot fields for display
    private String offerTitle;
    private BigDecimal discountValue;
}
