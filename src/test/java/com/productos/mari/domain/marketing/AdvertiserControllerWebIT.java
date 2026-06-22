package com.productos.mari.domain.marketing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdvertiserController.class)
@Import(com.productos.mari.config.TestSecurityConfig.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class AdvertiserControllerWebIT {

    private MockMvc mockMvc;
    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private AdvertiserService advertiserService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // ── Public ─────────────────────────────────────────────────

    @Test
    @WithAnonymousUser
    void getActiveAdvertisers_public_shouldReturn200() throws Exception {
        when(advertiserService.getActiveAdvertisers()).thenReturn(List.of(new AdvertiserDto()));

        mockMvc.perform(get("/api/advertisers/active"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithAnonymousUser
    void getActiveByPlacement_public_shouldReturn200() throws Exception {
        when(advertiserService.getActiveAdvertisersByPlacement(AdPlacement.PRODUCT_LIST))
            .thenReturn(List.of(new AdvertiserDto()));

        mockMvc.perform(get("/api/advertisers/active/placement/PRODUCT_LIST"))
            .andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser
    void trackMetric_public_shouldReturn200() throws Exception {
        mockMvc.perform(post("/api/advertisers/1/track").param("type", "CLICK"))
            .andExpect(status().isOk());

        verify(advertiserService).recordAdMetric(eq(1L), eq(AdMetricType.CLICK), any());
    }

    // ── Admin: 200 with ADMIN role ─────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllAdvertisers_asAdmin_shouldReturn200() throws Exception {
        when(advertiserService.getAllAdvertisers(anyString(), anyString())).thenReturn(List.of(new AdvertiserDto()));

        mockMvc.perform(get("/api/advertisers/admin"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createAdvertiser_asAdmin_shouldReturn200() throws Exception {
        AdvertiserDto dto = AdvertiserDto.builder().name("Test Ad").build();
        when(advertiserService.createAdvertiser(any())).thenReturn(dto);

        mockMvc.perform(post("/api/advertisers/admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateAdvertiser_asAdmin_shouldReturn200() throws Exception {
        AdvertiserDto dto = AdvertiserDto.builder().name("Updated").build();
        when(advertiserService.updateAdvertiser(eq(1L), any())).thenReturn(dto);

        mockMvc.perform(put("/api/advertisers/admin/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteAdvertiser_asAdmin_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/advertisers/admin/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void toggleAdvertiserStatus_asAdmin_shouldReturn200() throws Exception {
        mockMvc.perform(patch("/api/advertisers/admin/1/toggle"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void reorderAds_asAdmin_shouldReturn200() throws Exception {
        mockMvc.perform(put("/api/advertisers/admin/reorder")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[1, 2, 3]"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void uploadLogo_asAdmin_shouldReturn200() throws Exception {
        when(advertiserService.uploadLogo(any())).thenReturn("http://cdn.com/logo.png");
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", "img".getBytes());

        mockMvc.perform(multipart("/api/advertisers/admin/upload").file(file))
            .andExpect(status().isOk());
    }

    // ── Access control: CLIENT role → 403 ─────────────────────

    @Test
    @WithMockUser(roles = "CLIENT")
    void createAdvertiser_asClient_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/advertisers/admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AdvertiserDto())))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    void createAdvertiser_anonymous_shouldReturn401Or403() throws Exception {
        mockMvc.perform(post("/api/advertisers/admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                assert status == 401 || status == 403;
            });
    }
}
