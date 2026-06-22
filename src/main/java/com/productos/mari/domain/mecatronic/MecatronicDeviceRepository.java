package com.productos.mari.domain.mecatronic;

import com.productos.mari.domain.mecatronic.MecatronicDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MecatronicDeviceRepository extends JpaRepository<MecatronicDevice, Long> {
    Optional<MecatronicDevice> findByApiKey(String apiKey);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"product", "variables"})
    Optional<MecatronicDevice> findByProductId(Long productId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"product", "variables"})
    java.util.List<MecatronicDevice> findAllByProductIdIn(java.util.List<Long> productIds);
}
