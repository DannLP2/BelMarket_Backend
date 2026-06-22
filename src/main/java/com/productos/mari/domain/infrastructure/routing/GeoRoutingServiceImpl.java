package com.productos.mari.domain.infrastructure.routing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.productos.mari.domain.infrastructure.routing.GeoRoutingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@Service
public class GeoRoutingServiceImpl implements GeoRoutingService {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public GeoRoutingServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    // Constructor for testing
    public GeoRoutingServiceImpl(ObjectMapper objectMapper, HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public BigDecimal calculateDistanceShippingCost(String customerAddress, Double latitude, Double longitude, Double storeLat, Double storeLng, Double baseDistanceKm, BigDecimal defaultShippingCost, BigDecimal costPerExtraKm) {
        // Fallback strategy if parameters are invalid
        if (storeLat == null || storeLng == null) {
            log.warn("GeoRouting: Store coordinates are missing. Cannot calculate distance.");
            return defaultShippingCost != null ? defaultShippingCost : BigDecimal.ZERO;
        }

        // Ensure defaults if null
        baseDistanceKm = baseDistanceKm != null ? baseDistanceKm : 0.0;
        costPerExtraKm = costPerExtraKm != null ? costPerExtraKm : BigDecimal.ZERO;
        defaultShippingCost = defaultShippingCost != null ? defaultShippingCost : BigDecimal.ZERO;

        double customerLat;
        double customerLng;

        try {
            // Priority 1: Use direct coordinates if available
            if (latitude != null && longitude != null && Math.abs(latitude) > 0.001) {
                customerLat = latitude;
                customerLng = longitude;
                log.info("GeoRouting: Using map coordinates -> " + customerLat + ", " + customerLng);
            } 
           // Priority 2: Geocode address string as fallback
           else if (customerAddress != null && !customerAddress.trim().isEmpty()) {
               String searchAddress = customerAddress;
               
               // CLEANING: Strip title prefix (e.g. "CASA - ")
               if (searchAddress.contains(" - ")) {
                   searchAddress = searchAddress.substring(searchAddress.indexOf(" - ") + 3);
               }

               if (!searchAddress.toLowerCase().contains("popay")) {
                   searchAddress += ", Popayán, Cauca, Colombia";
               }
               
               log.info("GeoRouting: Geocoding cleaned address -> " + searchAddress);
               try {
                   String encodedAddress = URLEncoder.encode(searchAddress, StandardCharsets.UTF_8);
                   HttpRequest geocodeRequest = HttpRequest.newBuilder()
                           .uri(URI.create("https://nominatim.openstreetmap.org/search?q=" + encodedAddress + "&format=json&limit=1"))
                           .timeout(Duration.ofSeconds(15))
                           .header("User-Agent", "BelMarket-Routing-Engine/1.1 (daniel@belmarket.co)")
                           .GET()
                           .build();

                    HttpResponse<String> geocodeResponse = httpClient.send(geocodeRequest, HttpResponse.BodyHandlers.ofString());
                    
                    if (geocodeResponse.statusCode() != 200) {
                        log.error("GeoRouting: Nominatim HTTP Error: " + geocodeResponse.statusCode());
                        return defaultShippingCost;
                    }

                    JsonNode geocodeArr = objectMapper.readTree(geocodeResponse.body());
                    if (!geocodeArr.isArray() || geocodeArr.isEmpty()) {
                        log.error("GeoRouting: Nominatim returned empty for: " + searchAddress);
                        return defaultShippingCost;
                    }
                    
                    JsonNode locationNode = geocodeArr.get(0);
                    customerLat = locationNode.get("lat").asDouble();
                    customerLng = locationNode.get("lon").asDouble();
                    log.info("GeoRouting: Geocoded successfully -> " + customerLat + ", " + customerLng);
                } catch (Exception e) {
                    System.err.print("GeoRouting: Geocoding fatal error: " + e.getMessage());
                    return defaultShippingCost;
                }
            } 
            else {
                log.error("GeoRouting: No address or coordinates provided.");
                return defaultShippingCost;
            }

            // 2. Calculate Distance (Unified with Frontend)
            // We use Haversine * 1.35 factor to guarantee consistency with the cart display.
            double distanceKm = calculateHaversineDistance(storeLat, storeLng, customerLat, customerLng) * 1.35;
            log.info("GeoRouting: Final Shipping Distance -> " + distanceKm + " km");

            // Final check on results
            if (distanceKm <= baseDistanceKm) {
                log.info("GeoRouting: Within base range (" + baseDistanceKm + "km). Fixed cost applied: " + defaultShippingCost);
                return defaultShippingCost;
            } else {
                double extraKm = distanceKm - baseDistanceKm;
                BigDecimal extraCost = costPerExtraKm.multiply(BigDecimal.valueOf(extraKm));
                BigDecimal totalShipping = defaultShippingCost.add(extraCost).setScale(0, RoundingMode.HALF_UP);
                log.info("GeoRouting: Distance " + distanceKm + "km exceeds base. Total calculated cost: " + totalShipping);
                return totalShipping;
            }
        } catch (Exception e) {
            log.error("GeoRouting: Fatal error during calculation for " + customerAddress + ": " + e.getMessage());
            return defaultShippingCost;
        }
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
