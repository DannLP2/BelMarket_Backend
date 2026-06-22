package com.productos.mari.domain.support;

import com.productos.mari.domain.support.SupportRequestDto;
import com.productos.mari.domain.support.SupportRequest;
import com.productos.mari.domain.support.SupportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportController {

    private final SupportService supportService;

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<Map<String, Object>> createSupportRequest(
            @ModelAttribute SupportRequestDto dto,
            @RequestParam(value = "attachment", required = false) org.springframework.web.multipart.MultipartFile attachment) throws java.io.IOException {

        SupportRequestDto request = supportService.processSupportRequest(dto, attachment);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Ticket de soporte creado correctamente");
        response.put("requestId", request.getId());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin")
    public ResponseEntity<java.util.List<SupportRequestDto>> getAllSupportRequests() {
        return ResponseEntity.ok(supportService.getAllRequests());
    }

    @PatchMapping("/admin/{id}/status")
    public ResponseEntity<SupportRequestDto> updateSupportStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        return ResponseEntity.ok(supportService.updateStatus(id, status));
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Void> deleteSupportRequest(@PathVariable Long id) {
        supportService.deleteRequest(id);
        return ResponseEntity.noContent().build();
    }
}
