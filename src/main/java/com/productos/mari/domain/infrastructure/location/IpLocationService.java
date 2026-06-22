package com.productos.mari.domain.infrastructure.location;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class IpLocationService {

    private final RestTemplate restTemplate;

    /**
     * Detects the currency code based on the given IP address.
     * Uses ipapi.co (free tier).
     * @param ip The client IP address
     * @return The currency code (e.g. "COP", "USD") or "COP" as default fallback.
     */
    public String getCurrencyFromIp(String ip) {
        if (ip == null || ip.equals("0:0:0:0:0:0:0:1") || ip.equals("127.0.0.1") || ip.startsWith("192.168.") || ip.equals("0.0.0.0")) {
            log.info("IP local o nula detectada ({}), usando moneda por defecto: COP", ip);
            return "COP";
        }

        try {
            String url = "https://ipapi.co/" + ip + "/json/";
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && response.containsKey("currency")) {
                String currency = (String) response.get("currency");
                log.info("Ubicación detectada para IP {}: {}", ip, currency);
                return currency != null ? currency : "COP";
            }
        } catch (Exception e) {
            log.warn("No se pudo detectar ubicación por IP ({}): {}", ip, e.getMessage());
        }

        return "COP";
    }

    /**
     * Proxies exchange rate requests to api.exchangerate-api.com
     * @param base The base currency code (e.g. "COP")
     * @return Map containing rates and timestamp.
     */
    public Map<String, Object> getExchangeRates(String base) {
        try {
            String url = "https://api.exchangerate-api.com/v4/latest/" + (base != null ? base : "COP");
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            return response;
        } catch (Exception e) {
            log.error("Error al obtener tasas de cambio desde el servidor: {}", e.getMessage());
            return Map.of("error", "Failed to fetch rates", "rates", Map.of());
        }
    }
}
