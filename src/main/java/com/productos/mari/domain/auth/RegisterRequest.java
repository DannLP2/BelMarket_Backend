package com.productos.mari.domain.auth;

import com.productos.mari.domain.user.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    private String name;
    private String email;
    
    @NotBlank(message = "La contraseña es obligatoria.")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$", 
             message = "La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula y un número.")
    private String password;
    
    private String phone;
    private Set<Role> roles;
    private String profilePictureUrl;
    private String documentType;
    private String documentNumber;
    private java.time.LocalDate birthDate;
    private String gender;
    private String location;
}
