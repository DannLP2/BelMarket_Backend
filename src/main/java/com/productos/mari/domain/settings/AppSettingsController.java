package com.productos.mari.domain.settings;

import com.productos.mari.domain.settings.AppSettings;
import com.productos.mari.domain.settings.AppSettingsService;
import com.productos.mari.domain.infrastructure.media.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class AppSettingsController {

    private final AppSettingsService service;
    private final CloudinaryService cloudinaryService;

    @GetMapping
    public ResponseEntity<AppSettings> getSettings() {
        return ResponseEntity.ok(service.getSettings());
    }

    @PutMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppSettings> updateSettings(@RequestBody AppSettings settings) {
        return ResponseEntity.ok(service.updateSettings(settings));
    }

    @PostMapping("/admin/upload-logo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> uploadLogo(@RequestParam("file") MultipartFile file) throws IOException {
        String url = cloudinaryService.uploadFile(file);
        return ResponseEntity.ok(Collections.singletonMap("url", url));
    }

    @PostMapping("/admin/upload-favicon")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> uploadFavicon(@RequestParam("file") MultipartFile file) throws IOException {
        String url = cloudinaryService.uploadFile(file, "belmarket/general");
        return ResponseEntity.ok(Collections.singletonMap("url", url));
    }

    @PostMapping("/admin/upload-bg")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> uploadBackground(@RequestParam("file") MultipartFile file) throws IOException {
        String url = cloudinaryService.uploadFile(file);
        return ResponseEntity.ok(Collections.singletonMap("url", url));
    }
}
