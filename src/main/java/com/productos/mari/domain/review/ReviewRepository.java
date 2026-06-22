package com.productos.mari.domain.review;

import com.productos.mari.domain.product.Product;
import com.productos.mari.domain.review.Review;
import com.productos.mari.domain.review.ReviewStatus;
import com.productos.mari.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByProductAndStatusOrderByCreatedAtDesc(Product product, ReviewStatus status, Pageable pageable);
    Page<Review> findByProductOrderByCreatedAtDesc(Product product, Pageable pageable);
    List<Review> findByProductAndStatus(Product product, ReviewStatus status);
    List<Review> findByProduct(Product product);
    Optional<Review> findByUserAndProduct(User user, Product product);
    long countByProduct(Product product);
    Page<Review> findByStatusOrderByCreatedAtDesc(ReviewStatus status, Pageable pageable);
    long countByStatus(ReviewStatus status);
}
