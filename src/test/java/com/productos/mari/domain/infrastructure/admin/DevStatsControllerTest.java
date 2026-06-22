package com.productos.mari.domain.infrastructure.admin;

import com.productos.mari.domain.auth.SecurityLogRepository;
import com.productos.mari.domain.mecatronic.MecatronicDeviceRepository;
import com.productos.mari.domain.notification.NotificationRepository;
import com.productos.mari.domain.product.ProductRepository;
import com.productos.mari.domain.reservation.ReservationRepository;
import com.productos.mari.domain.review.ReviewRepository;
import com.productos.mari.domain.user.UserLinkedDeviceRepository;
import com.productos.mari.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DevStatsControllerTest {

    private MockMvc mockMvc;

    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private MecatronicDeviceRepository mecatronicDeviceRepository;
    @Mock private UserLinkedDeviceRepository userLinkedDeviceRepository;
    @Mock private SecurityLogRepository securityLogRepository;
    @Mock private NotificationRepository notificationRepository;

    @InjectMocks
    private DevStatsController devStatsController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(devStatsController).build();
    }

    @Test
    void getDevStats_ReturnsAllCategories() throws Exception {
        when(productRepository.count()).thenReturn(10L);
        when(userRepository.count()).thenReturn(20L);
        when(reservationRepository.count()).thenReturn(5L);

        mockMvc.perform(get("/api/admin/dev/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products.total").value(10))
                .andExpect(jsonPath("$.users.total").value(20))
                .andExpect(jsonPath("$.reservations.total").value(5))
                .andExpect(jsonPath("$._meta").exists());
    }

    @Test
    void getSystemHealth_ReturnsMemoryMetrics() throws Exception {
        mockMvc.perform(get("/api/admin/dev/system-health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memory.max").exists())
                .andExpect(jsonPath("$.processors").exists());
    }

    @Test
    void getLogs_ReturnsResponse() throws Exception {
        // We verify the structure without assuming the file exists/doesn't exist in the CI/Test env
        mockMvc.perform(get("/api/admin/dev/logs"))
                .andExpect(status().isOk());
    }
}
