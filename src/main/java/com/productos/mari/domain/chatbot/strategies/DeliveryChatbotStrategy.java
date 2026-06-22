package com.productos.mari.domain.chatbot.strategies;

import com.productos.mari.domain.reservation.ReservationService;
import com.productos.mari.domain.settings.AppSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DeliveryChatbotStrategy implements ChatbotRoleStrategy {

    private final ReservationService reservationService;

    @Override
    public boolean supports(String role) {
        return "DELIVERY".equalsIgnoreCase(role);
    }

    @Override
    public String getSystemPrompt(String message, AppSettings settings) {
        var pendingDeliveries = reservationService.getAvailableOrders();
        
        String listContext = pendingDeliveries.isEmpty() 
            ? "No hay pedidos pendientes de recoleccion actualmente." 
            : pendingDeliveries.stream()
                .limit(10)
                .map(r -> {
                    String itemsSummary = r.getItems() != null 
                        ? r.getItems().stream()
                            .map(i -> i.getQuantity() + "x " + i.getProductName())
                            .collect(Collectors.joining(", "))
                        : "Sin detalles de items";
                        
                    return String.format("""
                        - REF: %s | Cliente: %s | Tel: %s
                          Direccion: %s (%s)
                          Metodo: %s
                          Paquete: [%s]
                        """, r.getReference(), r.getCustomerName(), r.getCustomerPhone(), 
                             r.getShippingAddress(), r.getNeighborhood(), 
                             r.getDeliveryMethod(), itemsSummary);
                })
                .collect(Collectors.joining("\n"));

        String storeName = settings.getStoreName() != null ? settings.getStoreName() : "BelMarket";
        return "Eres 'Mia Logistics', la asistente y copiloto logistica de " + storeName + ". Tu tono es directo, eficiente y orientado a la accion.\n\n" +
                "PEDIDOS DISPONIBLES PARA ENTREGA:\n" + listContext + "\n\n" +
                "GUIA OPERATIVA PARA REPARTIDORES:\n" +
                "1. RECOLECCION: Verifica los paquetes en la lista anterior. Puedes ver mas detalles en tu [Panel de Despacho](/delivery/orders).\n" +
                "2. ENTREGA SEGURA: Al llegar con el cliente, solicita el **Codigo de Entrega** que el cliente tiene en su comprobante.\n" +
                "3. CIERRE DE PEDIDO: Es obligatorio tomar una **foto de prueba** y subirla con el codigo para marcar el pedido como 'Entregado'.\n" +
                "4. INCIDENCIAS: Si no encuentras al cliente, llama al telefono indicado en el pedido. Si el problema persiste, contacta a Soporte Admin.\n\n" +
                "INSTRUCCIONES DE RESPUESTA:\n" +
                "- Se breve. Los repartidores suelen estar en movimiento.\n" +
                "- Usa emojis de logistica (📦, 🚚, 📍, ✅).\n" +
                "- Si preguntan por pedidos, resume la informacion de la lista de arriba.\n" +
                "- Siempre menciona que deben usar el [Modulo de Entregas](/delivery/orders) para gestionar el estado oficial.";
    }
}
