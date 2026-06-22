package com.productos.mari.domain.auth;

import com.productos.mari.domain.user.User;
import com.productos.mari.domain.user.UserRepository;
import com.productos.mari.exception.TokenRefreshException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenRefreshServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenDurationMs", 3600000L);
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@test.com");
    }

    @Test
    void createRefreshToken_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        RefreshToken result = refreshTokenService.createRefreshToken(1L);

        assertNotNull(result);
        assertEquals(mockUser, result.getUserInfo());
        assertNotNull(result.getToken());
        assertTrue(result.getExpiryDate().isAfter(Instant.now()));
    }

    @Test
    void verifyExpiration_ValidToken_ReturnsToken() {
        RefreshToken token = new RefreshToken();
        token.setExpiryDate(Instant.now().plusSeconds(60));
        token.setRevoked(false);

        RefreshToken result = refreshTokenService.verifyExpiration(token);
        assertEquals(token, result);
    }

    @Test
    void verifyExpiration_ExpiredToken_ThrowsAndDeletes() {
        RefreshToken token = new RefreshToken();
        token.setToken("expired-token");
        token.setExpiryDate(Instant.now().minusSeconds(60));

        assertThrows(TokenRefreshException.class, () -> refreshTokenService.verifyExpiration(token));
        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void verifyExpiration_RevokedToken_Throws() {
        RefreshToken token = new RefreshToken();
        token.setToken("revoked-token");
        token.setRevoked(true);

        assertThrows(TokenRefreshException.class, () -> refreshTokenService.verifyExpiration(token));
    }

    @Test
    void revokeToken_SetsRevokedTrue() {
        RefreshToken token = new RefreshToken();
        token.setToken("token-to-revoke");
        token.setRevoked(false);

        when(refreshTokenRepository.findByToken("token-to-revoke")).thenReturn(Optional.of(token));

        refreshTokenService.revokeToken("token-to-revoke");

        assertTrue(token.isRevoked());
        verify(refreshTokenRepository).save(token);
    }
}
