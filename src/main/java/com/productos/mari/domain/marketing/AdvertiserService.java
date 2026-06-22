package com.productos.mari.domain.marketing;

import java.util.List;

public interface AdvertiserService {
    List<AdvertiserDto> getAllAdvertisers(String search, String status);
    List<AdvertiserDto> getActiveAdvertisers();
    List<AdvertiserDto> getActiveAdvertisersByPlacement(AdPlacement placement);
    List<AdvertiserDto> getAllAdvertisersByPlacement(AdPlacement placement);
    AdvertiserDto createAdvertiser(AdvertiserDto advertiserDto);
    AdvertiserDto updateAdvertiser(Long id, AdvertiserDto advertiserDto);
    void deleteAdvertiser(Long id);
    void toggleAdvertiserStatus(Long id);
    String uploadLogo(org.springframework.web.multipart.MultipartFile file) throws java.io.IOException;
    void reorderAds(List<Long> rankedIds);
    void recordAdMetric(Long advertiserId, AdMetricType type, String ipAddress);
}
