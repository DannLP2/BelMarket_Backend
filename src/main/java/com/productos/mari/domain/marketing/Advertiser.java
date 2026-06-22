package com.productos.mari.domain.marketing;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "advertisers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Advertiser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToMany(mappedBy = "advertiser", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AdMetric> metrics = new ArrayList<>();

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_name")
    private String contactName;

    private String phone;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "website_url")
    private String websiteUrl;

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

    @Column(name = "ad_order")
    @Builder.Default
    private int adOrder = 0;

    @Column(name = "views_count")
    @Builder.Default
    private Long viewsCount = 0L;

    @Column(name = "clicks_count")
    @Builder.Default
    private Long clicksCount = 0L;

    @Builder.Default
    private boolean active = true;

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
}
