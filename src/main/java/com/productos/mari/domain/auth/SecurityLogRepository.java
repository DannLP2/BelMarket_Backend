package com.productos.mari.domain.auth;

import com.productos.mari.domain.auth.SecurityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SecurityLogRepository extends JpaRepository<SecurityLog, Long> {
    
    List<SecurityLog> findTop100ByOrderByTimestampDesc();
    
    @Query("SELECT COUNT(s) FROM SecurityLog s WHERE s.timestamp > :after")
    long countLogsSince(@Param("after") LocalDateTime after);

    @Query("SELECT COUNT(s) FROM SecurityLog s WHERE s.timestamp > :after AND s.action IN (com.productos.mari.domain.auth.SecurityAction.LOGIN_FAILED, com.productos.mari.domain.auth.SecurityAction.UNAUTHORIZED_ACCESS, com.productos.mari.domain.auth.SecurityAction.RATE_LIMIT_EXCEEDED)")
    long countAlertsSince(@Param("after") LocalDateTime after);

    @Query("SELECT s.ipAddress, COUNT(s) FROM SecurityLog s WHERE s.action IN (com.productos.mari.domain.auth.SecurityAction.LOGIN_FAILED, com.productos.mari.domain.auth.SecurityAction.UNAUTHORIZED_ACCESS, com.productos.mari.domain.auth.SecurityAction.RATE_LIMIT_EXCEEDED, com.productos.mari.domain.auth.SecurityAction.VERIFY_FAILED) GROUP BY s.ipAddress HAVING COUNT(s) > 0 ORDER BY COUNT(s) DESC")
    List<Object[]> findTopSuspiciousIPs();

    @Query("SELECT COUNT(s) FROM SecurityLog s WHERE s.ipAddress = :ip AND s.action = :action AND s.timestamp > :after")
    long countByIpAndActionAfter(@Param("ip") String ip, @Param("action") com.productos.mari.domain.auth.SecurityAction action, @Param("after") LocalDateTime after);

    @Query("SELECT s FROM SecurityLog s WHERE " +
           "(:email IS NULL OR s.email LIKE %:email%) AND " +
           "(:action IS NULL OR s.action = :action) AND " +
           "(:ip IS NULL OR s.ipAddress LIKE %:ip%) AND " +
           "(:start IS NULL OR s.timestamp >= :start) AND " +
           "(:end IS NULL OR s.timestamp <= :end)")
    org.springframework.data.domain.Page<SecurityLog> searchLogs(
        @Param("email") String email, 
        @Param("action") com.productos.mari.domain.auth.SecurityAction action, 
        @Param("ip") String ip, 
        @Param("start") LocalDateTime start, 
        @Param("end") LocalDateTime end, 
        org.springframework.data.domain.Pageable pageable
    );

    void deleteByTimestampBefore(LocalDateTime threshold);
}
