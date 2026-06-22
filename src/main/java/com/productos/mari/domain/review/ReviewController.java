package com.productos.mari.domain.review;

import com.productos.mari.domain.review.ReviewDto;
import com.productos.mari.domain.review.ReviewRequest;
import com.productos.mari.domain.review.ReviewStatsDto;
import com.productos.mari.domain.review.ReviewStatus;
import com.productos.mari.domain.review.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<Page<ReviewDto>> getReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(reviewService.getProductReviews(productId, page, size));
    }

    @GetMapping("/my")
    public ResponseEntity<ReviewDto> getMyReview(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.ok(null);
        }
        return ResponseEntity.ok(reviewService.getMyReviewByProduct(productId, userDetails.getUsername()));
    }

    @PostMapping
    public ResponseEntity<?> addOrUpdateReview(
            @PathVariable Long productId,
            @RequestBody ReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            return ResponseEntity.ok(reviewService.addOrUpdateReview(productId, userDetails.getUsername(), request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> deleteReview(
            @PathVariable Long productId,
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            reviewService.deleteReview(reviewId, userDetails.getUsername());
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{reviewId}/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteReviewAsAdmin(
            @PathVariable Long productId,
            @PathVariable Long reviewId
    ) {
        reviewService.deleteReviewAsAdmin(reviewId);
        return ResponseEntity.noContent().build();
    }

    // ─── Admin Moderation ───────────────────────────────

    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ReviewDto>> getPendingReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(reviewService.getPendingReviews(page, size));
    }

    @PatchMapping("/{reviewId}/moderate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReviewDto> moderateReview(
            @PathVariable Long productId,
            @PathVariable Long reviewId,
            @RequestParam ReviewStatus status
    ) {
        return ResponseEntity.ok(reviewService.moderateReview(reviewId, status));
    }

    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReviewStatsDto> getReviewStats(
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(reviewService.getReviewStats());
    }
}
