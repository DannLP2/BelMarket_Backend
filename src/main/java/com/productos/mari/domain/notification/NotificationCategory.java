package com.productos.mari.domain.notification;

/**
 * Categorías de notificación para diferenciación visual y funcional en BelMarket.
 */
public enum NotificationCategory {
    INFO,       // Avisos generales, soporte básico
    SUCCESS,    // Operaciones exitosas (perfil, registro)
    WARNING,    // Alertas preventivas (stock bajo)
    ERROR,      // Fallos críticos
    SECURITY,   // Logins nuevos, cambios de clave, auditoría
    ORDER,      // Flujo de pedidos (nuevo, enviado, pagado)
    PROMO,      // Ofertas, cupones, anuncios administrador
    IOT         // Eventos de dispositivos inteligentes (Mecatronic)
}
