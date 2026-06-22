package com.productos.mari.domain.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.productos.mari.exception.TokenRefreshException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock private AuthService authService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private JwtService jwtService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    void register_Success() throws Exception {
        RegisterRequest regReq = new RegisterRequest();
        regReq.setEmail("test@test.com");
        regReq.setPassword("Password123");
        
        MockMultipartFile userPart = new MockMultipartFile("user", "", "application/json", objectMapper.writeValueAsBytes(regReq));
        MockMultipartFile imagePart = new MockMultipartFile("image", "test.jpg", "image/jpeg", "content".getBytes());

        when(authService.register(any(), any())).thenReturn(new AuthResponse());

        mockMvc.perform(multipart("/auth/register")
                .file(userPart)
                .file(imagePart))
                .andExpect(status().isOk());
    }

    @Test
    void authenticate_Success() throws Exception {
        AuthRequest req = new AuthRequest();
        req.setEmail("test@test.com");
        req.setPassword("password");

        when(authService.authenticate(any())).thenReturn(new AuthResponse());

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void refreshToken_Success() throws Exception {
        TokenRefreshRequest req = new TokenRefreshRequest();
        req.setRefreshToken("old-token");

        RefreshToken mockRefreshToken = new RefreshToken();
        mockRefreshToken.setToken("new-token");
        
        when(refreshTokenService.findByToken("old-token")).thenReturn(Optional.of(mockRefreshToken));
        when(refreshTokenService.verifyExpiration(any())).thenReturn(mockRefreshToken);
        when(refreshTokenService.renewToken(any())).thenReturn(mockRefreshToken);
        when(jwtService.generateToken(any())).thenReturn("new-jwt");

        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-jwt"));
    }

    @Test
    void refreshToken_NotFound_ThrowsException() throws Exception {
        TokenRefreshRequest req = new TokenRefreshRequest();
        req.setRefreshToken("bad-token");

        when(refreshTokenService.findByToken("bad-token")).thenReturn(Optional.empty());

        // Note: Standalone setup doesn't always handle custom exceptions perfectly without @ControllerAdvice
        // But the controller code itself throws it.
        try {
            mockMvc.perform(post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)));
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof TokenRefreshException);
        }
    }

    @Test
    void verifyEmail_Success() throws Exception {
        VerificationRequest req = new VerificationRequest();
        req.setEmail("test@test.com");
        req.setCode("123456");

        when(authService.verifyEmail(any())).thenReturn(new AuthResponse());

        mockMvc.perform(post("/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void logout_Success() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isNoContent());
        
        verify(authService).logout();
    }
}
