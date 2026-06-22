package com.productos.mari.domain.favorite;

import com.productos.mari.domain.product.Product;
import com.productos.mari.domain.product.ProductRepository;
import com.productos.mari.domain.user.User;
import com.productos.mari.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final FavoriteMapper favoriteMapper;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado o no autenticado."));
    }

    @Override
    @Transactional
    public FavoriteDto toggleFavorite(Long productId) {
        User user = getCurrentUser();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con el ID: " + productId));

        if (favoriteRepository.existsByUserAndProduct(user, product)) {
            favoriteRepository.deleteByUserAndProduct(user, product);
            log.info("Eliminado favorito: Usuario {}, Producto {}", user.getEmail(), product.getName());
            return null; // Return null to indicate removal
        } else {
            Favorite favorite = Favorite.builder()
                    .user(user)
                    .product(product)
                    .build();
            Favorite saved = favoriteRepository.save(favorite);
            log.info("Agregado favorito: Usuario {}, Producto {}", user.getEmail(), product.getName());
            return favoriteMapper.toDto(saved);
        }
    }

    @Override
    public Page<FavoriteDto> getUserFavorites(Pageable pageable) {
        User user = getCurrentUser();
        return favoriteRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(favoriteMapper::toDto);
    }

    @Override
    public List<Long> getUserFavoriteProductIds() {
        User user = getCurrentUser();
        return favoriteRepository.findByUser(user).stream()
                .map(f -> f.getProduct().getId())
                .collect(Collectors.toList());
    }

    @Override
    public boolean isFavorite(Long productId) {
        User user = getCurrentUser();
        Product product = productRepository.getReferenceById(productId);
        return favoriteRepository.existsByUserAndProduct(user, product);
    }
}
