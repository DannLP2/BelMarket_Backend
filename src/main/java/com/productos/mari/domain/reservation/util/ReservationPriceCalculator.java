package com.productos.mari.domain.reservation.util;

import com.productos.mari.domain.settings.AppSettings;
import com.productos.mari.domain.reservation.Reservation;
import com.productos.mari.domain.reservation.DeliveryMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationPriceCalculator {

    private final com.productos.mari.domain.infrastructure.routing.GeoRoutingService geoRoutingService;

    public void calculateAndSetShippingAndTax(Reservation reservation, BigDecimal subtotal, AppSettings settings, String context) {
        BigDecimal shippingCost = BigDecimal.ZERO;
        log.info("Reservation {}: Subtotal for shipping: {}", context, subtotal);

        if (reservation.getDeliveryMethod() == DeliveryMethod.DELIVERY &&
                reservation.getShippingAddress() != null && !reservation.getShippingAddress().trim().isEmpty()) {
            
            // Check for free shipping threshold
            BigDecimal freeShippingThreshold = settings.getFreeShippingThreshold() != null 
                ? settings.getFreeShippingThreshold() 
                : BigDecimal.valueOf(100000); // Default

            if (subtotal.compareTo(freeShippingThreshold) < 0) {
                // If distance shipping is enabled
                if (Boolean.TRUE.equals(settings.getDistanceShippingEnabled()) && settings.getStoreLatitude() != null
                        && settings.getStoreLongitude() != null) {
                    shippingCost = geoRoutingService.calculateDistanceShippingCost(
                            reservation.getShippingAddress(),
                            reservation.getLatitude(),
                            reservation.getLongitude(),
                            settings.getStoreLatitude(),
                            settings.getStoreLongitude(),
                            settings.getBaseDistanceKm(),
                            settings.getDefaultShippingCost(),
                            settings.getCostPerKm());
                } else {
                    log.info("Reservation {}: Distance shipping disabled or store coordinates missing. Using default.", context);
                    shippingCost = settings.getDefaultShippingCost() != null ? settings.getDefaultShippingCost() : BigDecimal.ZERO;
                }
            } else {
                log.info("Reservation {}: Free shipping threshold reached.", context);
            }
        }

        reservation.setShippingCost(shippingCost);
        log.info("Reservation {}: Final shipping cost: {}", context, shippingCost);
        
        // Tax logic
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal taxRate = BigDecimal.ZERO;
        
        if (Boolean.TRUE.equals(settings.getTaxEnabled())) {
            String userLocation = (reservation.getUser() != null) ? reservation.getUser().getLocation() : null;
            
            // Check for regional rate first (priority)
            if (userLocation != null && settings.getRegionalTaxRates() != null && settings.getRegionalTaxRates().containsKey(userLocation)) {
                taxRate = settings.getRegionalTaxRates().get(userLocation);
                log.info("Reservation {}: Using REGIONAL tax rate for {}: {}%", context, userLocation, taxRate);
            } else {
                log.info("Reservation {}: No regional tax rate found for {}. Defaulting to 0% as per new policy.", context, userLocation);
                taxRate = BigDecimal.ZERO;
            }
        }

        if (taxRate.compareTo(BigDecimal.ZERO) > 0) {
            taxAmount = subtotal.multiply(taxRate)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        }
        reservation.setTaxRate(taxRate); // Important to store which rate was applied
        reservation.setTaxAmount(taxAmount);
        
        log.info("Reservation {}: Final Tax: {} (Rate: {}%)", context, taxAmount, taxRate);
    }

    public BigDecimal normalizePrice(BigDecimal price) {
        if (price == null) return BigDecimal.ZERO;
        if (price.compareTo(BigDecimal.valueOf(1000)) < 0 && price.compareTo(BigDecimal.ZERO) > 0) {
            return price.multiply(BigDecimal.valueOf(1000));
        }
        return price;
    }
}
