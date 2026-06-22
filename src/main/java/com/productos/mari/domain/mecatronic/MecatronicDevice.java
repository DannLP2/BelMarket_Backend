package com.productos.mari.domain.mecatronic;
import com.productos.mari.domain.product.Product;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "mecatronic_devices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MecatronicDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "device_serial", unique = true)
    private String deviceSerial;

    @Column(name = "api_key", unique = true, nullable = false)
    private String apiKey;

    @Column(name = "last_connection")
    private LocalDateTime lastConnection;

    @Column(name = "is_enabled")
    @Builder.Default
    private Boolean isEnabled = true;

    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DeviceVariable> variables = new java.util.ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.apiKey == null) {
            this.apiKey = UUID.randomUUID().toString();
        }
    }
}
