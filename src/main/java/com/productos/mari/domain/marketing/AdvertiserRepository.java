package com.productos.mari.domain.marketing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AdvertiserRepository extends JpaRepository<Advertiser, Long> {
    List<Advertiser> findByActiveTrueOrderByAdOrderAsc();
    List<Advertiser> findAllByOrderByAdOrderAsc();
    List<Advertiser> findByActiveTrueAndPlacementOrderByAdOrderAsc(AdPlacement placement);
    List<Advertiser> findAllByPlacementOrderByAdOrderAsc(AdPlacement placement);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);

    @Query("SELECT a FROM Advertiser a WHERE " +
           "(:search IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(a.contactName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:hasStatus = false OR " +
           "     (:status = 'ACTIVE' AND a.active = true) OR " +
           "     (:status = 'INACTIVE' AND a.active = false)) " +
           "ORDER BY a.adOrder ASC")
    List<Advertiser> searchAdvertisers(@Param("search") String search, @Param("hasStatus") boolean hasStatus, @Param("status") String status);


    @Modifying
    @Query("UPDATE Advertiser a SET a.viewsCount = a.viewsCount + 1 WHERE a.id = :id")
    void incrementViewsCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Advertiser a SET a.clicksCount = a.clicksCount + 1 WHERE a.id = :id")
    void incrementClicksCount(@Param("id") Long id);
}
