package com.productos.mari.domain.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.productos.mari.config.TestSecurityConfig;
import com.productos.mari.domain.user.Role;
import com.productos.mari.domain.user.User;
import com.productos.mari.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.productos.mari.domain.infrastructure.media.CloudinaryService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class SupportWebIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private SupportRequestRepository supportRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ObjectMapper objectMapper;

    @MockBean
    private CloudinaryService cloudinaryService;

    @BeforeEach
    void setUp() {
        supportRepository.deleteAll();
    }

    @Test
    void createSupportRequest_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "attachment", "test.txt", "text/plain", "Hello".getBytes());

        mockMvc.perform(multipart("/api/support")
                .file(file)
                .param("name", "Test User")
                .param("email", "test@user.com")
                .param("requestType", "Issue")
                .param("message", "Something is wrong"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminEndpoints_shouldBeAccessibleToAdmin() throws Exception {
        // Create an initial request
        SupportRequest request = supportRepository.save(SupportRequest.builder()
                .name("User")
                .email("u@t.com")
                .requestType("Technical")
                .message("Msg")
                .status("PENDING")
                .build());

        // Get all
        mockMvc.perform(get("/api/support/admin"))
                .andExpect(status().isOk());

        // Update status
        mockMvc.perform(patch("/api/support/admin/" + request.getId() + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", "IN_PROGRESS"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // Delete
        mockMvc.perform(delete("/api/support/admin/" + request.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void adminEndpoints_shouldBeForbiddenToClient() throws Exception {
        mockMvc.perform(get("/api/support/admin"))
                .andExpect(status().isForbidden());
    }
}
