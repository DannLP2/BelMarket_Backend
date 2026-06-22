package com.productos.mari.domain.review;

import com.productos.mari.domain.product.Product;
import com.productos.mari.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
class ReviewRepositoryIT {

    @Autowired private ReviewRepository reviewRepository;
    @Autowired private EntityManager em;

    private Product product;
    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        // Persist supporting entities via EntityManager (bypass their own repos)
        product = Product.builder()
            .name("Producto Test")
            .slug("producto-test")
            .price(BigDecimal.valueOf(10000))
            .stock(10)
            .build();
        em.persist(product);

        user1 = User.builder()
            .name("User One")
            .email("user1@test.com")
            .password("pass")
            .roles(new java.util.HashSet<>(java.util.Set.of(com.productos.mari.domain.user.Role.CLIENT)))
            .enabled(true)
            .build();
        em.persist(user1);

        user2 = User.builder()
            .name("User Two")
            .email("user2@test.com")
            .password("pass")
            .roles(new java.util.HashSet<>(java.util.Set.of(com.productos.mari.domain.user.Role.CLIENT)))
            .enabled(true)
            .build();
        em.persist(user2);

        em.flush();

        // Create reviews
        reviewRepository.save(Review.builder()
            .user(user1).product(product).rating(5)
            .comment("Excelente").status(ReviewStatus.APPROVED).build());

        reviewRepository.save(Review.builder()
            .user(user2).product(product).rating(3)
            .comment("Regular").status(ReviewStatus.PENDING).build());
    }

    @Test
    void findByProductAndStatusOrderByCreatedAtDesc_shouldOnlyReturnApproved() {
        Page<Review> result = reviewRepository.findByProductAndStatusOrderByCreatedAtDesc(
            product, ReviewStatus.APPROVED, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals(5, result.getContent().get(0).getRating());
    }

    @Test
    void findByUserAndProduct_shouldReturnExistingReview() {
        Optional<Review> result = reviewRepository.findByUserAndProduct(user1, product);

        assertTrue(result.isPresent());
        assertEquals("Excelente", result.get().getComment());
    }

    @Test
    void findByUserAndProduct_shouldReturnEmptyIfNoReview() {
        User newUser = User.builder()
            .name("No Review User").email("norv@test.com").password("pass")
            .roles(new java.util.HashSet<>(java.util.Set.of(com.productos.mari.domain.user.Role.CLIENT)))
            .enabled(true).build();
        em.persist(newUser);
        em.flush();

        Optional<Review> result = reviewRepository.findByUserAndProduct(newUser, product);
        assertFalse(result.isPresent());
    }

    @Test
    void countByStatus_shouldReturnCorrectCounts() {
        assertEquals(1L, reviewRepository.countByStatus(ReviewStatus.APPROVED));
        assertEquals(1L, reviewRepository.countByStatus(ReviewStatus.PENDING));
    }

    @Test
    void findByProductAndStatus_shouldReturnAllApprovedForProduct() {
        List<Review> approved = reviewRepository.findByProductAndStatus(product, ReviewStatus.APPROVED);
        assertEquals(1, approved.size());
        assertEquals(5, approved.get(0).getRating());
    }

    @Test
    void findByStatusOrderByCreatedAtDesc_shouldReturnPaginatedPending() {
        Page<Review> pending = reviewRepository.findByStatusOrderByCreatedAtDesc(
            ReviewStatus.PENDING, PageRequest.of(0, 10));
        assertEquals(1, pending.getTotalElements());
        assertEquals("Regular", pending.getContent().get(0).getComment());
    }
}
