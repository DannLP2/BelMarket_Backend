package com.productos.mari.domain.mecatronic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.productos.mari.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class MecatronicControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MecatronicService mecatronicService;

    @InjectMocks
    private MecatronicController mecatronicController;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(1L).email("user@test.com").build();
        
        // Mock resolver for @AuthenticationPrincipal User
        HandlerMethodArgumentResolver userResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().equals(User.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                        NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return mockUser;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(mecatronicController)
                .setCustomArgumentResolvers(userResolver)
                .build();
    }

    @Test
    void receiveTelemetry_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/mecatronic/telemetry")
                .param("apiKey", "test-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"temp\":\"25.5\"}"))
                .andExpect(status().isOk());
        
        verify(mecatronicService).processTelemetry(eq("test-key"), anyMap());
    }

    @Test
    void getCommands_ReturnsOk() throws Exception {
        when(mecatronicService.getPendingCommands(anyString()))
                .thenReturn(Map.of("relay", "ON"));

        mockMvc.perform(get("/api/mecatronic/commands")
                .param("apiKey", "test-key"))
                .andExpect(status().isOk());
    }

    @Test
    void getDashboard_ReturnsOk() throws Exception {
        when(mecatronicService.getDashboard(eq(1L)))
                .thenReturn(new MecatronicDashboardDto());

        mockMvc.perform(get("/api/mecatronic/dashboard/1"))
                .andExpect(status().isOk());
    }

    @Test
    void sendAction_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/mecatronic/action/100")
                .param("value", "ON"))
                .andExpect(status().isOk());
        
        verify(mecatronicService).sendCommand(100L, "ON");
    }

    @Test
    void getMyDevices_ReturnsOk() throws Exception {
        when(mecatronicService.getMyDevices(eq(1L), anyString(), anyString(), anyString())).thenReturn(List.of(new MecatronicDashboardDto()));

        mockMvc.perform(get("/api/mecatronic/my-devices"))
                .andExpect(status().isOk());
    }

    @Test
    void bindDevice_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/mecatronic/bind/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"serial\":\"SN123\",\"pin\":\"1234\"}"))
                .andExpect(status().isOk());
        
        verify(mecatronicService).bindDevice(1L, 1L, "SN123", "1234");
    }

    @Test
    void bindExternalDevice_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/mecatronic/bind-external/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"serial\":\"SN123\",\"pin\":\"1234\"}"))
                .andExpect(status().isOk());

        verify(mecatronicService).bindExternalDevice(1L, 1L, "SN123", "1234");
    }
}
