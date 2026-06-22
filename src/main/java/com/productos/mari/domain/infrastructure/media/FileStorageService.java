package com.productos.mari.domain.infrastructure.media;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String saveFile(MultipartFile file);
    String saveBase64(String base64Content, String fileNamePrefix);
    void deleteFile(String fileName);
}
