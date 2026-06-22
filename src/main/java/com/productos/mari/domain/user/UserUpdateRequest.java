package com.productos.mari.domain.user;

import com.productos.mari.domain.user.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
    
    /**
     * Marker interface for full/administrative updates that require mandatory fields.
     */
    public interface FullUpdate {}

    @NotBlank(message = "El nombre es obligatorio", groups = FullUpdate.class)
    private String name;
    
    @NotBlank(message = "El email no puede estar vacío", groups = FullUpdate.class)
    @Email(message = "Debe ser un correo electrónico válido", groups = FullUpdate.class)
    private String email;
    
    private String phone;
    private String password;
    private Set<Role> roles;
    private String documentType;
    private String documentNumber;
    private java.time.LocalDate birthDate;
    private String gender;
    private String status;
    private com.productos.mari.domain.user.DashboardType defaultDashboard;
    private String location;
    private String currentPassword;
}
