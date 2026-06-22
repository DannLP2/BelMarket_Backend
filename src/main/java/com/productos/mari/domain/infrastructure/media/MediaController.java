package com.productos.mari.domain.infrastructure.media;

import com.productos.mari.domain.infrastructure.media.MediaDto;
import com.productos.mari.domain.infrastructure.media.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MediaController {

    private final MediaService mediaService;

    @GetMapping
    public ResponseEntity<List<MediaDto>> getAllMedia() throws Exception {
        return ResponseEntity.ok(mediaService.getAllMedia());
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteMedia(@RequestParam String publicId) throws IOException {
        mediaService.deleteMedia(publicId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<Void> deleteMediaBulk(@RequestBody List<String> publicIds) throws IOException {
        mediaService.deleteMediaBulk(publicIds);
        return ResponseEntity.noContent().build();
    }
}
