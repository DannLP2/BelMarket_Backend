package com.productos.mari.domain.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.junit.jupiter.api.BeforeEach;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
@Import(com.productos.mari.config.TestSecurityConfig.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class ReviewControllerWebIT {

    private MockMvc mockMvc;
    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // ── GET /api/products/{id}/reviews → PÚBLICO ───────────────

    @Test
    @WithAnonymousUser
    void getReviews_public_shouldReturn200() throws Exception {
        when(reviewService.getProductReviews(eq(1L), anyInt(), anyInt()))
            .thenReturn(new PageImpl<>(List.of(new ReviewDto())));

        mockMvc.perform(get("/api/products/1/reviews"))
            .andExpect(status().isOk());
    }

    // ── POST /api/products/{id}/reviews → AUTENTICADO ──────────

    @Test
    @WithAnonymousUser
    void addReview_anonymous_shouldReturn401() throws Exception {
        ReviewRequest req = new ReviewRequest();
        req.setRating(5);
        req.setComment("Excelente");

        mockMvc.perform(post("/api/products/1/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = "CLIENT")
    void addReview_authenticated_shouldReturn200() throws Exception {
        ReviewRequest req = new ReviewRequest();
        req.setRating(4);
        req.setComment("Muy bueno");

        ReviewDto dto = ReviewDto.builder().id(1L).rating(4).build();
        when(reviewService.addOrUpdateReview(eq(1L), eq("user@test.com"), any(ReviewRequest.class)))
            .thenReturn(dto);

        mockMvc.perform(post("/api/products/1/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk());
    }

    // ── DELETE /api/products/{id}/reviews/{reviewId} → AUTENTICADO ──

    @Test
    @WithMockUser(username = "user@test.com", roles = "CLIENT")
    void deleteReview_asOwner_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/products/1/reviews/5"))
            .andExpect(status().isNoContent());
    }

    // ── DELETE .../reviews/{reviewId}/admin → solo ADMIN ───────

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteReviewAsAdmin_asAdmin_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/products/1/reviews/5/admin"))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void deleteReviewAsAdmin_asClient_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/products/1/reviews/5/admin"))
            .andExpect(status().isForbidden());
    }

    // ── PATCH .../reviews/{reviewId}/moderate → solo ADMIN ─────

    @Test
    @WithMockUser(roles = "ADMIN")
    void moderateReview_asAdmin_shouldReturn200() throws Exception {
        when(reviewService.moderateReview(eq(5L), eq(ReviewStatus.APPROVED)))
            .thenReturn(new ReviewDto());

        mockMvc.perform(patch("/api/products/1/reviews/5/moderate")
                .param("status", "APPROVED"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void moderateReview_asClient_shouldReturn403() throws Exception {
        mockMvc.perform(patch("/api/products/1/reviews/5/moderate")
                .param("status", "APPROVED"))
            .andExpect(status().isForbidden());
    }

    // ── GET .../reviews/admin/stats → solo ADMIN ───────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void getReviewStats_asAdmin_shouldReturn200() throws Exception {
        when(reviewService.getReviewStats()).thenReturn(new ReviewStatsDto());

        mockMvc.perform(get("/api/products/1/reviews/admin/stats"))
            .andExpect(status().isOk());
    }
}
