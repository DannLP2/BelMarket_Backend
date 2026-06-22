package com.productos.mari.domain.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReviewStatsDto {
    private double averageRating;
    private long totalReviews;
    private long pendingReviews;
    private Map<Integer, Long> ratingDistribution; // Star rating (1-5) -> Count
}
