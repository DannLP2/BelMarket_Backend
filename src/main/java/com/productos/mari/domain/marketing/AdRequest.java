package com.productos.mari.domain.marketing;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "ad_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "contact_name", nullable = false)
    private String contactName;

    @Column(nullable = false)
    private String email;

    private String phone;

    @Column(name = "website_url")
    private String websiteUrl;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "ad_image_url")
    private String adImageUrl;

    @Column(name = "ad_title")
    private String adTitle;

    @Column(columnDefinition = "TEXT", name = "ad_description")
    private String adDescription;

    @Column(columnDefinition = "TEXT", name = "company_description")
    private String companyDescription;

    @Column(name = "button_text", length = 50)
    @Builder.Default
    private String buttonText = "VER MÁS";

    @Column(name = "redirect_url")
    private String redirectUrl;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AdRequestStatus status = AdRequestStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "placement")
    @Builder.Default
    private AdPlacement placement = AdPlacement.PRODUCT_DETAIL;

    @Column(name = "duration_months")
    private Integer durationMonths;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
