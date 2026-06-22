package com.productos.mari.domain.mecatronic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MecatronicDashboardDto {
    private Long id;
    private Long productId;
    private String deviceSerial;
    private String apiKey;
    private List<VariableDto> variables;
    private String productName;
    private String productImageUrl;
    private String productSlug;
    private Boolean isExternallyLinked;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariableDto {
        private Long id;
        private String key;
        private String label;
        private String unit;
        private String type;
        private String icon;
        private String lastValue;
        private Double minValue;
        private Double maxValue;
        private List<ReadingDto> history;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReadingDto {
        private String value;
        private String timestamp;
    }
}
