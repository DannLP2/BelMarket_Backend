package com.productos.mari.domain.infrastructure.payment;

public interface PaymentService {
    String createPaymentPreference(Long reservationId, String userEmail);
    void processWebhookNotification(java.util.Map<String, Object> payload);
}
