package com.productos.mari.domain.mecatronic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "device_variables")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceVariable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private MecatronicDevice device;

    @Column(name = "field_key", nullable = false)
    private String fieldKey; // The key in the JSON (e.g., "temp")

    @Column(nullable = false)
    private String label; // Display name (e.g., "Temperatura")

    private String unit; // (e.g., "°C", "%")

    @Column(name = "variable_type", nullable = false)
    private String variableType; // SENSOR or ACTUATOR

    @Column(name = "ui_icon")
    private String icon; // Icon name for the dashboard

    @Column(name = "min_value")
    private Double minValue; // Safe minimum range

    @Column(name = "max_value")
    private Double maxValue; // Safe maximum range

    @OneToMany(mappedBy = "variable", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VariableReading> readings;
}
