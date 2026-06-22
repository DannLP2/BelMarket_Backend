package com.productos.mari.domain.marketing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "banners")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @JsonIgnore
    @OneToMany(mappedBy = "banner", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BannerMetric> metrics = new ArrayList<>();

    // Internal Banners exclusively. No external advertiser relationship.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BannerPlacement placement = BannerPlacement.HOME_CAROUSEL;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "button_text", length = 50)
    @Builder.Default
    private String buttonText = "VER MÁS";

    @Column(name = "link_url")
    private String linkUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "banner_order")
    @Builder.Default
    private int bannerOrder = 0;

    @Column(name = "views_count")
    @Builder.Default
    private Long viewsCount = 0L;

    @Column(name = "clicks_count")
    @Builder.Default
    private Long clicksCount = 0L;
}
