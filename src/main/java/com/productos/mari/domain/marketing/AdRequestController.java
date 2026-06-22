package com.productos.mari.domain.marketing;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/ad-requests")
@RequiredArgsConstructor
public class AdRequestController {

    private final AdRequestService adRequestService;
    private final com.productos.mari.domain.infrastructure.media.CloudinaryService cloudinaryService;

    @PostMapping("/public")
    public ResponseEntity<AdRequest> submitRequest(@RequestBody AdRequest request) {
        return ResponseEntity.ok(adRequestService.createRequest(request));
    }

    @PostMapping("/public/upload")
    public ResponseEntity<java.util.Map<String, String>> uploadPautaFile(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        String url = cloudinaryService.uploadFile(file, "belmarket/pauta-requests");
        return ResponseEntity.ok(java.util.Collections.singletonMap("url", url));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdRequest>> getAllRequests(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(adRequestService.getAllRequests(search, status));
    }

    @PatchMapping("/admin/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdRequest> updateRequestStatus(
            @PathVariable Long id, 
            @RequestParam AdRequestStatus status) {
        return ResponseEntity.ok(adRequestService.updateStatus(id, status));
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRequest(@PathVariable Long id) {
        adRequestService.deleteRequest(id);
        return ResponseEntity.noContent().build();
    }
}
