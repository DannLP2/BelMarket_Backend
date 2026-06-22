package com.productos.mari.domain.marketing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarketingScheduler {

    private final BannerRepository bannerRepository;
    private final AdRequestRepository adRequestRepository;
    private final AdvertiserRepository advertiserRepository;

    /**
     * Runs every hour to clean up expired marketing content.
     */
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void processExpirations() {
        LocalDateTime now = LocalDateTime.now();
        log.info("Starting marketing expiration check at {}", now);

        // 1. Deactivate expired internal banners
        processBanners(now);

        // 2. Mark expired ad requests as EXPIRED
        processAdRequests(now);

        // 3. Deactivate expired external advertisers
        processAdvertisers(now);
    }

    private void processBanners(LocalDateTime now) {
        List<Banner> expiredBanners = bannerRepository.findAll().stream()
                .filter(b -> b.isActive() && b.getEndDate() != null && b.getEndDate().isBefore(now))
                .toList();

        if (!expiredBanners.isEmpty()) {
            log.info("Deactivating {} expired banners", expiredBanners.size());
            expiredBanners.forEach(b -> b.setActive(false));
            bannerRepository.saveAll(expiredBanners);
        }
    }

    private void processAdRequests(LocalDateTime now) {
        List<AdRequest> expiredRequests = adRequestRepository.findAll().stream()
                .filter(r -> r.getStatus() == AdRequestStatus.APPROVED && r.getEndDate() != null && r.getEndDate().isBefore(now))
                .toList();

        if (!expiredRequests.isEmpty()) {
            log.info("Marking {} ad requests as EXPIRED", expiredRequests.size());
            expiredRequests.forEach(r -> r.setStatus(AdRequestStatus.EXPIRED));
            adRequestRepository.saveAll(expiredRequests);
        }
    }

    private void processAdvertisers(LocalDateTime now) {
        List<Advertiser> expiredAds = advertiserRepository.findAll().stream()
                .filter(a -> a.isActive() && a.getEndDate() != null && a.getEndDate().isBefore(now))
                .toList();
        
        if (!expiredAds.isEmpty()) {
            log.info("Deactivating {} expired advertiser ads", expiredAds.size());
            expiredAds.forEach(a -> a.setActive(false));
            advertiserRepository.saveAll(expiredAds);
        }
    }
}
