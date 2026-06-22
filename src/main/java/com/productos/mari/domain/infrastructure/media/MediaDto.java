package com.productos.mari.domain.infrastructure.media;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaDto {
    private String publicId;
    private String url;
    private String format;
    private long bytes;
    private String createdAt;
    private boolean inUse;
    @Builder.Default
    private List<MediaUsageDto> usedIn = new ArrayList<>();
}
