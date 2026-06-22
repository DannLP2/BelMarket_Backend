package com.productos.mari.domain.reservation;

import com.productos.mari.domain.infrastructure.admin.AdminReportingService;
import com.productos.mari.domain.infrastructure.admin.AdminStatsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ReservationService reservationService;

    @Mock
    private AdminReportingService adminReportingService;

    @InjectMocks
    private ReservationController reservationController;

    private UserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        mockUserDetails = mock(UserDetails.class);
        when(mockUserDetails.getUsername()).thenReturn("user@test.com");

        HandlerMethodArgumentResolver userResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().equals(UserDetails.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                        NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return mockUserDetails;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(reservationController)
                .setCustomArgumentResolvers(userResolver)
                .build();
    }

    @Test
    void createReservation_ReturnsOk() throws Exception {
        when(reservationService.createReservation(any(ReservationDto.class), eq("user@test.com")))
                .thenReturn(new ReservationDto());

        mockMvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\":[]}"))
                .andExpect(status().isOk());
    }

    @Test
    void getMyReservations_ReturnsOk() throws Exception {
        when(reservationService.getMyReservations("user@test.com"))
                .thenReturn(List.of(new ReservationDto()));

        mockMvc.perform(get("/api/reservations/my"))
                .andExpect(status().isOk());
    }

    @Test
    void updateReservation_ReturnsOk() throws Exception {
        when(reservationService.updateReservation(eq(1L), any(ReservationDto.class), eq("user@test.com")))
                .thenReturn(new ReservationDto());

        mockMvc.perform(put("/api/reservations/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\":[]}"))
                .andExpect(status().isOk());
    }

    @Test
    void cancelReservation_ReturnsOk() throws Exception {
        when(reservationService.cancelReservation(eq(1L), eq("user@test.com")))
                .thenReturn(new ReservationDto());

        mockMvc.perform(patch("/api/reservations/1/cancel"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllReservations_ReturnsOk() throws Exception {
        when(reservationService.getAllReservations())
                .thenReturn(List.of(new ReservationDto()));

        mockMvc.perform(get("/api/reservations/admin"))
                .andExpect(status().isOk());
    }

    @Test
    void updateStatus_ReturnsOk() throws Exception {
        when(reservationService.updateReservationStatus(eq(1L), eq(ReservationStatus.CONFIRMED)))
                .thenReturn(new ReservationDto());

        mockMvc.perform(patch("/api/reservations/admin/1/status")
                .param("status", "CONFIRMED"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteAllCancelled_ReturnsOk() throws Exception {
        when(reservationService.deleteAllCancelled()).thenReturn(3);

        mockMvc.perform(delete("/api/reservations/admin/cancelled"))
                .andExpect(status().isOk());
    }

    @Test
    void getAdminStats_ReturnsOk() throws Exception {
        when(adminReportingService.getAdminStats()).thenReturn(new AdminStatsDto());

        mockMvc.perform(get("/api/reservations/admin/stats"))
                .andExpect(status().isOk());
    }

    @Test
    void checkUserPurchase_WhenAuthenticated_ReturnsOk() throws Exception {
        when(reservationService.hasUserPurchasedProduct(eq("user@test.com"), eq(5L))).thenReturn(true);

        mockMvc.perform(get("/api/reservations/check-purchase/5"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void checkUserPurchase_WhenNotAuthenticated_ReturnsFalse() throws Exception {
        HandlerMethodArgumentResolver nullResolver = new HandlerMethodArgumentResolver() {
            @Override public boolean supportsParameter(MethodParameter p) { return p.getParameterType().equals(UserDetails.class); }
            @Override public Object resolveArgument(MethodParameter p, ModelAndViewContainer m, NativeWebRequest w, WebDataBinderFactory f) { return null; }
        };
        MockMvc publicMockMvc = MockMvcBuilders.standaloneSetup(reservationController)
                .setCustomArgumentResolvers(nullResolver).build();

        publicMockMvc.perform(get("/api/reservations/check-purchase/5"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void getReservationItem_ReturnsOk() throws Exception {
        when(reservationService.getReservationItem(eq(10L), eq("user@test.com")))
                .thenReturn(new ReservationItemDto());

        mockMvc.perform(get("/api/reservations/item/10"))
                .andExpect(status().isOk());
    }

    @Test
    void getAvailableOrders_ReturnsOk() throws Exception {
        when(reservationService.getAvailableOrders())
                .thenReturn(List.of(new ReservationDto()));

        mockMvc.perform(get("/api/reservations/delivery/available"))
                .andExpect(status().isOk());
    }

    @Test
    void getAssignedOrders_ReturnsOk() throws Exception {
        when(reservationService.getAssignedOrders("user@test.com"))
                .thenReturn(List.of(new ReservationDto()));

        mockMvc.perform(get("/api/reservations/delivery/assigned"))
                .andExpect(status().isOk());
    }

    @Test
    void claimOrder_ReturnsOk() throws Exception {
        when(reservationService.assignToMe(eq(1L), eq("user@test.com")))
                .thenReturn(new ReservationDto());

        mockMvc.perform(patch("/api/reservations/delivery/1/claim"))
                .andExpect(status().isOk());
    }

    @Test
    void updateDeliveryStatus_ReturnsOk() throws Exception {
        when(reservationService.updateReservationStatus(eq(1L), eq(ReservationStatus.COMPLETED)))
                .thenReturn(new ReservationDto());

        mockMvc.perform(patch("/api/reservations/delivery/1/status")
                .param("status", "COMPLETED"))
                .andExpect(status().isOk());
    }

    @Test
    void completeDelivery_ReturnsOk() throws Exception {
        when(reservationService.completeReservationWithProof(eq(1L), eq("ABC123"), any()))
                .thenReturn(new ReservationDto());

        mockMvc.perform(multipart("/api/reservations/delivery/1/complete")
                .param("code", "ABC123"))
                .andExpect(status().isOk());
    }

    @Test
    void getReservationPdf_ReturnsOk() throws Exception {
        when(reservationService.getReservationPdf(eq(1L), eq("user@test.com"), any()))
                .thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/reservations/1/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }
}
