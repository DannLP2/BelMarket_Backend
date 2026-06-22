package com.productos.mari.domain.auth;

import com.productos.mari.domain.auth.RefreshToken;
import com.productos.mari.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByUserInfo(User user);
    java.util.List<RefreshToken> findAllByUserInfo(User user);
    int deleteByUserInfo(User user);
}
