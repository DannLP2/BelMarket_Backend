package com.productos.mari.domain.infrastructure.media;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploadValidationService {
    void validateImage(MultipartFile file);
    void validatePdf(MultipartFile file);
    void validateFile(MultipartFile file, long maxSizeInBytes, String label);
    int getMaxImageSizeMb();
    int getMaxPdfSizeMb();
}
