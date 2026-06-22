package com.productos.mari.domain.mecatronic;

import com.productos.mari.domain.mecatronic.VariableReading;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface VariableReadingRepository extends JpaRepository<VariableReading, Long> {
    /**
     * Optimized Bulk-load: Trae las últimas 10 lecturas de CADA variable del device.
     * Utiliza una Native Query con window functions (MySQL 8+) para evitar el N+1 
     * y el problema de "Traer todo a memoria".
     */
    @Query(value = "SELECT * FROM (" +
                   "  SELECT r.*, ROW_NUMBER() OVER (PARTITION BY r.variable_id ORDER BY r.timestamp DESC) as rn " +
                   "  FROM variable_readings r " +
                   "  JOIN device_variables v ON r.variable_id = v.id " +
                   "  WHERE v.device_id = :deviceId" +
                   ") as ranked " +
                   "WHERE rn <= 10", nativeQuery = true)
    List<VariableReading> findTop10ReadingsPerVariableByDeviceId(@Param("deviceId") Long deviceId);

    /**
     * Versión para múltiples dispositivos (Dashboard de usuario)
     */
    @Query(value = "SELECT * FROM (" +
                   "  SELECT r.*, ROW_NUMBER() OVER (PARTITION BY r.variable_id ORDER BY r.timestamp DESC) as rn " +
                   "  FROM variable_readings r " +
                   "  JOIN device_variables v ON r.variable_id = v.id " +
                   "  WHERE v.device_id IN (:deviceIds)" +
                   ") as ranked " +
                   "WHERE rn <= 10", nativeQuery = true)
    List<VariableReading> findTop10ReadingsPerVariableByDeviceIds(@Param("deviceIds") List<Long> deviceIds);
}
