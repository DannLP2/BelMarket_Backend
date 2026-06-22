package com.productos.mari.domain.reservation;

import com.productos.mari.domain.reservation.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReservationDto {
    private Long id;
    private Long userId;
    private String reference;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String customerProfilePictureUrl;
    private ReservationStatus status;
    private String deliveryMethod;
    private BigDecimal total;
    private BigDecimal shippingCost;
    private String shippingAddress;
    private Double latitude;
    private Double longitude;
    private String neighborhood;
    private String receiverName;
    private String receiverPhone;
    private Long delivererId;
    private String delivererName;
    private BigDecimal taxAmount;
    private BigDecimal taxRate;
    private LocalDateTime createdAt;
    private LocalDateTime shippedAt;
    private LocalDateTime completedAt;
    private String deliveryCode;
    private String deliveryImageUrl;
    private String deliveryNotes;
    private String displayCurrency;
    private BigDecimal exchangeRate;
    private String paymentMethod;
    private String paymentSubMethod;
    private String paymentId;
    private List<ReservationItemDto> items;
}
