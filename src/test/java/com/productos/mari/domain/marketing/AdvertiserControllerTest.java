package com.productos.mari.domain.marketing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdvertiserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdvertiserService advertiserService;

    @InjectMocks
    private AdvertiserController advertiserController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(advertiserController).build();
    }

    @Test
    void getActiveAdvertisers_ReturnsOk() throws Exception {
        when(advertiserService.getActiveAdvertisers()).thenReturn(List.of(new AdvertiserDto()));
        mockMvc.perform(get("/api/advertisers/active"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllAdvertisers_ReturnsOk() throws Exception {
        when(advertiserService.getAllAdvertisers(any(), any())).thenReturn(List.of(new AdvertiserDto()));
        mockMvc.perform(get("/api/advertisers/admin"))
                .andExpect(status().isOk());
    }

    @Test
    void createAdvertiser_ReturnsOk() throws Exception {
        when(advertiserService.createAdvertiser(any(AdvertiserDto.class))).thenReturn(new AdvertiserDto());

        mockMvc.perform(post("/api/advertisers/admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Sample\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateAdvertiser_ReturnsOk() throws Exception {
        when(advertiserService.updateAdvertiser(eq(1L), any(AdvertiserDto.class))).thenReturn(new AdvertiserDto());

        mockMvc.perform(put("/api/advertisers/admin/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Sample\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteAdvertiser_ReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/advertisers/admin/1"))
                .andExpect(status().isNoContent());
        verify(advertiserService).deleteAdvertiser(1L);
    }

    @Test
    void toggleAdvertiserStatus_ReturnsOk() throws Exception {
        mockMvc.perform(patch("/api/advertisers/admin/1/toggle"))
                .andExpect(status().isOk());
        verify(advertiserService).toggleAdvertiserStatus(1L);
    }

    @Test
    void reorderAds_ReturnsOk() throws Exception {
        mockMvc.perform(put("/api/advertisers/admin/reorder")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[1, 2, 3]"))
                .andExpect(status().isOk());
        verify(advertiserService).reorderAds(anyList());
    }

    @Test
    void uploadLogo_ReturnsOk() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", "test image content".getBytes());
        when(advertiserService.uploadLogo(any())).thenReturn("http://image.url");

        mockMvc.perform(multipart("/api/advertisers/admin/upload").file(file))
                .andExpect(status().isOk());
    }

    @Test
    void trackAdMetric_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/advertisers/1/track")
                .param("type", "CLICK"))
                .andExpect(status().isOk());
    }

    @Test
    void getActiveAdvertisersByPlacement_ReturnsOk() throws Exception {
        when(advertiserService.getActiveAdvertisersByPlacement(AdPlacement.PRODUCT_LIST))
                .thenReturn(List.of(new AdvertiserDto()));
        mockMvc.perform(get("/api/advertisers/active/placement/PRODUCT_LIST"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllAdvertisersByPlacement_ReturnsOk() throws Exception {
        when(advertiserService.getAllAdvertisersByPlacement(AdPlacement.CHECKOUT))
                .thenReturn(List.of(new AdvertiserDto()));
        mockMvc.perform(get("/api/advertisers/admin/placement/CHECKOUT"))
                .andExpect(status().isOk());
    }
}
