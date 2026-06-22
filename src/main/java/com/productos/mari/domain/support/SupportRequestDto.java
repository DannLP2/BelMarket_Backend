package com.productos.mari.domain.support;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportRequestDto {
    private Long id;
    private String name;
    private String email;
    private String requestType;
    private String orderNumber;
    private String message;
    private String status;
    private java.time.LocalDateTime createdAt;
}
