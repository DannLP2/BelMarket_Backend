package com.productos.mari.domain.mecatronic;

import com.productos.mari.domain.mecatronic.DeviceAction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DeviceActionRepository extends JpaRepository<DeviceAction, Long> {
    List<DeviceAction> findByVariableDeviceIdAndStatusOrderByCreatedAtAsc(Long deviceId, DeviceAction.ActionStatus status);
}
