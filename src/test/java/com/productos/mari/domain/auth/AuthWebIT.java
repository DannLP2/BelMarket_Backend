package com.productos.mari.domain.auth;

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

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AuthController.class, SecurityAdminController.class})
@Import(TestSecurityConfig.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public class AuthWebIT {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    // JwtService is already mocked in TestSecurityConfig, but we might need to stub it here
    @Autowired
    private JwtService jwtService;

    @MockBean
    private SecurityLogRepository securityLogRepository;

    @MockBean
    private com.productos.mari.domain.reservation.ReservationRepository reservationRepository;

    @MockBean
    private com.productos.mari.domain.product.ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // ──────────────────────────────────────────────────────────
    // AuthController - Public Endpoints
    // ──────────────────────────────────────────────────────────

    @Test
    @WithAnonymousUser
    void login_shouldBePublic() throws Exception {
        AuthRequest request = new AuthRequest("test@test.com", "password");
        when(authService.authenticate(any())).thenReturn(new AuthResponse());

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser
    void forgotPassword_shouldBePublic() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("test@test.com");
        when(authService.forgotPassword(any())).thenReturn(new AuthResponse());

        mockMvc.perform(post("/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // ──────────────────────────────────────────────────────────
    // AuthController - Authenticated Endpoints
    // ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void getMe_whenAuthenticated_shouldReturn200() throws Exception {
        when(authService.getMe()).thenReturn(new AuthResponse());

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser
    void getMe_whenAnonymous_shouldReturn401() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    // ──────────────────────────────────────────────────────────
    // SecurityAdminController - Role Protection
    // ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void getLogs_asSuperAdmin_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/users/admin/security/logs"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getLogs_asAdminOnly_shouldReturn403() throws Exception {
        // SecurityAdminController has @PreAuthorize("hasRole('SUPER_ADMIN')") on getLogs
        mockMvc.perform(get("/api/users/admin/security/logs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void getLogs_asClient_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/users/admin/security/logs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getDelivererAudit_asAdmin_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/users/admin/security/deliverers"))
                .andExpect(status().isOk());
    }
}
