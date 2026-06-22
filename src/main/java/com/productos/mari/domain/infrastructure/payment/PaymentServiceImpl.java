package com.productos.mari.domain.infrastructure.payment;

import lombok.extern.slf4j.Slf4j;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.resources.payment.Payment;
import com.productos.mari.domain.reservation.Reservation;
import com.productos.mari.domain.reservation.ReservationStatus;
import com.productos.mari.domain.reservation.ReservationItem;
import com.productos.mari.domain.infrastructure.payment.PaymentService;
import com.productos.mari.domain.reservation.ReservationService;
import com.productos.mari.domain.reservation.ReservationRepository;
import com.productos.mari.domain.product.Product;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;

    @Value("${mercadopago.access.token:TEST-DUMMY}")
    private String mercadoPagoAccessToken;

    @Value("${app.webhook.url:http://localhost:8080/api}")
    private String webhookBaseUrl;

    @Value("${app.client.url:http://localhost:4200}")
    private String clientBaseUrl;

    @PostConstruct
    public void init() {
        MercadoPagoConfig.setAccessToken(mercadoPagoAccessToken);
    }

    @Override
    public String createPaymentPreference(Long reservationId, String userEmail) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

        if (!reservation.getUser().getEmail().equals(userEmail)) {
            throw new IllegalArgumentException("No autorizado para pagar esta reserva");
        }

        // VALIDAR STOCK ANTES DE PERMITIR EL PAGO
        for (ReservationItem ri : reservation.getItems()) {
            Product product = ri.getProduct();
            if (product.getStock() < ri.getQuantity()) {
                throw new IllegalArgumentException("No hay stock suficiente para: " + product.getName() + 
                    " (Disponible: " + product.getStock() + ")");
            }
        }

        try {
            PreferenceClient client = new PreferenceClient();
            List<PreferenceItemRequest> items = new ArrayList<>();

            for (ReservationItem ri : reservation.getItems()) {
                PreferenceItemRequest itemRequest = PreferenceItemRequest.builder()
                        .title(ri.getProduct().getName())
                        .quantity(ri.getQuantity())
                        .unitPrice(ri.getPrice())
                        .currencyId("COP") // Moneda local
                        .build();
                items.add(itemRequest);
            }

            if (reservation.getShippingCost() != null && reservation.getShippingCost().compareTo(java.math.BigDecimal.ZERO) > 0) {
                PreferenceItemRequest shippingRequest = PreferenceItemRequest.builder()
                        .title("Costo de Envío")
                        .quantity(1)
                        .unitPrice(reservation.getShippingCost())
                        .currencyId("COP")
                        .build();
                items.add(shippingRequest);
            }

            if (reservation.getTaxAmount() != null && reservation.getTaxAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                String taxTitle = "Impuestos (IVA" + (reservation.getTaxRate() != null ? " " + reservation.getTaxRate() + "%" : "") + ")";
                PreferenceItemRequest taxRequest = PreferenceItemRequest.builder()
                        .title(taxTitle)
                        .quantity(1)
                        .unitPrice(reservation.getTaxAmount())
                        .currencyId("COP")
                        .build();
                items.add(taxRequest);
            }

            PreferencePayerRequest payer = PreferencePayerRequest.builder()
                    .email(userEmail)
                    .build();

            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success(clientBaseUrl + "/reservations")
                    .pending(clientBaseUrl + "/reservations")
                    .failure(clientBaseUrl + "/reservations")
                    .build();

            PreferenceRequest request = PreferenceRequest.builder()
                    .items(items)
                    .payer(payer)
                    .backUrls(backUrls)
                    .notificationUrl(webhookBaseUrl + "/payments/webhook")
                    .externalReference(reservationId.toString())
                    .build();

            Preference preference = client.create(request);
            
            // Retorna el enlace mágico de pago de MercadoPago en modo Sandbox (para pruebas)
            return preference.getSandboxInitPoint();

        } catch (MPException | MPApiException e) {
            log.error("Error creating MercadoPago preference for reservation {}: {}", reservationId, e.getMessage());
            if (e instanceof MPApiException) {
                MPApiException apiEx = (MPApiException) e;
                if (apiEx.getApiResponse() != null && apiEx.getApiResponse().getContent() != null) {
                    log.error("MERCADOPAGO API DETAIL: {}", apiEx.getApiResponse().getContent());
                }
            }
            throw new IllegalArgumentException("Error al comunicarse con MercadoPago: " + e.getMessage());
        }
    }

    @Override
    public void processWebhookNotification(java.util.Map<String, Object> payload) {
        try {
            // MercadoPago envía el ID del recurso y el tipo (topic)
            String topic = (String) payload.get("topic");
            String action = (String) payload.get("action");
            
            // Atender tanto por topic (IPN antigua) como por action (Webhooks nuevos)
            if ("payment".equals(topic) || "payment.created".equals(action)) {
                String idStr = "";
                if (payload.containsKey("id")) idStr = payload.get("id").toString();
                else if (payload.containsKey("data")) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> data = (java.util.Map<String, Object>) payload.get("data");
                    idStr = data.get("id").toString();
                }

                if (idStr == null || idStr.isEmpty()) return;

                Long paymentId = Long.parseLong(idStr);
                PaymentClient client = new PaymentClient();
                Payment payment = client.get(paymentId);

                if ("approved".equals(payment.getStatus())) {
                    String externalRef = payment.getExternalReference();
                    if (externalRef != null) {
                        Long reservationId = Long.parseLong(externalRef);
                        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
                        if (reservation != null && reservation.getStatus() == ReservationStatus.PENDING) {
                            // Capturar detalles de pago para el recibo PDF
                            reservation.setPaymentMethod("ONLINE");
                            reservation.setPaymentId(payment.getId().toString());
                            
                            // Traducir o limpiar el sub-método (ej: "account_money", "credit_card", "pse")
                            String pMethod = payment.getPaymentMethodId(); // ej: "visa", "pse"
                            String pType = payment.getPaymentTypeId(); // ej: "credit_card", "bank_transfer"
                            reservation.setPaymentSubMethod(pMethod.toUpperCase() + " (" + pType + ")");
                            
                            // USAR EL SERVICIO PARA QUE DISPARE EL DESCUENTO DE STOCK Y NOTIFICACIONES
                            reservationService.updateReservationStatus(reservationId, ReservationStatus.CONFIRMED);
                            log.info("WEBHOOK: Pago APROBADO via {} y Stock reservado para Reservación #{}", pMethod, reservationId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("ERROR PROCESANDO WEBHOOK: " + e.getMessage());
        }
    }
}
