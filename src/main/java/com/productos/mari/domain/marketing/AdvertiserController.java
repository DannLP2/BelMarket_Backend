package com.productos.mari.domain.marketing;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/advertisers")
@RequiredArgsConstructor
public class AdvertiserController {

    private final AdvertiserService advertiserService;

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdvertiserDto>> getAllAdvertisers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(advertiserService.getAllAdvertisers(search, status));
    }

    @GetMapping("/admin/placement/{placement}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdvertiserDto>> getAllAdvertisersByPlacement(@PathVariable AdPlacement placement) {
        return ResponseEntity.ok(advertiserService.getAllAdvertisersByPlacement(placement));
    }

    @GetMapping("/active")
    public ResponseEntity<List<AdvertiserDto>> getActiveAdvertisers() {
        return ResponseEntity.ok(advertiserService.getActiveAdvertisers());
    }

    @GetMapping("/active/placement/{placement}")
    public ResponseEntity<List<AdvertiserDto>> getActiveAdvertisersByPlacement(@PathVariable AdPlacement placement) {
        return ResponseEntity.ok(advertiserService.getActiveAdvertisersByPlacement(placement));
    }

    // Public endpoint to track metrics (Views/Clicks)
    @PostMapping("/{id}/track")
    public ResponseEntity<Void> trackMetric(
            @PathVariable Long id, 
            @RequestParam AdMetricType type,
            jakarta.servlet.http.HttpServletRequest request) {
        String ipAddress = request.getRemoteAddr();
        advertiserService.recordAdMetric(id, type, ipAddress);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdvertiserDto> createAdvertiser(@RequestBody AdvertiserDto advertiserDto) {
        return ResponseEntity.ok(advertiserService.createAdvertiser(advertiserDto));
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdvertiserDto> updateAdvertiser(@PathVariable Long id, @RequestBody AdvertiserDto advertiserDto) {
        return ResponseEntity.ok(advertiserService.updateAdvertiser(id, advertiserDto));
    }

    @PutMapping("/admin/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reorderAds(@RequestBody List<Long> rankedIds) {
        advertiserService.reorderAds(rankedIds);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAdvertiser(@PathVariable Long id) {
        advertiserService.deleteAdvertiser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/admin/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> toggleAdvertiserStatus(@PathVariable Long id) {
        advertiserService.toggleAdvertiserStatus(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> uploadLogo(@RequestParam("file") MultipartFile file) throws IOException {
        String url = advertiserService.uploadLogo(file);
        return ResponseEntity.ok(Collections.singletonMap("url", url));
    }
}
