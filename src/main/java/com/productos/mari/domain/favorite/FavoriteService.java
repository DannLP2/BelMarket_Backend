package com.productos.mari.domain.favorite;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface FavoriteService {
    FavoriteDto toggleFavorite(Long productId);
    Page<FavoriteDto> getUserFavorites(Pageable pageable);
    List<Long> getUserFavoriteProductIds();
    boolean isFavorite(Long productId);
}
