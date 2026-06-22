package com.productos.mari.domain.review;

import com.productos.mari.domain.review.ReviewDto;
import com.productos.mari.domain.review.ReviewRequest;
import com.productos.mari.domain.review.ReviewStatsDto;
import com.productos.mari.domain.review.ReviewStatus;
import org.springframework.data.domain.Page;

public interface ReviewService {
    Page<ReviewDto> getProductReviews(Long productId, int page, int size);
    ReviewDto getMyReviewByProduct(Long productId, String email);
    ReviewDto addOrUpdateReview(Long productId, String email, ReviewRequest request);
    void deleteReview(Long reviewId, String email);
    void deleteReviewAsAdmin(Long reviewId);
    Page<ReviewDto> getPendingReviews(int page, int size);
    ReviewDto moderateReview(Long reviewId, ReviewStatus status);
    ReviewStatsDto getReviewStats();
}
