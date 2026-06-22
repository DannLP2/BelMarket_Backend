package com.productos.mari.domain.infrastructure.payment;

import com.productos.mari.domain.infrastructure.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-preference")
    public ResponseEntity<String> createPreference(@RequestParam Long reservationId, @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(paymentService.createPaymentPreference(reservationId, userDetails.getUsername()));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(@RequestBody java.util.Map<String, Object> payload) {
        paymentService.processWebhookNotification(payload);
        return ResponseEntity.ok().build();
    }
}
