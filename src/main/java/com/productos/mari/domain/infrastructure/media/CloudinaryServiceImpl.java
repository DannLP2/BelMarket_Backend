package com.productos.mari.domain.infrastructure.media;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.productos.mari.domain.infrastructure.media.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;
    private final com.productos.mari.domain.infrastructure.media.FileUploadValidationService fileValidationService;

    @Override
    public String uploadFile(MultipartFile file) throws IOException {
        return uploadFile(file, "belmarket/general");
    }

    @Override
    public String uploadFile(MultipartFile file, String folder) throws IOException {
        return uploadFile(file, folder, null, "auto");
    }

    @Override
    public String uploadFile(MultipartFile file, String folder, String format) throws IOException {
        return uploadFile(file, folder, format, "image");
    }

    @Override
    @SuppressWarnings("unchecked")
    public String uploadFile(MultipartFile file, String folder, String format, String resourceType) throws IOException {
        Map<String, Object> options = new HashMap<>();
        options.put("folder", folder);
        
        if (resourceType != null) {
            options.put("resource_type", resourceType);
        }
        
        if (format != null) {
            options.put("format", format);
        }

        // --- UNIFIED GLOBAL VALIDATION ---
        if (file != null && !file.isEmpty()) {
            String contentType = file.getContentType();
            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
            
            if ("application/pdf".equals(contentType) || originalName.endsWith(".pdf")) {
                fileValidationService.validatePdf(file);
            } else {
                // Default to image validation for images or unknown types handled as images
                fileValidationService.validateImage(file);
            }
        }

        // Only apply image-specific optimizations if it's an image or resourceType is auto/image
        boolean isImage = resourceType == null || "image".equalsIgnoreCase(resourceType) || "auto".equalsIgnoreCase(resourceType);

        if (isImage && !"ico".equalsIgnoreCase(format)) {
            options.put("quality", "auto");
            options.put("fetch_format", "auto");
        }
        
        Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), options);
        return (String) uploadResult.get("secure_url");
    }

    @Override
    @SuppressWarnings("unchecked")
    public String uploadBase64(String base64Content, String folder) throws IOException {
        String base64Data = base64Content;
        if (base64Content.contains(",")) {
            base64Data = base64Content.split(",")[1];
        }

        byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64Data);
        Map<String, Object> options = ObjectUtils.asMap(
            "folder", folder,
            "quality", "auto",
            "fetch_format", "auto"
        );

        Map<String, Object> uploadResult = cloudinary.uploader().upload(decodedBytes, options);
        return (String) uploadResult.get("secure_url");
    }

    @Override
    public void deleteFile(String publicId) throws IOException {
        deleteFile(publicId, "image");
    }

    @Override
    public void deleteFile(String publicId, String resourceType) throws IOException {
        cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", resourceType));
    }

    @Override
    @SuppressWarnings("unchecked")
    public java.util.List<com.productos.mari.domain.infrastructure.media.MediaDto> listResources() throws Exception {
        Map<String, Object> result = cloudinary.api().resources(ObjectUtils.asMap("max_results", 500));
        java.util.List<Map<String, Object>> resources = (java.util.List<Map<String, Object>>) result.get("resources");

        return resources.stream().map(res -> com.productos.mari.domain.infrastructure.media.MediaDto.builder()
                .publicId((String) res.get("public_id"))
                .url((String) res.get("secure_url"))
                .format((String) res.get("format"))
                .bytes(((Number) res.get("bytes")).longValue())
                .createdAt((String) res.get("created_at"))
                .build()
        ).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public String extractPublicId(String url) {
        if (url == null || !url.contains("cloudinary.com")) return null;
        try {
            // El publicId en Cloudinary está entre el último '/' (sin la versión /v12345/) y la extensión.
            // Una URL típica: https://res.cloudinary.com/demo/image/upload/v1571218530/sample.jpg
            // O con folders: https://res.cloudinary.com/demo/image/upload/v1571218530/folder/subfolder/sample.jpg
            
            // 1. Quitar la parte inicial hasta /upload/
            String partAfterUpload = url.split("/upload/")[1];
            
            // 2. Quitar la versión (v seguido de números) si existe
            if (partAfterUpload.startsWith("v")) {
                int nextSlash = partAfterUpload.indexOf("/");
                partAfterUpload = partAfterUpload.substring(nextSlash + 1);
            }
            
            // 3. Quitar la extensión (.jpg, .png, .webp)
            int lastDot = partAfterUpload.lastIndexOf(".");
            if (lastDot != -1) {
                return partAfterUpload.substring(0, lastDot);
            }
            return partAfterUpload;
        } catch (Exception e) {
            return null;
        }
    }
}
