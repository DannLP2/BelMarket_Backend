package com.productos.mari.domain.favorite;

import com.productos.mari.domain.product.Product;
import com.productos.mari.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    
    Page<Favorite> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    List<Favorite> findByUser(User user);
    
    Optional<Favorite> findByUserAndProduct(User user, Product product);
    
    boolean existsByUserAndProduct(User user, Product product);
    
    void deleteByUserAndProduct(User user, Product product);
}
