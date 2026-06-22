package com.productos.mari.domain.product;

import com.productos.mari.domain.marketing.OfferDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductDto {
    private Long id;
    @NotBlank(message = "El nombre del producto es obligatorio")
    private String name;
    
    private String slug;
    
    @NotBlank(message = "La marca no puede estar vacía")
    private String brand;
    
    private String description;
    
    @NotNull(message = "El precio es obligatorio")
    @PositiveOrZero(message = "El precio debe ser 0 o mayor")
    private BigDecimal price;
    
    @PositiveOrZero(message = "El precio de compra no puede ser negativo")
    private BigDecimal purchasePrice;
    
    @NotNull(message = "El stock es obligatorio")
    @PositiveOrZero(message = "El stock debe ser 0 o mayor")
    private Integer stock;
    private String imageUrl;
    private java.util.List<String> categories;
    private java.util.List<String> galleryImageUrls;
    private String videoUrl;
    private java.util.List<ProductManualDto> manuals;
    private LocalDateTime createdAt;

    private Double averageRating;
    private Integer reviewCount;

    /** Precio con descuento (null si no hay oferta activa) */
    private BigDecimal discountedPrice;

    /** Oferta activa del producto (null si no hay) */
    private OfferDto activeOffer;

    private Boolean isActive;
    private Boolean isMecatronic;
    private java.util.List<ProductDetailListDto> detailLists;
}
