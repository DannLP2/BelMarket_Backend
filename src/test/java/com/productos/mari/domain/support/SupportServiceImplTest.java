package com.productos.mari.domain.support;

import com.productos.mari.domain.infrastructure.communication.EmailService;
import com.productos.mari.domain.infrastructure.media.CloudinaryService;
import com.productos.mari.domain.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupportServiceImplTest {

    @Mock private SupportRequestRepository supportRequestRepository;
    @Mock private CloudinaryService cloudinaryService;
    @Mock private EmailService emailService;
    @Mock private NotificationService notificationService;
    @Mock private SupportMapper supportMapper;

    @InjectMocks
    private SupportServiceImpl supportService;

    private SupportRequest mockRequest;

    @BeforeEach
    void setUp() {
        mockRequest = SupportRequest.builder()
                .id(1L)
                .name("John Doe")
                .email("john@test.com")
                .requestType("ORDER_ISSUE")
                .message("Issue with my order")
                .status("PENDING")
                .build();
    }

    @Test
    void processSupportRequest_SuccessWithAttachment() throws IOException {
        SupportRequestDto dto = new SupportRequestDto();
        dto.setName("John Doe");
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "data".getBytes());

        when(cloudinaryService.uploadFile(any(), eq("belmarket/support"))).thenReturn("http://cloud.com/test.jpg");
        when(supportRequestRepository.save(any())).thenReturn(mockRequest);
        when(supportMapper.toDto(any())).thenReturn(new SupportRequestDto());

        SupportRequestDto result = supportService.processSupportRequest(dto, file);

        assertNotNull(result);
        verify(emailService).sendSupportNotification(any());
        verify(emailService).sendSupportConfirmation(any());
        verify(notificationService).broadcastNotification(any(), any(), any(), any(), any(), anyBoolean());
        verify(notificationService).broadcastSupportUpdate(contains("NEW_TICKET"));
    }

    @Test
    void getAllRequests_ReturnsList() {
        when(supportRequestRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(mockRequest));
        when(supportMapper.toDto(any())).thenReturn(new SupportRequestDto());

        List<SupportRequestDto> result = supportService.getAllRequests();

        assertEquals(1, result.size());
    }

    @Test
    void updateStatus_Success() {
        when(supportRequestRepository.findById(1L)).thenReturn(Optional.of(mockRequest));
        when(supportRequestRepository.save(any())).thenReturn(mockRequest);
        when(supportMapper.toDto(any())).thenReturn(new SupportRequestDto());

        SupportRequestDto result = supportService.updateStatus(1L, "RESOLVED");

        assertEquals("RESOLVED", mockRequest.getStatus());
        verify(notificationService).broadcastSupportUpdate(contains("STATUS_CHANGED"));
    }

    @Test
    void deleteRequest_Success() {
        supportService.deleteRequest(1L);

        verify(supportRequestRepository).deleteById(1L);
        verify(notificationService).broadcastSupportUpdate(contains("TICKET_DELETED"));
    }
}
