package com.productos.mari.domain.infrastructure.routing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeoRoutingServiceImplTest {

    @Mock private HttpClient httpClient;
    private ObjectMapper objectMapper = new ObjectMapper();
    private GeoRoutingServiceImpl geoRoutingService;

    @BeforeEach
    void setUp() {
        geoRoutingService = new GeoRoutingServiceImpl(objectMapper, httpClient);
    }

    @Test
    void calculateDistanceShippingCost_WithDirectCoordinates() {
        BigDecimal result = geoRoutingService.calculateDistanceShippingCost(
                "ignored", 1.0, 1.0, 0.0, 0.0, 
                1.0, new BigDecimal("5000"), new BigDecimal("1000")
        );

        // Distance between (0,0) and (1,1) is approx 157km. 
        // 157 * 1.35 = 212km approx.
        // Base is 1km. Extra is 211km. 5000 + (211 * 1000) = 216000 approx.
        assertTrue(result.compareTo(new BigDecimal("5000")) > 0);
    }

    @Test
    void calculateDistanceShippingCost_WithinBaseDistance() {
        BigDecimal result = geoRoutingService.calculateDistanceShippingCost(
                "ignored", 0.001, 0.001, 0.0, 0.0, 
                10.0, new BigDecimal("5000"), new BigDecimal("1000")
        );

        assertEquals(0, result.compareTo(new BigDecimal("5000")));
    }

    @Test
    void calculateDistanceShippingCost_GeocodingFallback() throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("[{\"lat\":\"1.0\", \"lon\":\"1.0\"}]");
        
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        BigDecimal result = geoRoutingService.calculateDistanceShippingCost(
                "Calle 123", null, null, 0.0, 0.0, 
                1.0, new BigDecimal("5000"), new BigDecimal("1000")
        );

        assertTrue(result.compareTo(new BigDecimal("5000")) > 0);
        verify(httpClient).send(any(), any());
    }

    @Test
    void calculateDistanceShippingCost_GeocodingError_ReturnsDefault() throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(404);
        
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        BigDecimal result = geoRoutingService.calculateDistanceShippingCost(
                "Calle 123", null, null, 0.0, 0.0, 
                1.0, new BigDecimal("5000"), new BigDecimal("1000")
        );

        assertEquals(0, result.compareTo(new BigDecimal("5000")));
    }
}
