package com.productos.mari.domain.product.util;

import com.productos.mari.domain.infrastructure.media.CloudinaryService;
import com.productos.mari.domain.product.Product;
import com.productos.mari.domain.product.ProductDto;
import com.productos.mari.domain.product.ProductManual;
import com.productos.mari.domain.product.ProductManualDto;
import com.productos.mari.domain.reservation.ReservationItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductMediaManager {

    private final CloudinaryService cloudinaryService;
    private final ReservationItemRepository reservationItemRepository;

    public String uploadMainImage(MultipartFile mainImage) {
        if (mainImage == null || mainImage.isEmpty()) return null;
        try {
            return cloudinaryService.uploadFile(mainImage, "belmarket/products");
        } catch (IOException e) {
            throw new IllegalArgumentException("Error al subir la imagen principal: " + e.getMessage());
        }
    }

    public List<ProductManual> uploadManuals(List<MultipartFile> technicalManuals) {
        List<ProductManual> items = new ArrayList<>();
        if (technicalManuals != null) {
            for (MultipartFile file : technicalManuals) {
                if (file != null && !file.isEmpty()) {
                    try {
                        String url = cloudinaryService.uploadFile(file, "belmarket/products/manuals");
                        items.add(ProductManual.builder()
                                .url(url)
                                .title(file.getOriginalFilename())
                                .build());
                    } catch (IOException e) {
                        throw new IllegalArgumentException("Error al subir el manual técnico: " + e.getMessage());
                    }
                }
            }
        }
        return items;
    }

    public List<String> updateGallery(Product product, List<MultipartFile> newGalleryFiles, List<String> keptUrls, boolean hasReservations) {
        List<String> currentUrls = product.getGalleryImageUrls() != null ? 
                new ArrayList<>(product.getGalleryImageUrls()) : new ArrayList<>();
        List<String> finalUrls = keptUrls != null ? new ArrayList<>(keptUrls) : new ArrayList<>();

        // 1. Cleanup removed images from Cloudinary (if no reservations)
        for (String url : currentUrls) {
            if (!finalUrls.contains(url) && url.contains("cloudinary.com")) {
                if (!hasReservations) {
                    deleteCloudinaryFile(url);
                }
            }
        }

        // 2. Upload new images
        if (newGalleryFiles != null) {
            for (MultipartFile file : newGalleryFiles) {
                if (file != null && !file.isEmpty()) {
                    try {
                        finalUrls.add(cloudinaryService.uploadFile(file, "belmarket/products/gallery"));
                    } catch (IOException e) {
                        throw new IllegalArgumentException("Error al subir imagen de galería: " + e.getMessage());
                    }
                }
            }
        }
        return finalUrls;
    }

    public List<ProductManual> updateManuals(Product product, List<MultipartFile> newManualFiles, List<ProductManualDto> keptManualsDto, boolean hasReservations) {
        List<ProductManual> currentManuals = product.getManuals() != null ?
                new ArrayList<>(product.getManuals()) : new ArrayList<>();
        List<ProductManual> updatedManuals = new ArrayList<>();

        // 1. Identification and biological cleanup
        for (ProductManual manual : currentManuals) {
            boolean stillKept = keptManualsDto != null && keptManualsDto.stream().anyMatch(dto -> dto.getUrl().equals(manual.getUrl()));
            if (!stillKept && manual.getUrl().contains("cloudinary.com")) {
                if (!hasReservations) {
                    deleteCloudinaryFile(manual.getUrl());
                }
            }
            if (stillKept) {
                updatedManuals.add(manual);
            }
        }

        // 2. Upload new ones
        updatedManuals.addAll(uploadManuals(newManualFiles));
        return updatedManuals;
    }

    public void handleMainImageUpdate(Product product, MultipartFile newImage, boolean hasReservations) {
        if (newImage != null && !newImage.isEmpty()) {
            if (!hasReservations && product.getMainImageUrl() != null && product.getMainImageUrl().contains("cloudinary.com")) {
                deleteCloudinaryFile(product.getMainImageUrl());
            }
            product.setMainImageUrl(uploadMainImage(newImage));
        }
    }

    public void deleteCloudinaryFile(String url) {
        if (url == null || !url.contains("cloudinary.com")) return;
        String publicId = cloudinaryService.extractPublicId(url);
        if (publicId != null) {
            try {
                String resourceType = url.contains("/raw/") ? "raw" : "image";
                cloudinaryService.deleteFile(publicId, resourceType);
            } catch (Exception e) {
                log.error("No se pudo eliminar el recurso de Cloudinary: " + e.getMessage());
            }
        }
    }
}
