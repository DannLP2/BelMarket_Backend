package com.productos.mari.domain.infrastructure.media;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaControllerTest {

    @Mock
    private MediaService mediaService;

    @InjectMocks
    private MediaController mediaController;

    @Test
    void getAllMedia_ReturnsListWithOk() throws Exception {
        MediaDto media1 = MediaDto.builder().publicId("img1").inUse(true).build();
        MediaDto media2 = MediaDto.builder().publicId("img2").inUse(false).build();
        when(mediaService.getAllMedia()).thenReturn(List.of(media1, media2));

        ResponseEntity<List<MediaDto>> response = mediaController.getAllMedia();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        verify(mediaService).getAllMedia();
    }

    @Test
    void deleteMedia_ReturnsNoContent() throws Exception {
        String publicId = "folder/my_image";
        doNothing().when(mediaService).deleteMedia(publicId);

        ResponseEntity<Void> response = mediaController.deleteMedia(publicId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(mediaService).deleteMedia(publicId);
    }

    @Test
    void deleteMediaBulk_ReturnsNoContent() throws Exception {
        List<String> ids = List.of("img1", "img2", "img3");
        doNothing().when(mediaService).deleteMediaBulk(ids);

        ResponseEntity<Void> response = mediaController.deleteMediaBulk(ids);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(mediaService).deleteMediaBulk(ids);
    }
}
