package com.productos.mari.domain.support;

import com.productos.mari.domain.support.SupportRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportRequestRepository extends JpaRepository<SupportRequest, Long> {
    java.util.List<SupportRequest> findAllByOrderByCreatedAtDesc();
    long countByStatus(String status);
}
