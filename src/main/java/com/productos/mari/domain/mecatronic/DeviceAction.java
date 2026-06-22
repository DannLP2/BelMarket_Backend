package com.productos.mari.domain.mecatronic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "device_actions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variable_id", nullable = false)
    private DeviceVariable variable; // Linked to an ACTUATOR variable

    @Column(name = "command_value", nullable = false)
    private String commandValue; // e.g., "ON", "OFF", "50"

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ActionStatus status = ActionStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public enum ActionStatus {
        PENDING, EXECUTED, FAILED
    }
}
