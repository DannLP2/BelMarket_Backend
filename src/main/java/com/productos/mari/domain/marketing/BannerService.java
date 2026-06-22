package com.productos.mari.domain.marketing;

import com.productos.mari.domain.marketing.Banner;
import java.util.List;

public interface BannerService {
    List<BannerDto> getActiveBanners();
    List<BannerDto> getActiveBannersByPlacement(BannerPlacement placement);
    List<BannerDto> getAllBanners();
    BannerDto createBanner(BannerDto bannerDto);
    BannerDto updateBanner(Long id, BannerDto bannerDto);
    void deleteBanner(Long id);
    void toggleBannerStatus(Long id);
    void recordMetric(Long bannerId, AdMetricType type, String ipAddress);
}
