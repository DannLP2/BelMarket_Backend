package com.productos.mari.domain.review;

import com.productos.mari.domain.review.ReviewDto;
import com.productos.mari.domain.review.ReviewRequest;
import com.productos.mari.domain.review.ReviewStatsDto;
import com.productos.mari.domain.notification.NotificationCategory;
import com.productos.mari.domain.product.Product;
import com.productos.mari.domain.reservation.ReservationStatus;
import com.productos.mari.domain.review.Review;
import com.productos.mari.domain.review.ReviewStatus;
import com.productos.mari.domain.user.User;
import com.productos.mari.domain.product.ProductRepository;
import com.productos.mari.domain.reservation.ReservationRepository;
import com.productos.mari.domain.review.ReviewRepository;
import com.productos.mari.domain.user.UserRepository;
import com.productos.mari.domain.review.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final com.productos.mari.domain.notification.NotificationService notificationService;
    private final com.productos.mari.domain.infrastructure.audit.SecurityAuditService securityAuditService;
    private final ReviewMapper reviewMapper;

    @Override
    public Page<ReviewDto> getProductReviews(Long productId, int page, int size) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        Pageable pageable = PageRequest.of(page, size);
        // Only return APPROVED reviews publicly
        return reviewRepository.findByProductAndStatusOrderByCreatedAtDesc(product, ReviewStatus.APPROVED, pageable)
                .map(reviewMapper::toDto);
    }

    @Override
    public ReviewDto getMyReviewByProduct(Long productId, String email) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Optional<Review> review = reviewRepository.findByUserAndProduct(user, product);
        return review.map(reviewMapper::toDto).orElse(null);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ReviewDto addOrUpdateReview(Long productId, String email, ReviewRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Validate that user has purchased the product
        boolean hasPurchased = reservationRepository.existsByUserAndStatusAndProductInItems(
                user, ReservationStatus.COMPLETED, product);

        if (!hasPurchased) {
            throw new IllegalArgumentException("Solo puedes calificar productos que has comprado.");
        }
        
        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new IllegalArgumentException("La calificación debe estar entre 1 y 5.");
        }

        Review review = reviewRepository.findByUserAndProduct(user, product).orElse(new Review());
        review.setUser(user);
        review.setProduct(product);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        // Reset to PENDING on each update so admin re-reviews it
        review.setStatus(ReviewStatus.PENDING);

        Review savedReview = reviewRepository.save(review);
        
        // Notify Admins for moderation
        notifyAdmins(
            "NUEVA RESEÑA RECIBIDA",
            "Usuario: " + user.getName() + " calificó '" + product.getName() + "' con " + savedReview.getRating() + " estrellas.",
            "rate_review",
            "/admin/reviews",
            NotificationCategory.INFO
        );

        return reviewMapper.toDto(savedReview);
    }

    private void notifyAdmins(String title, String description, String icon, String link, NotificationCategory category) {
        notificationService.broadcastNotification(title, description, icon, link, category, true);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void deleteReview(Long reviewId, String email) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Reseña no encontrada"));
        
        if (!review.getUser().getEmail().equals(email)) {
             throw new IllegalArgumentException("No autorizado");
        }
        
        Product product = review.getProduct();
        reviewRepository.delete(review);
        if (review.getStatus() == ReviewStatus.APPROVED) {
            updateProductStats(product);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void deleteReviewAsAdmin(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Reseña no encontrada"));
        Product product = review.getProduct();
        boolean wasApproved = review.getStatus() == ReviewStatus.APPROVED;
        
        String currentAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        securityAuditService.log(
            com.productos.mari.domain.auth.SecurityAction.REVIEW_DELETED,
            null,
            currentAdmin,
            "Reseña eliminada por administrador (ID: " + reviewId + ") del producto: " + product.getName()
        );

        reviewRepository.delete(review);
        if (wasApproved) {
            updateProductStats(product);
        }
    }

    @Override
    public Page<ReviewDto> getPendingReviews(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return reviewRepository.findByStatusOrderByCreatedAtDesc(ReviewStatus.PENDING, pageable)
                .map(reviewMapper::toDto);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ReviewDto moderateReview(Long reviewId, ReviewStatus status) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Reseña no encontrada"));

        ReviewStatus previousStatus = review.getStatus();
        review.setStatus(status);
        Review saved = reviewRepository.save(review);

        String currentAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        securityAuditService.log(
            status == ReviewStatus.APPROVED ? com.productos.mari.domain.auth.SecurityAction.REVIEW_APPROVED : com.productos.mari.domain.auth.SecurityAction.REVIEW_REJECTED,
            null,
            currentAdmin,
            "Reseña moderada a estado " + status + " (ID: " + reviewId + ")"
        );

        // Recalculate product stats only when status changes to/from APPROVED
        boolean wasApproved = previousStatus == ReviewStatus.APPROVED;
        boolean isNowApproved = status == ReviewStatus.APPROVED;
        if (wasApproved != isNowApproved) {
            updateProductStats(saved.getProduct());
        }

        return reviewMapper.toDto(saved);
    }

    private void updateProductStats(Product product) {
        // Only count APPROVED reviews for ratings
        List<Review> approvedReviews = reviewRepository.findByProductAndStatus(product, ReviewStatus.APPROVED);
        if (approvedReviews.isEmpty()) {
            product.setAverageRating(0.0);
            product.setReviewCount(0);
        } else {
            double sum = approvedReviews.stream().mapToDouble(Review::getRating).sum();
            double avg = sum / approvedReviews.size();
            product.setAverageRating(Math.round(avg * 10.0) / 10.0);
            product.setReviewCount(approvedReviews.size());
        }
        productRepository.save(product);
    }

    // Manual mapping removed in favor of MapStruct

    @Override
    public ReviewStatsDto getReviewStats() {
        long totalApproved = reviewRepository.countByStatus(ReviewStatus.APPROVED);
        long totalPending = reviewRepository.countByStatus(ReviewStatus.PENDING);
        
        // Calculate distribution
        Map<Integer, Long> distribution = new HashMap<>();
        double sum = 0;
        
        List<Review> approvedReviews = reviewRepository.findAll().stream()
                .filter(r -> r.getStatus() == ReviewStatus.APPROVED)
                .collect(Collectors.toList());

        for (int i = 1; i <= 5; i++) {
            int rating = i;
            long count = approvedReviews.stream().filter(r -> r.getRating() == rating).count();
            distribution.put(rating, count);
        }

        if (totalApproved > 0) {
            sum = approvedReviews.stream().mapToDouble(Review::getRating).sum();
        }

        double average = totalApproved > 0 ? (Math.round((sum / totalApproved) * 10.0) / 10.0) : 0.0;

        return ReviewStatsDto.builder()
                .averageRating(average)
                .totalReviews(totalApproved)
                .pendingReviews(totalPending)
                .ratingDistribution(distribution)
                .build();
    }
}
