package com.productos.mari.domain.infrastructure.media;

import com.productos.mari.domain.infrastructure.media.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

// @Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path root;

    public FileStorageServiceImpl(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir);
        try {
            if (!Files.exists(root)) {
                Files.createDirectories(root);
            }
        } catch (IOException e) {
            throw new RuntimeException("No se pudo inicializar la carpeta de uploads: " + uploadDir);
        }
    }

    @Override
    public String saveFile(MultipartFile file) {
        try {
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Files.copy(file.getInputStream(), this.root.resolve(fileName));
            return fileName;
        } catch (Exception e) {
            throw new RuntimeException("No se pudo almacenar el archivo: " + e.getMessage());
        }
    }

    @Override
    public String saveBase64(String base64Content, String fileNamePrefix) {
        try {
            String base64Data = base64Content;
            String extension = "jpg";
            
            if (base64Content.contains(",")) {
                String header = base64Content.split(",")[0];
                base64Data = base64Content.split(",")[1];
                if (header.contains("png")) extension = "png";
                else if (header.contains("jpeg") || header.contains("jpg")) extension = "jpg";
            }
            
            byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64Data);
            String fileName = fileNamePrefix + "_" + UUID.randomUUID().toString() + "." + extension;
            Files.write(this.root.resolve(fileName), decodedBytes);
            return fileName;
        } catch (Exception e) {
            throw new RuntimeException("No se pudo almacenar la imagen base64: " + e.getMessage());
        }
    }

    @Override
    public void deleteFile(String fileName) {
        try {
            Path file = root.resolve(fileName);
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new RuntimeException("Error al eliminar el archivo: " + e.getMessage());
        }
    }
}

