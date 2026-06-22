package com.productos.mari.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    // Dummy controller to trigger exceptions
    @RestController
    static class ExceptionTriggerController {
        @GetMapping("/trigger-not-found")
        public void triggerNotFound() {
            throw new ResourceNotFoundException("Not found");
        }

        @GetMapping("/trigger-bad-credentials")
        public void triggerBadCredentials() {
            throw new BadCredentialsException("Bad credentials");
        }

        @GetMapping("/trigger-illegal-argument")
        public void triggerIllegalArgument() {
            throw new IllegalArgumentException("Illegal argument");
        }

        @GetMapping("/trigger-max-upload")
        public void triggerMaxUpload() {
            throw new MaxUploadSizeExceededException(1000L);
        }

        @GetMapping("/trigger-access-denied")
        public void triggerAccessDenied() {
            throw new AccessDeniedException("Access denied");
        }

        @GetMapping("/trigger-token-refresh")
        public void triggerTokenRefresh() {
            throw new TokenRefreshException("token", "Token error");
        }

        @GetMapping("/trigger-data-integrity")
        public void triggerDataIntegrity() {
            throw new DataIntegrityViolationException("Duplicate entry 'test'");
        }

        @GetMapping("/trigger-general")
        public void triggerGeneral() throws Exception {
            throw new Exception("General error");
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ExceptionTriggerController())
                .setControllerAdvice(globalExceptionHandler)
                .build();
    }

    @Test
    void handleResourceNotFound_Returns404() throws Exception {
        mockMvc.perform(get("/trigger-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Not found"));
    }

    @Test
    void handleBadCredentials_Returns401() throws Exception {
        mockMvc.perform(get("/trigger-bad-credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales incorrectas."));
    }

    @Test
    void handleIllegalArgument_Returns400() throws Exception {
        mockMvc.perform(get("/trigger-illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Illegal argument"));
    }

    @Test
    void handleMaxUploadSize_Returns413() throws Exception {
        mockMvc.perform(get("/trigger-max-upload"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.status").value(413));
    }

    @Test
    void handleAccessDenied_Returns403() throws Exception {
        mockMvc.perform(get("/trigger-access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tienes permisos para realizar esta acción."));
    }

    @Test
    void handleTokenRefresh_Returns403() throws Exception {
        mockMvc.perform(get("/trigger-token-refresh"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Failed for [token]: Token error"));
    }

    @Test
    void handleDataIntegrity_Returns400() throws Exception {
        mockMvc.perform(get("/trigger-data-integrity"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: Ya existe un registro con estos datos únicos (nombre o identificador duplicado)."));
    }

    @Test
    void handleGeneralException_Returns500() throws Exception {
        mockMvc.perform(get("/trigger-general"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));
    }
}
