package com.productos.mari.domain.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.productos.mari.domain.review.ReviewStatus;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReviewDto {
    private Long id;
    private Long productId;
    private String productName;
    private String userName;
    private Long userId;
    private String userPictureUrl;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;
    private ReviewStatus status;
}
