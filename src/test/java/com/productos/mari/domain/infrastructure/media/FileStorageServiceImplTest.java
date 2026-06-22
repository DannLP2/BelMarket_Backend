package com.productos.mari.domain.infrastructure.media;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceImplTest {

    @TempDir
    Path tempDir;

    private FileStorageServiceImpl fileStorageService;

    @BeforeEach
    void setUp() {
        // Use the JUnit temp directory as the upload directory
        fileStorageService = new FileStorageServiceImpl(tempDir.toString());
    }

    @Test
    void saveFile_StoresFileAndReturnsFileName() throws IOException {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "test-image.jpg", "image/jpeg", "fake image content".getBytes()
        );

        String savedName = fileStorageService.saveFile(mockFile);

        assertNotNull(savedName);
        assertTrue(savedName.endsWith("_test-image.jpg"));
        assertTrue(Files.exists(tempDir.resolve(savedName)));
    }

    @Test
    void saveBase64_WithHeader_DetectsExtensionAndSavesFile() throws IOException {
        // Simulated PNG base64 with data URL header
        String fakeContent = "fake-png-data";
        String base64 = Base64.getEncoder().encodeToString(fakeContent.getBytes());
        String base64WithHeader = "data:image/png;base64," + base64;

        String savedName = fileStorageService.saveBase64(base64WithHeader, "profile");

        assertNotNull(savedName);
        assertTrue(savedName.startsWith("profile_"));
        assertTrue(savedName.endsWith(".png"));
        assertTrue(Files.exists(tempDir.resolve(savedName)));
    }

    @Test
    void saveBase64_WithJpegHeader_DetectsJpegExtension() {
        String base64 = Base64.getEncoder().encodeToString("jpeg-data".getBytes());
        String base64WithHeader = "data:image/jpeg;base64," + base64;

        String savedName = fileStorageService.saveBase64(base64WithHeader, "avatar");

        assertTrue(savedName.endsWith(".jpg"));
    }

    @Test
    void saveBase64_WithoutHeader_DefaultsToJpg() {
        String base64 = Base64.getEncoder().encodeToString("raw-data".getBytes());

        String savedName = fileStorageService.saveBase64(base64, "thumb");

        assertTrue(savedName.endsWith(".jpg"));
    }

    @Test
    void deleteFile_RemovesExistingFile() throws IOException {
        // Create a real temp file to delete
        Path tempFile = Files.createFile(tempDir.resolve("to-delete.jpg"));
        assertTrue(Files.exists(tempFile));

        fileStorageService.deleteFile("to-delete.jpg");

        assertFalse(Files.exists(tempFile));
    }

    @Test
    void deleteFile_NonExistentFile_DoesNotThrow() {
        // Should not throw even if file is missing
        assertDoesNotThrow(() -> fileStorageService.deleteFile("ghost-file.jpg"));
    }
}
