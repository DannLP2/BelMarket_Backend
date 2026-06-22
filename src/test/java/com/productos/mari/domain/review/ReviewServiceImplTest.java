package com.productos.mari.domain.review;

import com.productos.mari.domain.infrastructure.audit.SecurityAuditService;
import com.productos.mari.domain.notification.NotificationService;
import com.productos.mari.domain.product.Product;
import com.productos.mari.domain.product.ProductRepository;
import com.productos.mari.domain.reservation.ReservationRepository;
import com.productos.mari.domain.reservation.ReservationStatus;
import com.productos.mari.domain.user.User;
import com.productos.mari.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private NotificationService notificationService;
    @Mock private SecurityAuditService securityAuditService;
    @Mock private ReviewMapper reviewMapper;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User mockUser;
    private Product mockProduct;
    private Review mockReview;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(1L).email("user@test.com").name("Test User").build();
        mockProduct = Product.builder().id(10L).name("Test Product").build();
        mockReview = new Review();
        mockReview.setId(100L);
        mockReview.setUser(mockUser);
        mockReview.setProduct(mockProduct);
        mockReview.setRating(5);
        mockReview.setStatus(ReviewStatus.APPROVED);
    }

    @Test
    void getProductReviews_ReturnsPage() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(mockProduct));
        when(reviewRepository.findByProductAndStatusOrderByCreatedAtDesc(eq(mockProduct), eq(ReviewStatus.APPROVED), any()))
                .thenReturn(new PageImpl<>(List.of(mockReview)));
        when(reviewMapper.toDto(any())).thenReturn(new ReviewDto());

        Page<ReviewDto> result = reviewService.getProductReviews(10L, 0, 10);

        assertFalse(result.isEmpty());
    }

    @Test
    void addOrUpdateReview_Success() {
        ReviewRequest request = new ReviewRequest();
        request.setRating(5);
        request.setComment("Great!");

        when(productRepository.findById(10L)).thenReturn(Optional.of(mockProduct));
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(mockUser));
        when(reservationRepository.existsByUserAndStatusAndProductInItems(eq(mockUser), eq(ReservationStatus.COMPLETED), eq(mockProduct)))
                .thenReturn(true);
        when(reviewRepository.findByUserAndProduct(mockUser, mockProduct)).thenReturn(Optional.empty());
        when(reviewRepository.save(any())).thenReturn(mockReview);
        when(reviewMapper.toDto(any())).thenReturn(new ReviewDto());

        ReviewDto result = reviewService.addOrUpdateReview(10L, "user@test.com", request);

        assertNotNull(result);
        verify(notificationService).broadcastNotification(any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void addOrUpdateReview_NotPurchased_ThrowsException() {
        ReviewRequest request = new ReviewRequest();
        request.setRating(5);

        when(productRepository.findById(10L)).thenReturn(Optional.of(mockProduct));
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(mockUser));
        when(reservationRepository.existsByUserAndStatusAndProductInItems(any(), any(), any())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> 
            reviewService.addOrUpdateReview(10L, "user@test.com", request));
    }

    @Test
    void moderateReview_UpdatesStatsOnStatusChange() {
        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(auth.getName()).thenReturn("admin@test.com");
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        mockReview.setStatus(ReviewStatus.PENDING);
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(mockReview));
        when(reviewRepository.save(any())).thenReturn(mockReview);
        when(reviewRepository.findByProductAndStatus(eq(mockProduct), eq(ReviewStatus.APPROVED)))
                .thenReturn(List.of(mockReview));
        when(reviewMapper.toDto(any())).thenReturn(new ReviewDto());

        reviewService.moderateReview(100L, ReviewStatus.APPROVED);

        assertEquals(ReviewStatus.APPROVED, mockReview.getStatus());
        verify(productRepository).save(mockProduct);
        verify(securityAuditService).log(any(), any(), anyString(), anyString());
    }

    @Test
    void deleteReview_SuccessAsOwner() {
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(mockReview));
        
        reviewService.deleteReview(100L, "user@test.com");

        verify(reviewRepository).delete(mockReview);
        verify(productRepository).save(mockProduct);
    }

    @Test
    void deleteReview_Forbidden_ThrowsException() {
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(mockReview));

        assertThrows(IllegalArgumentException.class, () -> 
            reviewService.deleteReview(100L, "wrong@test.com"));
    }

    @Test
    void getReviewStats_CalculatesCorrectly() {
        when(reviewRepository.countByStatus(ReviewStatus.APPROVED)).thenReturn(1L);
        when(reviewRepository.countByStatus(ReviewStatus.PENDING)).thenReturn(0L);
        when(reviewRepository.findAll()).thenReturn(List.of(mockReview));

        ReviewStatsDto stats = reviewService.getReviewStats();

        assertEquals(1, stats.getTotalReviews());
        assertEquals(5.0, stats.getAverageRating());
        assertEquals(1L, stats.getRatingDistribution().get(5));
    }

    @Test
    void moderateReview_RecalculatesAverage_WithMultipleRatings() {
        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(auth.getName()).thenReturn("admin@test.com");
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        // Current approved reviews: one with 5 stars, one with 4 stars
        Review otherReview = new Review();
        otherReview.setRating(4);
        otherReview.setStatus(ReviewStatus.APPROVED);

        mockReview.setStatus(ReviewStatus.PENDING);
        mockReview.setRating(5);

        when(reviewRepository.findById(100L)).thenReturn(Optional.of(mockReview));
        when(reviewRepository.save(any())).thenReturn(mockReview);
        // Returns both when approved
        when(reviewRepository.findByProductAndStatus(eq(mockProduct), eq(ReviewStatus.APPROVED)))
                .thenReturn(List.of(mockReview, otherReview));
        when(reviewMapper.toDto(any())).thenReturn(new ReviewDto());

        reviewService.moderateReview(100L, ReviewStatus.APPROVED);

        // (5 + 4) / 2 = 4.5
        assertEquals(4.5, mockProduct.getAverageRating());
        assertEquals(2, mockProduct.getReviewCount());
        verify(productRepository).save(mockProduct);
    }

    @Test
    void deleteReview_ResetsStats_WhenNoReviewsLeft() {
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(mockReview));
        when(reviewRepository.findByProductAndStatus(eq(mockProduct), eq(ReviewStatus.APPROVED)))
                .thenReturn(List.of()); // No reviews left after deletion
        
        reviewService.deleteReview(100L, "user@test.com");

        assertEquals(0.0, mockProduct.getAverageRating());
        assertEquals(0, mockProduct.getReviewCount());
        verify(productRepository).save(mockProduct);
    }
}
