package com.productos.mari.domain.infrastructure.communication;

import com.productos.mari.domain.reservation.ReservationDto;

public interface EmailService {
    void sendWelcomeEmail(String toEmail, String name);
    void sendVerificationEmail(String toEmail, String name, String code);
    void sendPasswordResetEmail(String toEmail, String name, String code);
    void sendReservationConfirmation(String toEmail, String name, ReservationDto reservation, byte[] pdfReceipt);
    void sendReservationStatusUpdate(String toEmail, String name, String reservationReference, com.productos.mari.domain.reservation.ReservationStatus newStatus, String deliveryCode);
    void sendPasswordChangeNotification(String toEmail, String name);
    void sendAccountStatusNotification(String toEmail, String name, boolean isEnabled);
    void sendBroadcastEmail(String toEmail, String subject, String message);
    void sendSupportNotification(com.productos.mari.domain.support.SupportRequest request);
    void sendSupportConfirmation(com.productos.mari.domain.support.SupportRequest request);
    void sendNewOfferEmail(com.productos.mari.domain.user.User user, com.productos.mari.domain.product.Product product, com.productos.mari.domain.marketing.Offer offer);
    void sendRoleUpdateNotification(String toEmail, String name, java.util.Set<com.productos.mari.domain.user.Role> roles);
}
