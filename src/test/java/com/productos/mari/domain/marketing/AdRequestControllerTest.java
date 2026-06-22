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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdRequestControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdRequestService adRequestService;

    @InjectMocks
    private AdRequestController adRequestController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adRequestController).build();
    }

    @Test
    void submitRequest_ReturnsOk() throws Exception {
        when(adRequestService.createRequest(any(AdRequest.class))).thenReturn(new AdRequest());

        mockMvc.perform(post("/api/ad-requests/public")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"companyName\": \"Test Corp\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllRequests_ReturnsOk() throws Exception {
        when(adRequestService.getAllRequests(any(), any())).thenReturn(List.of(new AdRequest()));
        mockMvc.perform(get("/api/ad-requests/admin"))
                .andExpect(status().isOk());
    }

    @Test
    void updateStatus_ReturnsOk() throws Exception {
        when(adRequestService.updateStatus(eq(1L), any(AdRequestStatus.class))).thenReturn(new AdRequest());

        mockMvc.perform(patch("/api/ad-requests/admin/1/status")
                .param("status", "APPROVED"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteRequest_ReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/ad-requests/admin/1"))
                .andExpect(status().isNoContent());
        verify(adRequestService).deleteRequest(1L);
    }
}
