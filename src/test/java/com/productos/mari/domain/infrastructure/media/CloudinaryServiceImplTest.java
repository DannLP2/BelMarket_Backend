package com.productos.mari.domain.infrastructure.media;

import com.cloudinary.Api;
import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudinaryServiceImplTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private FileUploadValidationService fileValidationService;

    @Mock
    private Uploader uploader;

    @Mock
    private Api api;

    @InjectMocks
    private CloudinaryServiceImpl cloudinaryService;

    @BeforeEach
    void setUp() {
        lenient().when(cloudinary.uploader()).thenReturn(uploader);
        lenient().when(cloudinary.api()).thenReturn(api);
    }

    @Test
    void uploadFile_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[10]);
        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("secure_url", "https://cloudinary.com/image.jpg");

        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(uploadResult);

        String result = cloudinaryService.uploadFile(file);

        assertEquals("https://cloudinary.com/image.jpg", result);
        verify(fileValidationService).validateImage(file);
    }

    @Test
    void extractPublicId_ParsesCorrectly() {
        // Standard URL
        String url1 = "https://res.cloudinary.com/demo/image/upload/v1571218530/sample.jpg";
        assertEquals("sample", cloudinaryService.extractPublicId(url1));

        // URL with folders
        String url2 = "https://res.cloudinary.com/demo/image/upload/v1571218530/folder/subfolder/img.png";
        assertEquals("folder/subfolder/img", cloudinaryService.extractPublicId(url2));

        // URL without version prefix
        String url3 = "https://res.cloudinary.com/demo/image/upload/products/item1.webp";
        assertEquals("products/item1", cloudinaryService.extractPublicId(url3));
    }

    @Test
    void extractPublicId_ReturnsNullForInvalidUrl() {
        assertNull(cloudinaryService.extractPublicId("https://google.com/image.jpg"));
        assertNull(cloudinaryService.extractPublicId(null));
    }

    @Test
    void deleteFile_CallsCloudinary() throws IOException {
        cloudinaryService.deleteFile("sample_id");
        verify(uploader).destroy(eq("sample_id"), anyMap());
    }

    @Test
    void uploadBase64_Success() throws IOException {
        String base64 = "data:image/jpeg;base64,dGVzdA=="; // "test" in base64
        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("secure_url", "https://cloudinary.com/base64.jpg");

        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(uploadResult);

        String result = cloudinaryService.uploadBase64(base64, "folder");

        assertEquals("https://cloudinary.com/base64.jpg", result);
    }
}
