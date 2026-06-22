package com.productos.mari.domain.marketing;

import com.productos.mari.domain.marketing.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {
    List<Banner> findByActiveTrueOrderByBannerOrderAsc();

    List<Banner> findAllByOrderByBannerOrderAsc();

    @Query("SELECT b FROM Banner b WHERE b.active = true AND b.placement = :placement " +
           "AND (b.startDate IS NULL OR b.startDate <= :now) " +
           "AND (b.endDate IS NULL OR b.endDate >= :now) " +
           "ORDER BY b.bannerOrder ASC")
    List<Banner> findActiveByPlacementAndDate(
        @Param("placement") BannerPlacement placement, 
        @Param("now") LocalDateTime now
    );

    @Modifying
    @Query("UPDATE Banner b SET b.viewsCount = b.viewsCount + 1 WHERE b.id = :id")
    void incrementViewsCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Banner b SET b.clicksCount = b.clicksCount + 1 WHERE b.id = :id")
    void incrementClicksCount(@Param("id") Long id);

    @Query("SELECT b.imageUrl FROM Banner b")
    List<String> findAllImageUrls();

    @Query(value = "SELECT b.* FROM banners b " +
           "JOIN banner_metrics m ON m.banner_id = b.id " +
           "WHERE b.active = true AND m.type = 'CLICK' " +
           "GROUP BY b.id " +
           "ORDER BY COUNT(m.id) DESC LIMIT 5", nativeQuery = true)
    List<Banner> findTop5MostClickedBanners();
}
