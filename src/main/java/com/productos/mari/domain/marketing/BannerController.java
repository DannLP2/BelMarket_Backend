package com.productos.mari.domain.marketing;

import com.productos.mari.domain.marketing.Banner;
import com.productos.mari.domain.marketing.BannerService;
import com.productos.mari.domain.infrastructure.media.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/banners")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;
    private final CloudinaryService cloudinaryService;

    // Public endpoint for home carousel
    @GetMapping
    public ResponseEntity<List<BannerDto>> getActiveBanners() {
        return ResponseEntity.ok(bannerService.getActiveBanners());
    }

    // Public endpoint for specific placements (Sidebar, Detail, etc.)
    @GetMapping("/placement/{placement}")
    public ResponseEntity<List<BannerDto>> getBannersByPlacement(@PathVariable BannerPlacement placement) {
        return ResponseEntity.ok(bannerService.getActiveBannersByPlacement(placement));
    }

    // Public endpoint to track metrics (Views/Clicks)
    @PostMapping("/{id}/track")
    public ResponseEntity<Void> trackMetric(
            @PathVariable Long id, 
            @RequestParam AdMetricType type,
            HttpServletRequest request) {
        String ipAddress = request.getRemoteAddr();
        bannerService.recordMetric(id, type, ipAddress);
        return ResponseEntity.ok().build();
    }

    // Admin endpoints
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BannerDto>> getAllBanners() {
        return ResponseEntity.ok(bannerService.getAllBanners());
    }

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BannerDto> createBanner(@RequestBody BannerDto bannerDto) {
        return ResponseEntity.ok(bannerService.createBanner(bannerDto));
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BannerDto> updateBanner(@PathVariable Long id, @RequestBody BannerDto bannerDto) {
        return ResponseEntity.ok(bannerService.updateBanner(id, bannerDto));
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBanner(@PathVariable Long id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/admin/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> toggleBannerStatus(@PathVariable Long id) {
        bannerService.toggleBannerStatus(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> uploadBanner(@RequestParam("file") MultipartFile file) throws IOException {
        String url = cloudinaryService.uploadFile(file, "belmarket/banners");
        return ResponseEntity.ok(Collections.singletonMap("url", url));
    }
}
