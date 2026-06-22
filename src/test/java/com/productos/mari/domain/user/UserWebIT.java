package com.productos.mari.domain.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.productos.mari.config.TestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
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

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({UserController.class, UserProfileController.class})
@Import(TestSecurityConfig.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public class UserWebIT {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private UserService userService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // ──────────────────────────────────────────────────────────
    // Admin Endpoints (/api/users/admin/**)
    // ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_asAdmin_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/users/admin"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void getAllUsers_asClient_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/users/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void toggleStatus_asAdmin_shouldReturn200() throws Exception {
        mockMvc.perform(patch("/api/users/admin/1/status"))
                .andExpect(status().isOk());
    }

    // ──────────────────────────────────────────────────────────
    // Profile Endpoints (/api/users/profile/**)
    // ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void getProfile_whenAuthenticated_shouldReturn200() throws Exception {
        when(userService.getCurrentUserProfile()).thenReturn(new UserProfileDto());

        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser
    void getProfile_whenAnonymous_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isUnauthorized());
    }
}
