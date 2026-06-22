package com.productos.mari.domain.user;

import com.productos.mari.domain.user.UserLinkedDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserLinkedDeviceRepository extends JpaRepository<UserLinkedDevice, Long> {
    List<UserLinkedDevice> findByUserId(Long userId);
    List<UserLinkedDevice> findByProductId(Long productId);
    boolean existsByUserIdAndProductId(Long userId, Long productId);
}
