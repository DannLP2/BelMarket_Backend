package com.productos.mari.domain.marketing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AdRequestRepository extends JpaRepository<AdRequest, Long> {
    List<AdRequest> findAllByOrderByCreatedAtDesc();
    List<AdRequest> findByStatusOrderByCreatedAtDesc(AdRequestStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT r FROM AdRequest r WHERE " +
           "(:search IS NULL OR LOWER(r.companyName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(r.contactName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:hasStatus = false OR r.status = :statusEnum) " +
           "ORDER BY r.createdAt DESC")
    List<AdRequest> searchRequests(@org.springframework.data.repository.query.Param("search") String search, @org.springframework.data.repository.query.Param("hasStatus") boolean hasStatus, @org.springframework.data.repository.query.Param("statusEnum") AdRequestStatus statusEnum);
}
