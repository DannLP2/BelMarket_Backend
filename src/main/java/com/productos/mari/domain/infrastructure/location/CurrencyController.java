package com.productos.mari.domain.infrastructure.location;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public/currency")
@RequiredArgsConstructor
public class CurrencyController {

    private final IpLocationService ipLocationService;

    @GetMapping("/rates/{base}")
    public ResponseEntity<Map<String, Object>> getRates(@PathVariable String base) {
        return ResponseEntity.ok(ipLocationService.getExchangeRates(base));
    }
}
