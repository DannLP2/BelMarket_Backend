package com.productos.mari.domain.auth;

import com.productos.mari.domain.user.User;
import com.productos.mari.domain.user.UserRepository;
import com.productos.mari.domain.auth.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/otp")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;
    private final UserRepository userRepository;

    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> sendOtp(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        otpService.generateAndSendOtp(user.getEmail(), user.getName());
        return ResponseEntity.ok(Map.of("message", "Código OTP enviado al correo."));
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyOtp(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> request) {
        String code = request.get("code");
        boolean isValid = otpService.verifyOtp(userDetails.getUsername(), code);
        
        if (isValid) {
            return ResponseEntity.ok(Map.of("message", "Verificación Exitosa."));
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "El código es inválido o ha expirado."));
        }
    }
}
