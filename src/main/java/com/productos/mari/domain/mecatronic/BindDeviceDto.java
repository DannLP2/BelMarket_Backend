package com.productos.mari.domain.mecatronic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BindDeviceDto {
    private String serial;
    private String pin;
}
