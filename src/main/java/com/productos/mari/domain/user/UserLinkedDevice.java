package com.productos.mari.domain.user;
import com.productos.mari.domain.user.User;
import com.productos.mari.domain.product.Product;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_linked_devices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLinkedDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String serialNumber;

    @CreationTimestamp
    private LocalDateTime linkedAt;
}
