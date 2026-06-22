package com.productos.mari.domain.mecatronic;

import com.productos.mari.domain.mecatronic.MecatronicDashboardDto;
import com.productos.mari.domain.mecatronic.BindDeviceDto;
import com.productos.mari.domain.mecatronic.DeviceVariable;
import com.productos.mari.domain.mecatronic.MecatronicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mecatronic")
@RequiredArgsConstructor
public class MecatronicController {

    private final MecatronicService mecatronicService;

    // ESP32 Telemetry ingestion
    @PostMapping("/telemetry")
    public ResponseEntity<Void> receiveTelemetry(
            @RequestParam String apiKey,
            @RequestBody Map<String, String> data) {
        mecatronicService.processTelemetry(apiKey, data);
        return ResponseEntity.ok().build();
    }

    // ESP32 Command polling
    @GetMapping("/commands")
    public ResponseEntity<Map<String, String>> getCommands(@RequestParam String apiKey) {
        return ResponseEntity.ok(mecatronicService.getPendingCommands(apiKey));
    }

    // Dashboard data
    @GetMapping("/dashboard/{productId}")
    public ResponseEntity<MecatronicDashboardDto> getDashboard(@PathVariable Long productId) {
        return ResponseEntity.ok(mecatronicService.getDashboard(productId));
    }

    // Send action from dashboard
    @PostMapping("/action/{variableId}")
    public ResponseEntity<Void> sendAction(
            @PathVariable Long variableId,
            @RequestParam String value) {
        mecatronicService.sendCommand(variableId, value);
        return ResponseEntity.ok().build();
    }

    // Configure new variable (Admin only)
    @PostMapping("/admin/configure/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> addVariable(
            @PathVariable Long productId,
            @RequestBody DeviceVariable variable) {
        mecatronicService.addVariable(productId, variable);
        return ResponseEntity.ok().build();
    }

    // Get purchased devices for current user
    @GetMapping("/my-devices")
    public ResponseEntity<java.util.List<MecatronicDashboardDto>> getMyDevices(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.productos.mari.domain.user.User user) {
        return ResponseEntity.ok(mecatronicService.getMyDevices(user.getId(), q, type, status));
    }

    // Bind a device for the first time
    @PostMapping("/bind/{productId}")
    public ResponseEntity<Void> bindDevice(
            @PathVariable Long productId,
            @RequestBody BindDeviceDto request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.productos.mari.domain.user.User user) {
        mecatronicService.bindDevice(productId, user.getId(), request.getSerial(), request.getPin());
        return ResponseEntity.ok().build();
    }

    // Bind a device that was purchased externally
    @PostMapping("/bind-external/{productId}")
    public ResponseEntity<Void> bindExternalDevice(
            @PathVariable Long productId,
            @RequestBody BindDeviceDto request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.productos.mari.domain.user.User user) {
        mecatronicService.bindExternalDevice(productId, user.getId(), request.getSerial(), request.getPin());
        return ResponseEntity.ok().build();
    }
}
