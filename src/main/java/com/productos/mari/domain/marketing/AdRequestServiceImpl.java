package com.productos.mari.domain.marketing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdRequestServiceImpl implements AdRequestService {

    private final AdRequestRepository adRequestRepository;
    private final com.productos.mari.domain.notification.NotificationService notificationService;
    private final com.productos.mari.domain.infrastructure.audit.SecurityAuditService securityAuditService;

    @Override
    @Transactional
    public AdRequest createRequest(AdRequest request) {
        request.setStatus(AdRequestStatus.PENDING);
        AdRequest saved = adRequestRepository.save(request);

        // Notify admins about the new ad request
        notificationService.broadcastNotification(
                "Nueva Solicitud de Pauta",
                "Se ha recibido una nueva solicitud de: " + request.getCompanyName(),
                "campaign",
                "/admin/ad-requests",
                com.productos.mari.domain.notification.NotificationCategory.INFO,
                true
        );

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdRequest> getAllRequests(String search, String status) {
        boolean hasStatus = status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL");
        AdRequestStatus statusEnum = null;
        if (hasStatus) {
            try {
                statusEnum = AdRequestStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                hasStatus = false;
            }
        }
        String finalSearch = (search != null && !search.trim().isEmpty()) ? search : null;
        return adRequestRepository.searchRequests(finalSearch, hasStatus, statusEnum);
    }

    @Override
    @Transactional
    public AdRequest updateStatus(Long id, AdRequestStatus status) {
        AdRequest request = adRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ad Request not found"));
        
        // Handle approval dates
        if (status == AdRequestStatus.APPROVED && request.getStatus() != AdRequestStatus.APPROVED) {
            request.setStartDate(java.time.LocalDateTime.now());
            int months = request.getDurationMonths() != null ? request.getDurationMonths() : 1;
            request.setEndDate(request.getStartDate().plusMonths(months));
        }
        
        request.setStatus(status);
        AdRequest saved = adRequestRepository.save(request);

        // Audit log
        String currentAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        securityAuditService.log(
            com.productos.mari.domain.auth.SecurityAction.AD_REQUEST_STATUS_CHANGED,
            null,
            currentAdmin,
            "Solicitud de pauta (ID: " + id + ") cambiada a: " + status
        );

        return saved;
    }

    @Override
    @Transactional
    public void deleteRequest(Long id) {
        AdRequest request = adRequestRepository.findById(id).orElse(null);
        if (request != null) {
            // Audit log
            String currentAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
            securityAuditService.log(
                com.productos.mari.domain.auth.SecurityAction.AD_REQUEST_DELETED,
                null,
                currentAdmin,
                "Solicitud de pauta eliminada de: " + request.getCompanyName() + " (ID: " + id + ")"
            );
            adRequestRepository.delete(request);
        }
    }
}
