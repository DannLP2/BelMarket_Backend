package com.productos.mari.domain.product.util;

import com.productos.mari.domain.infrastructure.media.CloudinaryService;
import com.productos.mari.domain.product.Product;
import com.productos.mari.domain.product.ProductManual;
import com.productos.mari.domain.product.ProductManualDto;
import com.productos.mari.domain.reservation.ReservationItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductMediaManagerTest {

    @Mock private CloudinaryService cloudinaryService;
    @Mock private ReservationItemRepository reservationItemRepository;

    @InjectMocks
    private ProductMediaManager productMediaManager;

    private Product mockProduct;
    private MockMultipartFile mockFile;

    @BeforeEach
    void setUp() {
        mockProduct = Product.builder()
                .id(1L)
                .galleryImageUrls(new ArrayList<>(List.of("http://cloudinary.com/old.jpg")))
                .mainImageUrl("http://cloudinary.com/main.jpg")
                .manuals(new ArrayList<>())
                .build();
        
        mockFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes());
    }

    @Test
    void uploadMainImage_ReturnsUrl() throws IOException {
        when(cloudinaryService.uploadFile(any(), anyString())).thenReturn("http://new-url.jpg");
        
        String url = productMediaManager.uploadMainImage(mockFile);
        
        assertEquals("http://new-url.jpg", url);
    }

    @Test
    void uploadManuals_ReturnsProductManuals() throws IOException {
        when(cloudinaryService.uploadFile(any(), anyString())).thenReturn("http://manual.pdf");
        
        List<ProductManual> manuals = productMediaManager.uploadManuals(List.of(mockFile));
        
        assertEquals(1, manuals.size());
        assertEquals("http://manual.pdf", manuals.get(0).getUrl());
    }

    @Test
    void updateGallery_DeletesOldFiles_WhenNoReservations() throws IOException {
        when(cloudinaryService.extractPublicId(anyString())).thenReturn("old_id");
        when(cloudinaryService.uploadFile(any(), anyString())).thenReturn("http://new.jpg");

        List<String> result = productMediaManager.updateGallery(
                mockProduct, List.of(mockFile), new ArrayList<>(), false
        );

        assertEquals(1, result.size());
        verify(cloudinaryService).deleteFile(eq("old_id"), anyString());
    }

    @Test
    void updateGallery_KeepsOldFiles_WhenReservationsExist() throws IOException {
        List<String> result = productMediaManager.updateGallery(
                mockProduct, null, new ArrayList<>(), true
        );

        assertEquals(0, result.size());
        verify(cloudinaryService, never()).deleteFile(anyString(), anyString());
    }

    @Test
    void updateManuals_HandlesKeptManuals() throws IOException {
        ProductManual manual = ProductManual.builder().url("http://kept.pdf").build();
        mockProduct.getManuals().add(manual);
        
        ProductManualDto dto = new ProductManualDto();
        dto.setUrl("http://kept.pdf");

        List<ProductManual> result = productMediaManager.updateManuals(
                mockProduct, null, List.of(dto), false
        );

        assertEquals(1, result.size());
        assertEquals("http://kept.pdf", result.get(0).getUrl());
    }

    @Test
    void handleMainImageUpdate_Success() throws IOException {
        when(cloudinaryService.extractPublicId(anyString())).thenReturn("main_id");
        when(cloudinaryService.uploadFile(any(), anyString())).thenReturn("http://new-main.jpg");

        productMediaManager.handleMainImageUpdate(mockProduct, mockFile, false);

        assertEquals("http://new-main.jpg", mockProduct.getMainImageUrl());
        verify(cloudinaryService).deleteFile(eq("main_id"), anyString());
    }
}
