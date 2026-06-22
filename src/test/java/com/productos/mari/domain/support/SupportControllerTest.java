package com.productos.mari.domain.support;

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
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SupportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SupportService supportService;

    @InjectMocks
    private SupportController supportController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(supportController).build();
    }

    @Test
    void createSupportRequest_ReturnsOk() throws Exception {
        SupportRequestDto dto = new SupportRequestDto();
        dto.setId(1L);
        when(supportService.processSupportRequest(any(SupportRequestDto.class), any())).thenReturn(dto);

        mockMvc.perform(multipart("/api/support")
                .param("name", "Test User")
                .param("email", "test@test.com")
                .param("message", "Help me!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.requestId").value(1));
    }

    @Test
    void getAllSupportRequests_ReturnsOk() throws Exception {
        when(supportService.getAllRequests()).thenReturn(List.of(new SupportRequestDto()));
        mockMvc.perform(get("/api/support/admin"))
                .andExpect(status().isOk());
    }

    @Test
    void updateSupportStatus_ReturnsOk() throws Exception {
        SupportRequestDto dto = new SupportRequestDto();
        dto.setId(1L);
        when(supportService.updateStatus(eq(1L), eq("RESOLVED"))).thenReturn(dto);

        mockMvc.perform(patch("/api/support/admin/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteSupportRequest_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/support/admin/1"))
                .andExpect(status().isNoContent());
        verify(supportService).deleteRequest(1L);
    }
}
