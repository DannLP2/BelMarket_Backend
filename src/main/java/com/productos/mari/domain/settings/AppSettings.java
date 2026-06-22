package com.productos.mari.domain.settings;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "app_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_name", nullable = false)
    private String storeName;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "whatsapp_number")
    private String whatsappNumber;

    @Column(name = "instagram_url")
    private String instagramUrl;

    @Column(name = "facebook_url")
    private String facebookUrl;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "free_shipping_threshold")
    private BigDecimal freeShippingThreshold;

    @Column(name = "default_shipping_cost")
    private BigDecimal defaultShippingCost;

    @Column(name = "footer_text", length = 500)
    private String footerText;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "meta_title")
    private String metaTitle;

    @Column(name = "meta_description", length = 500)
    private String metaDescription;

    @Column(name = "meta_keywords", length = 500)
    private String metaKeywords;

    @Column(name = "favicon_url")
    private String faviconUrl;

    @Builder.Default
    @Column(name = "copyright_text")
    private String copyrightText = "BelMarket | Tu mercado en línea";

    @Builder.Default
    @Column(name = "tagline")
    private String tagline = "Elegancia & Calidad";

    @Column(name = "bg_light_url")
    private String bgLightUrl;

    @Column(name = "bg_dark_url")
    private String bgDarkUrl;

    @Builder.Default
    @Column(name = "tax_enabled")
    private Boolean taxEnabled = false;

    @Builder.Default
    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate = BigDecimal.ZERO;

    // Delivery Routing System
    @Builder.Default
    @Column(name = "distance_shipping_enabled")
    private Boolean distanceShippingEnabled = false;

    @Column(name = "store_latitude")
    private Double storeLatitude;

    @Column(name = "store_longitude")
    private Double storeLongitude;

    @Column(name = "base_distance_km")
    private Double baseDistanceKm;

    @Column(name = "cost_per_km", precision = 10, scale = 2)
    private BigDecimal costPerKm;

    @Column(name = "max_image_size_mb")
    private Integer maxImageSizeMb;

    @Column(name = "max_pdf_size_mb")
    private Integer maxPdfSizeMb;

    @Builder.Default
    @Column(name = "chatbot_enabled")
    private Boolean chatbotEnabled = false;

    @Builder.Default
    @Column(name = "store_currency")
    private String storeCurrency = "COP";

    @Builder.Default
    @Column(name = "store_currency_symbol")
    private String storeCurrencySymbol = "$";

    @Builder.Default
    @Column(name = "smart_currency_enabled")
    private Boolean smartCurrencyEnabled = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "regional_tax_rates", joinColumns = @JoinColumn(name = "setting_id"))
    @MapKeyColumn(name = "country_code")
    @Column(name = "tax_rate")
    @Builder.Default
    private java.util.Map<String, BigDecimal> regionalTaxRates = new java.util.HashMap<>();
}
