package com.productos.mari.domain.marketing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
class BannerRepositoryIT {

    @Autowired
    private BannerRepository bannerRepository;

    @Autowired
    private EntityManager em;

    private Banner activeBanner;
    private Banner expiredBanner;
    private Banner futureBanner;

    @BeforeEach
    void setUp() {
        bannerRepository.deleteAll();

        activeBanner = bannerRepository.save(Banner.builder()
            .title("Active Banner")
            .imageUrl("http://img.com/active.png")
            .active(true)
            .placement(BannerPlacement.HOME_CAROUSEL)
            .bannerOrder(1)
            .startDate(LocalDateTime.now().minusDays(1))
            .endDate(LocalDateTime.now().plusDays(10))
            .viewsCount(0L)
            .clicksCount(0L)
            .build());

        expiredBanner = bannerRepository.save(Banner.builder()
            .title("Expired Banner")
            .imageUrl("http://img.com/exp.png")
            .active(true)
            .placement(BannerPlacement.HOME_CAROUSEL)
            .bannerOrder(2)
            .startDate(LocalDateTime.now().minusDays(10))
            .endDate(LocalDateTime.now().minusDays(1)) // Already past
            .viewsCount(0L)
            .clicksCount(0L)
            .build());

        futureBanner = bannerRepository.save(Banner.builder()
            .title("Future Banner")
            .imageUrl("http://img.com/future.png")
            .active(true)
            .placement(BannerPlacement.HOME_CAROUSEL)
            .bannerOrder(3)
            .startDate(LocalDateTime.now().plusDays(5)) // Not yet started
            .endDate(LocalDateTime.now().plusDays(30))
            .viewsCount(0L)
            .clicksCount(0L)
            .build());
    }

    @Test
    void findActiveByPlacementAndDate_shouldReturnOnlyCurrentlyActiveBanners() {
        List<Banner> result = bannerRepository.findActiveByPlacementAndDate(
            BannerPlacement.HOME_CAROUSEL, LocalDateTime.now());

        assertEquals(1, result.size());
        assertEquals("Active Banner", result.get(0).getTitle());
    }

    @Test
    void findActiveByPlacementAndDate_shouldNotReturnExpiredOrFutureBanners() {
        List<Banner> result = bannerRepository.findActiveByPlacementAndDate(
            BannerPlacement.HOME_CAROUSEL, LocalDateTime.now());

        boolean hasExpired = result.stream().anyMatch(b -> b.getTitle().equals("Expired Banner"));
        boolean hasFuture = result.stream().anyMatch(b -> b.getTitle().equals("Future Banner"));

        assertFalse(hasExpired, "Must not include expired banners");
        assertFalse(hasFuture, "Must not include future banners");
    }

    @Test
    void incrementViewsCount_shouldAtomicallyIncrement() {
        Long id = activeBanner.getId();
        bannerRepository.incrementViewsCount(id);
        bannerRepository.incrementViewsCount(id);
        em.flush();
        em.clear();

        Banner updated = bannerRepository.findById(id).orElseThrow();
        assertEquals(2L, updated.getViewsCount());
    }

    @Test
    void incrementClicksCount_shouldAtomicallyIncrement() {
        Long id = activeBanner.getId();
        bannerRepository.incrementClicksCount(id);
        em.flush();
        em.clear();

        Banner updated = bannerRepository.findById(id).orElseThrow();
        assertEquals(1L, updated.getClicksCount());
    }

    @Test
    void findAllByOrderByBannerOrderAsc_shouldReturnSortedByOrder() {
        List<Banner> result = bannerRepository.findAllByOrderByBannerOrderAsc();

        assertEquals(3, result.size());
        assertTrue(result.get(0).getBannerOrder() <= result.get(1).getBannerOrder());
        assertTrue(result.get(1).getBannerOrder() <= result.get(2).getBannerOrder());
    }

    @Test
    void bannerWithNullDates_shouldAlwaysBeActive() {
        bannerRepository.save(Banner.builder()
            .title("No Date Banner")
            .imageUrl("http://img.com/nodate.png")
            .active(true)
            .placement(BannerPlacement.HOME_CAROUSEL)
            .bannerOrder(0)
            .viewsCount(0L)
            .clicksCount(0L)
            .build());

        List<Banner> result = bannerRepository.findActiveByPlacementAndDate(
            BannerPlacement.HOME_CAROUSEL, LocalDateTime.now());

        assertTrue(result.stream().anyMatch(b -> b.getTitle().equals("No Date Banner")));
    }

    @Test
    void findTop5MostClickedBanners_shouldReturnBannersWithMostClicks() {
        // Banner with more clicks via metrics
        Banner popular = bannerRepository.save(Banner.builder()
            .title("Popular Banner")
            .imageUrl("http://img.com/pop.png")
            .active(true)
            .build());
        
        // Create 2 clicks for popular banner
        em.persist(BannerMetric.builder().banner(popular).type(AdMetricType.CLICK).build());
        em.persist(BannerMetric.builder().banner(popular).type(AdMetricType.CLICK).build());

        // Create 1 click for activeBanner
        em.persist(BannerMetric.builder().banner(activeBanner).type(AdMetricType.CLICK).build());

        em.flush();
        em.clear();

        List<Banner> topBanners = bannerRepository.findTop5MostClickedBanners();
        
        assertFalse(topBanners.isEmpty());
        assertEquals("Popular Banner", topBanners.get(0).getTitle());
    }
}
