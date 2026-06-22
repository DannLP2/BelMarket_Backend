package com.productos.mari.domain.infrastructure.media;

import com.productos.mari.domain.infrastructure.media.MediaDto;
import java.io.IOException;
import java.util.List;

public interface MediaService {
    List<MediaDto> getAllMedia() throws Exception;
    void deleteMedia(String publicId) throws IOException;
    void deleteMediaBulk(List<String> publicIds) throws IOException;
}
