package com.productos.mari.domain.infrastructure.reporting;

import com.productos.mari.domain.reservation.ReservationDto;
import com.productos.mari.domain.reservation.ReservationItemDto;
import com.productos.mari.domain.reservation.ReservationStatus;
import com.productos.mari.domain.settings.AppSettings;
import com.productos.mari.domain.settings.AppSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PDFServiceImplTest {

    @Mock
    private AppSettingsService appSettingsService;

    @InjectMocks
    private PDFServiceImpl pdfService;

    private ReservationDto mockReservation;
    private AppSettings mockSettings;

    @BeforeEach
    void setUp() {
        mockSettings = AppSettings.builder()
                .storeName("BelMarket Test")
                .address("Test Address")
                .contactEmail("test@test.com")
                .footerText("Footer Note")
                .build();

        mockReservation = ReservationDto.builder()
                .id(1L)
                .reference("RES-123")
                .customerName("John Doe")
                .customerEmail("john@test.com")
                .status(ReservationStatus.CONFIRMED)
                .total(new BigDecimal("1000.00"))
                .taxAmount(new BigDecimal("190.00"))
                .taxRate(new BigDecimal("19.0"))
                .shippingCost(new BigDecimal("50.00"))
                .shippingAddress("Shipping Ave 123")
                .createdAt(LocalDateTime.now())
                .items(List.of(
                        ReservationItemDto.builder()
                                .productName("Item 1")
                                .quantity(2)
                                .price(new BigDecimal("500.00"))
                                .build()
                ))
                .build();
    }

    @Test
    void generateReservationReceipt_Success() {
        when(appSettingsService.getSettings()).thenReturn(mockSettings);

        byte[] pdf = pdfService.generateReservationReceipt(mockReservation);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        // We can't easily verify the content of the PDF bytes here, 
        // but getting non-empty bytes means the iText generation finished.
    }

    @Test
    void generateReservationReceipt_WithDifferentStatuses() {
        when(appSettingsService.getSettings()).thenReturn(mockSettings);

        for (ReservationStatus status : ReservationStatus.values()) {
            mockReservation.setStatus(status);
            byte[] pdf = pdfService.generateReservationReceipt(mockReservation);
            assertNotNull(pdf);
        }
    }
}
