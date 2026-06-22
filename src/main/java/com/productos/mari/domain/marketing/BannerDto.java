package com.productos.mari.domain.marketing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BannerDto {
    private Long id;
    private BannerPlacement placement;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String imageUrl;
    private String title;
    private String description;
    private String buttonText;
    private String linkUrl;
    private boolean active;
    private int bannerOrder;
    private Long viewsCount;
    private Long clicksCount;
}
