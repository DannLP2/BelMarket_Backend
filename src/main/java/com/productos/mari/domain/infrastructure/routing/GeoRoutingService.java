package com.productos.mari.domain.infrastructure.routing;

import java.math.BigDecimal;

public interface GeoRoutingService {
    /**
     * Calculates the shipping cost dynamically based on distance.
     * 
     * @param customerAddress The shipping address to geocode (fallback).
     * @param latitude Direct customer latitude if available.
     * @param longitude Direct customer longitude if available.
     * @param storeLat The store latitude.
     * @param storeLng The store longitude.
     * @param baseDistanceKm The included base distance in km.
     * @param defaultShippingCost The base shipping cost.
     * @param costPerExtraKm The cost multiplier per extra km.
     * @return The final parsed cost or default if it fails.
     */
    BigDecimal calculateDistanceShippingCost(String customerAddress, Double latitude, Double longitude, Double storeLat, Double storeLng, Double baseDistanceKm, BigDecimal defaultShippingCost, BigDecimal costPerExtraKm);
}
