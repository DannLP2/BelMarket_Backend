package com.productos.mari.domain.auth;

import com.productos.mari.domain.user.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private Long id;
    private String token;
    private String refreshToken;
    private String email;
    private String name;
    private String phone;
    private Set<Role> roles;
    private String profilePictureUrl;
    private String documentType;
    private String documentNumber;
    private java.time.LocalDate birthDate;
    private String gender;
    private String location;
    private String message;
    private com.productos.mari.domain.user.DashboardType defaultDashboard;
    private boolean needsVerification;
    private java.time.LocalDateTime createdAt;
}
