package com.productos.mari.domain.user;
import com.productos.mari.domain.user.User;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "addresses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String title; // "Casa", "Oficina", "Trabajo"

    @Column(nullable = false, length = 200)
    private String street; // Dirección principal

    @Column(nullable = false, length = 100)
    private String city; // Ciudad o comuna

    @Column(length = 100)
    private String department;

    @Column(length = 100)
    private String country;

    @Column(length = 100)
    private String neighborhood;

    @Column(name = "apartment_office", length = 100)
    private String apartmentOffice;

    @Column(name = "receiver_name", length = 100)
    private String receiverName;

    @Column(name = "receiver_phone", length = 50)
    private String receiverPhone;

    @Column(length = 255)
    private String reference; // Referencia para el repartidor

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "is_default", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean isDefault = false;
}
