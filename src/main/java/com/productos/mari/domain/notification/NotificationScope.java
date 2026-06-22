package com.productos.mari.domain.notification;

/**
 * Define el alcance de la notificación para manejar la visibilidad y compartición.
 */
public enum NotificationScope {
    PERSONAL,       // Notificación individual para un cliente/usuario específico (e.g. "Tu pedido #123 ha sido enviado")
    ADMIN_SHARED,   // Notificación compartida para TODO el equipo administrativo (e.g. "Nueva Reserva Recibida")
    CLIENT_SHARED,  // Notificación compartida para TODO el equipo de clientes (e.g. "Promoción de Verano")
    DELIVERER_SHARED, // Notificación compartida para TODO el equipo de repartidores (e.g. "Aviso de Tráfico")
    GLOBAL          // Notificación general para absolutamente todos los usuarios de la plataforma
}
