package com.productos.mari.domain.marketing;

import com.productos.mari.domain.infrastructure.audit.SecurityAuditService;
import com.productos.mari.domain.notification.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdRequestServiceImplTest {

    @Mock
    private AdRequestRepository adRequestRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SecurityAuditService securityAuditService;

    @InjectMocks
    private AdRequestServiceImpl adRequestService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@test.com", "password")
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createRequest_shouldSetPendingAndBroadcast() {
        AdRequest request = AdRequest.builder().companyName("Test Corp").build();
        when(adRequestRepository.save(request)).thenReturn(request);

        AdRequest result = adRequestService.createRequest(request);

        assertEquals(AdRequestStatus.PENDING, result.getStatus());
        verify(adRequestRepository, times(1)).save(request);
        verify(notificationService, times(1)).broadcastNotification(anyString(), anyString(), anyString(), anyString(), any(), anyBoolean());
    }

    @Test
    void getAllRequests_shouldReturnDescList() {
        AdRequest r1 = AdRequest.builder().companyName("Test Corp").build();
        when(adRequestRepository.searchRequests(any(), anyBoolean(), any())).thenReturn(List.of(r1));

        List<AdRequest> result = adRequestService.getAllRequests(null, null);
        assertEquals(1, result.size());
    }

    @Test
    void updateStatus_shouldCalculateDatesWhenApproved() {
        Long id = 1L;
        AdRequest request = AdRequest.builder()
                .id(id)
                .status(AdRequestStatus.PENDING)
                .durationMonths(3)
                .build();
        when(adRequestRepository.findById(id)).thenReturn(Optional.of(request));
        when(adRequestRepository.save(request)).thenReturn(request);

        AdRequest result = adRequestService.updateStatus(id, AdRequestStatus.APPROVED);

        assertEquals(AdRequestStatus.APPROVED, result.getStatus());
        assertNotNull(result.getStartDate());
        assertNotNull(result.getEndDate());
        // Start date + 3 months
        assertEquals(result.getStartDate().plusMonths(3).toLocalDate(), result.getEndDate().toLocalDate());
        verify(securityAuditService, times(1)).log(any(), any(), anyString(), anyString());
    }

    @Test
    void updateStatus_shouldNotOverrideDatesIfNotApproved() {
        Long id = 1L;
        AdRequest request = AdRequest.builder()
                .id(id)
                .status(AdRequestStatus.PENDING)
                .build();
        when(adRequestRepository.findById(id)).thenReturn(Optional.of(request));
        when(adRequestRepository.save(request)).thenReturn(request);

        AdRequest result = adRequestService.updateStatus(id, AdRequestStatus.REJECTED);

        assertEquals(AdRequestStatus.REJECTED, result.getStatus());
        assertNull(result.getStartDate());
        assertNull(result.getEndDate());
    }

    @Test
    void deleteRequest_shouldAuditAndDelete() {
        Long id = 1L;
        AdRequest request = AdRequest.builder().id(id).companyName("Test Corp").build();
        when(adRequestRepository.findById(id)).thenReturn(Optional.of(request));

        adRequestService.deleteRequest(id);

        verify(securityAuditService, times(1)).log(any(), any(), anyString(), anyString());
        verify(adRequestRepository, times(1)).delete(request);
    }

    @Test
    void updateStatus_shouldNotOverrideDatesIfAlreadyApproved() {
        Long id = 1L;
        // Already APPROVED—dates should not be reset
        AdRequest request = AdRequest.builder()
                .id(id)
                .status(AdRequestStatus.APPROVED)
                .durationMonths(2)
                .build();
        when(adRequestRepository.findById(id)).thenReturn(Optional.of(request));
        when(adRequestRepository.save(request)).thenReturn(request);

        AdRequest result = adRequestService.updateStatus(id, AdRequestStatus.APPROVED);

        // startDate and endDate remain null since it was already APPROVED
        assertNull(result.getStartDate());
        assertEquals(AdRequestStatus.APPROVED, result.getStatus());
    }

    @Test
    void updateStatus_shouldThrowWhenNotFound() {
        when(adRequestRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> adRequestService.updateStatus(99L, AdRequestStatus.REJECTED));
    }

    @Test
    void deleteRequest_shouldDoNothingIfNotFound() {
        when(adRequestRepository.findById(99L)).thenReturn(Optional.empty());

        adRequestService.deleteRequest(99L);

        verify(adRequestRepository, never()).delete(any());
        verify(securityAuditService, never()).log(any(), any(), anyString(), anyString());
    }
}
