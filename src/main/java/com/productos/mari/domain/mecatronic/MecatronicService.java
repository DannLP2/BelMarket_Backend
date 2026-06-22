package com.productos.mari.domain.mecatronic;

import com.productos.mari.domain.mecatronic.MecatronicDashboardDto;
import com.productos.mari.domain.mecatronic.DeviceVariable;
import java.util.List;
import java.util.Map;

public interface MecatronicService {
    void processTelemetry(String apiKey, Map<String, String> data);
    Map<String, String> getPendingCommands(String apiKey);
    MecatronicDashboardDto getDashboard(Long productId);
    void addVariable(Long productId, DeviceVariable variable);
    void sendCommand(Long variableId, String value);
    List<MecatronicDashboardDto> getMyDevices(Long userId, String q, String type, String status);
    void bindDevice(Long productId, Long userId, String serial, String pin);
    void bindExternalDevice(Long productId, Long userId, String serial, String pin);
}
