package com.productos.mari.domain.reservation;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReservationController.class)
@Import(com.productos.mari.config.TestSecurityConfig.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class ReservationControllerWebIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private ReservationService reservationService;
    @MockBean private com.productos.mari.domain.infrastructure.admin.AdminReportingService adminReportingService;

    // ── POST /api/reservations → debe estar autenticado ────────

    @Test
    @WithAnonymousUser
    void createReservation_anonymous_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"total\": 10000}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = "CLIENT")
    void createReservation_authenticated_shouldReturn200() throws Exception {
        ReservationDto dto = ReservationDto.builder()
            .id(1L).total(BigDecimal.valueOf(10000)).build();
        when(reservationService.createReservation(any(ReservationDto.class), eq("user@test.com")))
            .thenReturn(dto);

        mockMvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk());
    }

    // ── GET /api/reservations/my → autenticado ─────────────────

    @Test
    @WithAnonymousUser
    void getMyReservations_anonymous_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/reservations/my"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = "CLIENT")
    void getMyReservations_authenticated_shouldReturn200() throws Exception {
        when(reservationService.getMyReservations("user@test.com"))
            .thenReturn(List.of(new ReservationDto()));

        mockMvc.perform(get("/api/reservations/my"))
            .andExpect(status().isOk());
    }

    // ── PATCH /api/reservations/{id}/cancel → autenticado ──────

    @Test
    @WithMockUser(username = "user@test.com", roles = "CLIENT")
    void cancelReservation_authenticated_shouldReturn200() throws Exception {
        when(reservationService.cancelReservation(eq(1L), eq("user@test.com")))
            .thenReturn(new ReservationDto());

        mockMvc.perform(patch("/api/reservations/1/cancel"))
            .andExpect(status().isOk());
    }

    // ── GET /api/reservations/admin → solo ADMIN ───────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllReservations_asAdmin_shouldReturn200() throws Exception {
        when(reservationService.getAllReservations()).thenReturn(List.of(new ReservationDto()));

        mockMvc.perform(get("/api/reservations/admin"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void getAllReservations_asClient_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/reservations/admin"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    void getAllReservations_anonymous_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/reservations/admin"))
            .andExpect(status().isUnauthorized());
    }

    // ── PATCH /api/reservations/admin/{id}/status → solo ADMIN ─

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateStatus_asAdmin_shouldReturn200() throws Exception {
        when(reservationService.updateReservationStatus(eq(1L), eq(ReservationStatus.CONFIRMED)))
            .thenReturn(new ReservationDto());

        mockMvc.perform(patch("/api/reservations/admin/1/status")
                .param("status", "CONFIRMED"))
            .andExpect(status().isOk());
    }

    // ── Deliverer endpoints ─────────────────────────────────────

    @Test
    @WithMockUser(roles = "DELIVERER")
    void getAvailableOrders_asDeliverer_shouldReturn200() throws Exception {
        when(reservationService.getAvailableOrders()).thenReturn(List.of(new ReservationDto()));

        mockMvc.perform(get("/api/reservations/delivery/available"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void getAvailableOrders_asClient_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/reservations/delivery/available"))
            .andExpect(status().isForbidden());
    }
}
