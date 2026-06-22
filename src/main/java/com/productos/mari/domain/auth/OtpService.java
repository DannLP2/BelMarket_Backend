package com.productos.mari.domain.auth;
import com.productos.mari.domain.infrastructure.communication.EmailService;

import com.productos.mari.domain.auth.Otp;
import com.productos.mari.domain.auth.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpRepository otpRepository;
    private final EmailService emailService;

    @Transactional
    public void generateAndSendOtp(String email, String name) {
        // Remove previous OTPs for this email to prevent multiple valid codes
        otpRepository.deleteByEmail(email);

        String code = String.format("%06d", new Random().nextInt(999999));
        
        Otp otp = Otp.builder()
                .email(email)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(5)) // Valid for 5 minutes
                .build();
                
        otpRepository.save(otp);
        
        // Use the EmailService to send it. (If SMTP fails, the emailService logs it as fallback)
        try {
            emailService.sendVerificationEmail(email, name != null ? name : "Cliente", code);
            log.info("OTP {} generated and scheduled for {} (Valid for 5m)", code, email);
        } catch (Exception e) {
            log.warn("SMTP integration missing or failed. The OTP for {} is: {}", email, code);
        }
    }

    @Transactional
    public boolean verifyOtp(String email, String code) {
        Optional<Otp> otpOpt = otpRepository.findByEmailAndCode(email, code);
        
        if (otpOpt.isEmpty()) {
            return false;
        }

        Otp otp = otpOpt.get();
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            otpRepository.delete(otp); // Clean up expired
            return false;
        }

        // Successfully verified, consume it
        otpRepository.delete(otp);
        return true;
    }
}
