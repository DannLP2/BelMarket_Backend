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
public class AdvertiserDto {
    private Long id;
    private String name;
    private String contactEmail;
    private String contactName;
    private String phone;
    private String logoUrl;
    private String websiteUrl;
    private String adImageUrl;
    private String adTitle;
    private String adDescription;
    private String companyDescription;
    private String buttonText;
    private String redirectUrl;
    private int adOrder;
    private Long viewsCount;
    private Long clicksCount;
    private boolean active;
    private AdPlacement placement;
    private Integer durationMonths;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
