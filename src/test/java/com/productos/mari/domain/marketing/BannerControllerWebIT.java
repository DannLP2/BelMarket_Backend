package com.productos.mari.domain.marketing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@WebMvcTest(BannerController.class)
@Import(com.productos.mari.config.TestSecurityConfig.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class BannerControllerWebIT {

    private MockMvc mockMvc;
    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private BannerService bannerService;
    @MockBean private com.productos.mari.domain.infrastructure.media.CloudinaryService cloudinaryService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // ──────────────────────────────────────────────────────────
    // PUBLIC endpoints (sin auth)
    // ──────────────────────────────────────────────────────────

    @Test
    @WithAnonymousUser
    void getActiveBanners_public_shouldReturn200() throws Exception {
        when(bannerService.getActiveBanners()).thenReturn(List.of(new BannerDto()));

        mockMvc.perform(get("/api/banners"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithAnonymousUser
    void getBannersByPlacement_public_shouldReturn200() throws Exception {
        when(bannerService.getActiveBannersByPlacement(BannerPlacement.HOME_CAROUSEL))
            .thenReturn(List.of(new BannerDto()));

        mockMvc.perform(get("/api/banners/placement/HOME_CAROUSEL"))
            .andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser
    void trackMetric_public_shouldReturn200() throws Exception {
        mockMvc.perform(post("/api/banners/1/track").param("type", "IMPRESSION"))
            .andDo(print())
            .andExpect(status().isOk());

        verify(bannerService).recordMetric(eq(1L), eq(AdMetricType.IMPRESSION), any());
    }

    // ──────────────────────────────────────────────────────────
    // ADMIN endpoints — con ADMIN role → 200
    // ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllBanners_asAdmin_shouldReturn200() throws Exception {
        when(bannerService.getAllBanners()).thenReturn(List.of(new BannerDto()));

        mockMvc.perform(get("/api/banners/admin"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createBanner_asAdmin_shouldReturn200() throws Exception {
        BannerDto dto = new BannerDto();
        dto.setTitle("Test Banner");
        when(bannerService.createBanner(any(BannerDto.class))).thenReturn(dto);

        mockMvc.perform(post("/api/banners/admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateBanner_asAdmin_shouldReturn200() throws Exception {
        BannerDto dto = new BannerDto();
        when(bannerService.updateBanner(eq(1L), any(BannerDto.class))).thenReturn(dto);

        mockMvc.perform(put("/api/banners/admin/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteBanner_asAdmin_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/banners/admin/1"))
            .andDo(print())
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void toggleBannerStatus_asAdmin_shouldReturn200() throws Exception {
        mockMvc.perform(patch("/api/banners/admin/1/toggle"))
            .andExpect(status().isOk());
    }

    // ──────────────────────────────────────────────────────────
    // ADMIN endpoints — sin auth → 401/403
    // ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "CLIENT")
    void createBanner_asClient_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/banners/admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new BannerDto())))
            .andExpect(status().isForbidden());
    }
}
