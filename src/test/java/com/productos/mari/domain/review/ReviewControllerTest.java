package com.productos.mari.domain.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewController reviewController;

    private UserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        mockUserDetails = mock(UserDetails.class);
        when(mockUserDetails.getUsername()).thenReturn("user@test.com");

        HandlerMethodArgumentResolver userResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().equals(UserDetails.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                        NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return mockUserDetails;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(reviewController)
                .setCustomArgumentResolvers(userResolver)
                .build();
    }

    @Test
    void getReviews_ReturnsOk() throws Exception {
        when(reviewService.getProductReviews(eq(1L), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(new ReviewDto()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/products/1/reviews"))
                .andExpect(status().isOk());
    }

    @Test
    void getPendingReviews_ReturnsOk() throws Exception {
        when(reviewService.getPendingReviews(anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(new ReviewDto()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/products/1/reviews/admin/pending"))
                .andExpect(status().isOk());
    }

    @Test
    void getReviewStats_ReturnsOk() throws Exception {
        when(reviewService.getReviewStats()).thenReturn(new ReviewStatsDto());

        mockMvc.perform(get("/api/products/1/reviews/admin/stats"))
                .andExpect(status().isOk());
    }

    @Test
    void moderateReview_ReturnsOk() throws Exception {
        when(reviewService.moderateReview(eq(5L), eq(ReviewStatus.APPROVED)))
                .thenReturn(new ReviewDto());

        mockMvc.perform(patch("/api/products/1/reviews/5/moderate")
                .param("status", "APPROVED"))
                .andExpect(status().isOk());
    }

    @Test
    void addOrUpdateReview_whenSuccess_ReturnsOk() throws Exception {
        when(reviewService.addOrUpdateReview(eq(1L), eq("user@test.com"), any(ReviewRequest.class)))
                .thenReturn(new ReviewDto());

        mockMvc.perform(post("/api/products/1/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":5,\"comment\":\"Excellent!\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void addOrUpdateReview_whenException_ReturnsBadRequest() throws Exception {
        when(reviewService.addOrUpdateReview(eq(1L), eq("user@test.com"), any(ReviewRequest.class)))
                .thenThrow(new RuntimeException("Product not purchased"));

        mockMvc.perform(post("/api/products/1/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":5,\"comment\":\"Excellent!\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteReview_whenSuccess_ReturnsNoContent() throws Exception {
        doNothing().when(reviewService).deleteReview(eq(5L), eq("user@test.com"));

        mockMvc.perform(delete("/api/products/1/reviews/5"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteReview_whenException_ReturnsBadRequest() throws Exception {
        doThrow(new RuntimeException("Not your review")).when(reviewService)
                .deleteReview(eq(5L), eq("user@test.com"));

        mockMvc.perform(delete("/api/products/1/reviews/5"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteReviewAsAdmin_ReturnsNoContent() throws Exception {
        doNothing().when(reviewService).deleteReviewAsAdmin(eq(5L));

        mockMvc.perform(delete("/api/products/1/reviews/5/admin"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getMyReview_ReturnsOk() throws Exception {
        when(reviewService.getMyReviewByProduct(eq(1L), eq("user@test.com")))
                .thenReturn(new ReviewDto());

        mockMvc.perform(get("/api/products/1/reviews/my"))
                .andExpect(status().isOk());
    }
}
