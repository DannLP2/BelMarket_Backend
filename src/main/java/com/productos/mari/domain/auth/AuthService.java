package com.productos.mari.domain.auth;

import com.productos.mari.domain.auth.AuthRequest;
import com.productos.mari.domain.auth.AuthResponse;
import com.productos.mari.domain.auth.RegisterRequest;

import com.productos.mari.domain.auth.VerificationRequest;
import com.productos.mari.domain.auth.ForgotPasswordRequest;
import com.productos.mari.domain.auth.ResetPasswordRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request, org.springframework.web.multipart.MultipartFile image);
    AuthResponse authenticate(AuthRequest request);
    AuthResponse verifyEmail(VerificationRequest request);
    AuthResponse forgotPassword(ForgotPasswordRequest request);
    AuthResponse resetPassword(ResetPasswordRequest request);
    AuthResponse getMe();
    void logout();
}
