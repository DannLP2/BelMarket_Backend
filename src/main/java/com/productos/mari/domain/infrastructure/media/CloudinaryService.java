package com.productos.mari.domain.infrastructure.media;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface CloudinaryService {
    String uploadFile(MultipartFile file) throws IOException;
    String uploadFile(MultipartFile file, String folder) throws IOException;
    String uploadFile(MultipartFile file, String folder, String format) throws IOException;
    String uploadFile(MultipartFile file, String folder, String format, String resourceType) throws IOException;
    String uploadBase64(String base64Content, String folder) throws IOException;
    void deleteFile(String publicId) throws IOException;
    void deleteFile(String publicId, String resourceType) throws IOException;
    String extractPublicId(String url);
    java.util.List<com.productos.mari.domain.infrastructure.media.MediaDto> listResources() throws Exception;
}
