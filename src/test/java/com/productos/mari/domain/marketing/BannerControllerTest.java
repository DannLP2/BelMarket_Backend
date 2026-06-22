package com.productos.mari.domain.marketing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockMultipartFile;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BannerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BannerService bannerService;

    @Mock
    private com.productos.mari.domain.infrastructure.media.CloudinaryService cloudinaryService;

    @InjectMocks
    private BannerController bannerController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(bannerController).build();
    }

    @Test
    void getActiveBanners_ReturnsOk() throws Exception {
        when(bannerService.getActiveBanners()).thenReturn(List.of(new BannerDto()));
        mockMvc.perform(get("/api/banners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getAllBanners_ReturnsOk() throws Exception {
        when(bannerService.getAllBanners()).thenReturn(List.of(new BannerDto()));
        mockMvc.perform(get("/api/banners/admin"))
                .andExpect(status().isOk());
    }

    @Test
    void createBanner_ReturnsOk() throws Exception {
        BannerDto dto = new BannerDto();
        dto.setTitle("Sample");
        when(bannerService.createBanner(any(BannerDto.class))).thenReturn(dto);

        mockMvc.perform(post("/api/banners/admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Sample\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateBanner_ReturnsOk() throws Exception {
        when(bannerService.updateBanner(eq(1L), any(BannerDto.class))).thenReturn(new BannerDto());

        mockMvc.perform(put("/api/banners/admin/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Sample\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteBanner_ReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/banners/admin/1"))
                .andExpect(status().isNoContent());
        verify(bannerService).deleteBanner(1L);
    }

    @Test
    void toggleBannerStatus_ReturnsOk() throws Exception {
        mockMvc.perform(patch("/api/banners/admin/1/toggle"))
                .andExpect(status().isOk());
        verify(bannerService).toggleBannerStatus(1L);
    }

    @Test
    void trackMetric_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/banners/1/track")
                .param("type", "IMPRESSION"))
                .andExpect(status().isOk());
    }

    @Test
    void getBannersByPlacement_ReturnsOk() throws Exception {
        when(bannerService.getActiveBannersByPlacement(BannerPlacement.HOME_CAROUSEL))
                .thenReturn(List.of(new BannerDto()));
        mockMvc.perform(get("/api/banners/placement/HOME_CAROUSEL"))
                .andExpect(status().isOk());
    }

    @Test
    void uploadBanner_ReturnsOk() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "banner.png", "image/png", "img".getBytes());
        when(cloudinaryService.uploadFile(any(), anyString())).thenReturn("http://img.url");

        mockMvc.perform(multipart("/api/banners/admin/upload").file(file))
                .andExpect(status().isOk());
    }
}
