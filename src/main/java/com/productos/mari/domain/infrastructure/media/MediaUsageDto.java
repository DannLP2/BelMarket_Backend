package com.productos.mari.domain.infrastructure.media;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaUsageDto {
    private String type;
    private String detail;
    private String name;
    private String url;
}
