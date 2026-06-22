package com.productos.mari.domain.user;

public enum UserStatus {
    ACTIVE,             // Cuenta operativa
    INACTIVE_BY_USER,   // Desactivada por el usuario
    SUSPENDED,          // Suspendida por un administrador
    PENDING             // Pendiente de verificación de correo
}
