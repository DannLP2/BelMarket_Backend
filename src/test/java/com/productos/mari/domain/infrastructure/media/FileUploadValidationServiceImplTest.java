package com.productos.mari.domain.infrastructure.media;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class FileUploadValidationServiceImplTest {

    @InjectMocks
    private FileUploadValidationServiceImpl validationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(validationService, "maxImageSize", DataSize.ofMegabytes(2));
        ReflectionTestUtils.setField(validationService, "maxPdfSize", DataSize.ofMegabytes(5));
    }

    @Test
    void validateImage_Success() {
        MultipartFile validImage = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", new byte[100]);

        assertDoesNotThrow(() -> validationService.validateImage(validImage));
    }

    @Test
    void validateImage_RejectsInvalidExtension() {
        MultipartFile invalidImage = new MockMultipartFile(
                "file", "script.exe", "image/jpeg", new byte[100]);

        assertThrows(IllegalArgumentException.class, () -> validationService.validateImage(invalidImage));
    }

    @Test
    void validateImage_RejectsInvalidContentType() {
        MultipartFile invalidImage = new MockMultipartFile(
                "file", "test.jpg", "application/json", new byte[100]);

        assertThrows(IllegalArgumentException.class, () -> validationService.validateImage(invalidImage));
    }

    @Test
    void validateImage_RejectsOversizeFile() {
        // Create an array slightly larger than 2MB
        byte[] largeData = new byte[(2 * 1024 * 1024) + 1];
        MultipartFile largeImage = new MockMultipartFile(
                "file", "large.png", "image/png", largeData);

        assertThrows(IllegalArgumentException.class, () -> validationService.validateImage(largeImage));
    }

    @Test
    void validatePdf_Success() {
        MultipartFile validPdf = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", new byte[100]);

        assertDoesNotThrow(() -> validationService.validatePdf(validPdf));
    }

    @Test
    void validatePdf_RejectsInvalidExtension() {
        MultipartFile invalidPdf = new MockMultipartFile(
                "file", "document.txt", "application/pdf", new byte[100]);

        assertThrows(IllegalArgumentException.class, () -> validationService.validatePdf(invalidPdf));
    }

    @Test
    void validatePdf_RejectsOversizeFile() {
        byte[] largeData = new byte[(5 * 1024 * 1024) + 1];
        MultipartFile largePdf = new MockMultipartFile(
                "file", "large.pdf", "application/pdf", largeData);

        assertThrows(IllegalArgumentException.class, () -> validationService.validatePdf(largePdf));
    }
}
