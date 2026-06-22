package com.productos.mari.domain.reservation.util;

import com.productos.mari.domain.infrastructure.routing.GeoRoutingService;
import com.productos.mari.domain.reservation.DeliveryMethod;
import com.productos.mari.domain.reservation.Reservation;
import com.productos.mari.domain.settings.AppSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationPriceCalculatorTest {

    @Mock
    private GeoRoutingService geoRoutingService;

    @InjectMocks
    private ReservationPriceCalculator calculator;

    @Test
    void calculateAndSetShippingAndTax_FreeShipping() {
        Reservation reservation = Reservation.builder()
                .deliveryMethod(DeliveryMethod.DELIVERY)
                .shippingAddress("Addr 123")
                .build();
        AppSettings settings = AppSettings.builder()
                .freeShippingThreshold(new BigDecimal("100.00"))
                .taxEnabled(false)
                .build();
        
        calculator.calculateAndSetShippingAndTax(reservation, new BigDecimal("150.00"), settings, "test");
        
        assertEquals(BigDecimal.ZERO, reservation.getShippingCost());
        assertEquals(BigDecimal.ZERO, reservation.getTaxAmount());
    }

    @Test
    void calculateAndSetShippingAndTax_WithDistanceCost() {
        Reservation reservation = Reservation.builder()
                .deliveryMethod(DeliveryMethod.DELIVERY)
                .shippingAddress("Addr 123")
                .latitude(1.0)
                .longitude(1.0)
                .build();
        AppSettings settings = AppSettings.builder()
                .freeShippingThreshold(new BigDecimal("1000.00"))
                .distanceShippingEnabled(true)
                .storeLatitude(0.0)
                .storeLongitude(0.0)
                .taxEnabled(true)
                .taxRate(new BigDecimal("10.0"))
                .regionalTaxRates(java.util.Map.of("test", new BigDecimal("10.0")))
                .build();
        
        com.productos.mari.domain.user.User mockUser = com.productos.mari.domain.user.User.builder()
                .location("test")
                .build();
        reservation.setUser(mockUser);

        when(geoRoutingService.calculateDistanceShippingCost(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new BigDecimal("5000.00"));

        calculator.calculateAndSetShippingAndTax(reservation, new BigDecimal("100.00"), settings, "test");

        assertEquals(new BigDecimal("5000.00"), reservation.getShippingCost());
        // 10% of 100 = 10
        assertEquals(new BigDecimal("10"), reservation.getTaxAmount());
    }

    @Test
    void normalizePrice_Handling() {
        assertEquals(new BigDecimal("0"), calculator.normalizePrice(null));
        // 5.0 -> 5000.0
        assertEquals(new BigDecimal("5000.0"), calculator.normalizePrice(new BigDecimal("5.0")));
        // 1500.0 -> 1500.0
        assertEquals(new BigDecimal("1500.0"), calculator.normalizePrice(new BigDecimal("1500.0")));
    }
}
