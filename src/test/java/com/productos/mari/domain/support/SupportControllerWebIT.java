package com.productos.mari.domain.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SupportController.class)
@Import(com.productos.mari.config.TestSecurityConfig.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class SupportControllerWebIT {

    private MockMvc mockMvc;
    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private SupportService supportService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // ── Public: POST /api/support (multipart) ─────────────────

    @Test
    @WithAnonymousUser
    void createSupportRequest_public_shouldReturn200() throws Exception {
        SupportRequestDto dto = SupportRequestDto.builder()
            .id(1L).name("Test").email("t@t.com").build();
        when(supportService.processSupportRequest(any(SupportRequestDto.class), any()))
            .thenReturn(dto);

        mockMvc.perform(multipart("/api/support")
                .param("name", "Test User")
                .param("email", "test@test.com")
                .param("requestType", "COMPLAINT")
                .param("message", "Necesito ayuda con mi pedido"))
            .andExpect(status().isOk());
    }

    // ── Admin: GET /api/support/admin ──────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllRequests_asAdmin_shouldReturn200() throws Exception {
        when(supportService.getAllRequests()).thenReturn(List.of(new SupportRequestDto()));

        mockMvc.perform(get("/api/support/admin"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void getAllRequests_asClient_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/support/admin"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    void getAllRequests_anonymous_shouldReturn401or403() throws Exception {
        mockMvc.perform(get("/api/support/admin"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                assert status == 401 || status == 403 : "Expected 401 or 403 but was " + status;
            });
    }

    // ── Admin: PATCH /api/support/admin/{id}/status ────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateStatus_asAdmin_shouldReturn200() throws Exception {
        SupportRequestDto dto = SupportRequestDto.builder().id(1L).build();
        when(supportService.updateStatus(eq(1L), eq("RESOLVED"))).thenReturn(dto);

        mockMvc.perform(patch("/api/support/admin/1/status")
                .contentType("application/json")
                .content("{\"status\": \"RESOLVED\"}"))
            .andExpect(status().isOk());
    }

    // ── Admin: DELETE /api/support/admin/{id} ─────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteRequest_asAdmin_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/support/admin/1"))
            .andExpect(status().isNoContent());
    }
}
