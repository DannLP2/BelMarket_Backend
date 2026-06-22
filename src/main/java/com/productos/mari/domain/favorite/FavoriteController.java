package com.productos.mari.domain.favorite;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/toggle/{productId}")
    public ResponseEntity<FavoriteDto> toggleFavorite(@PathVariable Long productId) {
        return ResponseEntity.ok(favoriteService.toggleFavorite(productId));
    }

    @GetMapping
    public ResponseEntity<Page<FavoriteDto>> getMyFavorites(
            @PageableDefault(size = 12) Pageable pageable) {
        return ResponseEntity.ok(favoriteService.getUserFavorites(pageable));
    }

    @GetMapping("/ids")
    public ResponseEntity<List<Long>> getMyFavoriteIds() {
        return ResponseEntity.ok(favoriteService.getUserFavoriteProductIds());
    }

    @GetMapping("/check/{productId}")
    public ResponseEntity<Boolean> isFavorite(@PathVariable Long productId) {
        return ResponseEntity.ok(favoriteService.isFavorite(productId));
    }
}
