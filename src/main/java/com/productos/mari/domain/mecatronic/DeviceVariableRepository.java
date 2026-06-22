package com.productos.mari.domain.mecatronic;

import com.productos.mari.domain.mecatronic.DeviceVariable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DeviceVariableRepository extends JpaRepository<DeviceVariable, Long> {
    List<DeviceVariable> findByDeviceId(Long deviceId);
    Optional<DeviceVariable> findByDeviceIdAndFieldKey(Long deviceId, String fieldKey);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByDeviceId(Long deviceId);
}
