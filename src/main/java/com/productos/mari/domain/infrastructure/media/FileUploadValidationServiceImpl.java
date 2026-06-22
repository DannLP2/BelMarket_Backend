package com.productos.mari.domain.infrastructure.media;

import com.productos.mari.domain.infrastructure.media.FileUploadValidationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileUploadValidationServiceImpl implements FileUploadValidationService {

    @Value("${app.upload.max-image-size:2MB}")
    private DataSize maxImageSize;

    @Value("${app.upload.max-pdf-size:5MB}")
    private DataSize maxPdfSize;

    @Override
    public void validateImage(MultipartFile file) {
        validateFile(file, maxImageSize.toBytes(), "Imagen");
        if (file != null && !file.isEmpty()) {
            String contentType = file.getContentType();
            String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
            
            boolean isValidType = contentType != null && (
                contentType.equals("image/jpeg") || 
                contentType.equals("image/png") || 
                contentType.equals("image/webp")
            );
            
            boolean isValidExtension = fileName.endsWith(".jpg") || 
                                     fileName.endsWith(".jpeg") || 
                                     fileName.endsWith(".png") || 
                                     fileName.endsWith(".webp");

            if (!isValidType || !isValidExtension) {
                throw new IllegalArgumentException("Tipo de imagen no permitido. Solo se aceptan JPG, PNG o WEBP.");
            }
        }
    }

    @Override
    public void validatePdf(MultipartFile file) {
        validateFile(file, maxPdfSize.toBytes(), "PDF");
        if (file != null && !file.isEmpty()) {
            String contentType = file.getContentType();
            String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";

            boolean isValidType = "application/pdf".equals(contentType);
            boolean isValidExtension = fileName.endsWith(".pdf");

            if (!isValidType || !isValidExtension) {
                throw new IllegalArgumentException("Tipo de archivo no permitido. Solo se aceptan documentos PDF.");
            }
        }
    }

    @Override
    public void validateFile(MultipartFile file, long maxSizeInBytes, String label) {
        if (file != null && !file.isEmpty()) {
            if (file.getSize() > maxSizeInBytes) {
                int mb = (int) (maxSizeInBytes / (1024 * 1024));
                throw new IllegalArgumentException(String.format("El archivo %s (%s) excede el tamaño máximo permitido de %d MB", 
                    file.getOriginalFilename(), label, mb));
            }
        }
    }

    @Override
    public int getMaxImageSizeMb() {
        return (int) maxImageSize.toMegabytes();
    }

    @Override
    public int getMaxPdfSizeMb() {
        return (int) maxPdfSize.toMegabytes();
    }
}
