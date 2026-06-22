package com.productos.mari.domain.auth;

import com.productos.mari.domain.auth.AuthRequest;
import com.productos.mari.domain.auth.AuthResponse;
import com.productos.mari.domain.auth.RegisterRequest;
import com.productos.mari.domain.auth.VerificationRequest;
import com.productos.mari.domain.auth.ForgotPasswordRequest;
import com.productos.mari.domain.auth.ResetPasswordRequest;
import com.productos.mari.domain.auth.AuthService;
import com.productos.mari.domain.auth.RefreshTokenService;
import com.productos.mari.domain.auth.JwtService;
import com.productos.mari.exception.TokenRefreshException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    @PostMapping(value = "/register", consumes = {"multipart/form-data"})
    public ResponseEntity<AuthResponse> register(
            @RequestPart("user") @Valid RegisterRequest request,
            @RequestPart(value = "image", required = false) org.springframework.web.multipart.MultipartFile image) {
        return ResponseEntity.ok(authService.register(request, image));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticate(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(@RequestBody VerificationRequest request) {
        return ResponseEntity.ok(authService.verifyEmail(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<AuthResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<com.productos.mari.domain.auth.TokenRefreshResponse> refreshToken(@RequestBody com.productos.mari.domain.auth.TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(refreshTokenService::renewToken)
                .map(refreshToken -> {
                    String token = jwtService.generateToken(refreshToken.getUserInfo());
                    return ResponseEntity.ok(new com.productos.mari.domain.auth.TokenRefreshResponse(token, refreshToken.getToken()));
                })
                .orElseThrow(() -> new TokenRefreshException(requestRefreshToken, "Refresh token no encontrado o inválido!"));
    }

    @org.springframework.web.bind.annotation.GetMapping("/me")
    public ResponseEntity<AuthResponse> getMe() {
        return ResponseEntity.ok(authService.getMe());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        authService.logout();
        return ResponseEntity.noContent().build();
    }
}
