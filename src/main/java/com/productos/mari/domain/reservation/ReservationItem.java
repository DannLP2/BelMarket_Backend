package com.productos.mari.domain.reservation;

import com.productos.mari.domain.product.Product;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "reservation_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "purchase_price")
    private BigDecimal purchasePrice; // Cost at the time of reservation

    @Builder.Default
    @Column(name = "price_modified")
    private Boolean priceModified = false;

    @Column(name = "original_price")
    private BigDecimal originalPrice;

    @Column(name = "product_name_snapshot")
    private String productNameSnapshot;

    @Column(name = "product_image_snapshot")
    private String productImageSnapshot;

    @Column(name = "offer_id_snapshot")
    private Long offerIdSnapshot;

    @Column(name = "offer_title_snapshot")
    private String offerTitleSnapshot;

    @Column(name = "discount_value_snapshot")
    private java.math.BigDecimal discountValueSnapshot;
}
