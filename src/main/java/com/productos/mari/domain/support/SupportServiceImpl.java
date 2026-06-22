package com.productos.mari.domain.support;

import com.productos.mari.domain.support.SupportRequestDto;
import com.productos.mari.domain.notification.NotificationCategory;
import com.productos.mari.domain.support.SupportRequest;
import com.productos.mari.domain.support.SupportRequestRepository;
import com.productos.mari.domain.infrastructure.media.CloudinaryService;
import com.productos.mari.domain.infrastructure.communication.EmailService;
import com.productos.mari.domain.support.SupportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class SupportServiceImpl implements SupportService {

    private final SupportRequestRepository supportRequestRepository;
    private final CloudinaryService cloudinaryService;
    private final EmailService emailService;
    private final com.productos.mari.domain.notification.NotificationService notificationService;
    private final SupportMapper supportMapper;

    @Override
    @Transactional
    public SupportRequestDto processSupportRequest(SupportRequestDto dto, MultipartFile attachment) throws IOException {
        String attachmentUrl = null;
        
        // 1. Upload to Cloudinary if file exists
        if (attachment != null && !attachment.isEmpty()) {
            attachmentUrl = cloudinaryService.uploadFile(attachment, "belmarket/support");
        }

        // 2. Map DTO to Entity and Save
        SupportRequest request = SupportRequest.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .requestType(dto.getRequestType())
                .orderNumber(dto.getOrderNumber())
                .message(dto.getMessage())
                .attachmentUrl(attachmentUrl)
                .status("PENDING") // Explícitamente por si el builder.default no actúa como esperamos
                .build();

        SupportRequest savedRequest = supportRequestRepository.save(request);

        // 3. Send Email Notification to Admin
        emailService.sendSupportNotification(savedRequest);

        // 4. Send Confirmation Email to User
        emailService.sendSupportConfirmation(savedRequest);

        // 5. Notify Admins in Database (Red Badge)
        notifyAdmins(
            "NUEVA SOLICITUD DE SOPORTE",
            "De: " + savedRequest.getName() + " (" + savedRequest.getRequestType() + ")",
            "contact_support",
            "/admin/support",
            NotificationCategory.INFO
        );

        notificationService.broadcastSupportUpdate("NEW_TICKET:" + savedRequest.getId());
        
        return supportMapper.toDto(savedRequest);
    }

    private void notifyAdmins(String title, String description, String icon, String link, NotificationCategory category) {
        notificationService.broadcastNotification(title, description, icon, link, category, true);
    }

    @Override
    public java.util.List<SupportRequestDto> getAllRequests() {
        return supportRequestRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(supportMapper::toDto)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional
    public SupportRequestDto updateStatus(Long id, String status) {
        SupportRequest request = supportRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
        request.setStatus(status);
        SupportRequest saved = supportRequestRepository.save(request);
        notificationService.broadcastSupportUpdate("STATUS_CHANGED:" + id + ":" + status);
        return supportMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteRequest(Long id) {
        supportRequestRepository.deleteById(id);
        notificationService.broadcastSupportUpdate("TICKET_DELETED:" + id);
    }
}
