package com.productos.mari.domain.marketing;
import com.productos.mari.domain.infrastructure.media.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AdvertiserServiceImpl implements AdvertiserService {

    private final AdvertiserRepository repository;
    private final AdMetricRepository adMetricRepository;
    private final CloudinaryService cloudinaryService;
    private final com.productos.mari.domain.notification.NotificationService notificationService;
    private final com.productos.mari.domain.infrastructure.audit.SecurityAuditService securityAuditService;

    @Override
    public List<AdvertiserDto> getAllAdvertisers(String search, String status) {
        boolean hasStatus = status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL");
        String finalSearch = (search != null && !search.trim().isEmpty()) ? search : null;
        return repository.searchAdvertisers(finalSearch, hasStatus, hasStatus ? status.toUpperCase() : null)
                .stream().map(a -> mapToDto(a)).toList();
    }

    @Override
    public List<AdvertiserDto> getActiveAdvertisers() {
        return repository.findByActiveTrueOrderByAdOrderAsc()
                .stream().map(a -> mapToDto(a)).toList();
    }

    @Override
    public List<AdvertiserDto> getActiveAdvertisersByPlacement(AdPlacement placement) {
        return repository.findByActiveTrueAndPlacementOrderByAdOrderAsc(placement)
                .stream().map(a -> mapToDto(a)).toList();
    }

    @Override
    public List<AdvertiserDto> getAllAdvertisersByPlacement(AdPlacement placement) {
        return repository.findAllByPlacementOrderByAdOrderAsc(placement)
                .stream().map(a -> mapToDto(a)).toList();
    }

    @Override
    @Transactional
    public AdvertiserDto createAdvertiser(AdvertiserDto dto) {
        if (repository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Ya existe un anunciante con el nombre: " + dto.getName());
        }

        Advertiser advertiser = mapToEntity(dto);
        if (advertiser.getButtonText() == null || advertiser.getButtonText().trim().isEmpty()) {
            advertiser.setButtonText("VER MÁS");
        }
        Advertiser saved = repository.save(advertiser);
        
        // Audit log
        String currentAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        securityAuditService.log(
            com.productos.mari.domain.auth.SecurityAction.ADVERTISER_CREATED,
            null,
            currentAdmin,
            "Anunciante creado: " + saved.getName() + " (ID: " + saved.getId() + ")"
        );

        notificationService.broadcastMarketingUpdate("ADVERTISER_CREATED");
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public AdvertiserDto updateAdvertiser(Long id, AdvertiserDto dto) {
        Advertiser current = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Anunciante no encontrado"));
        
        if (repository.existsByNameAndIdNot(dto.getName(), id)) {
            throw new IllegalArgumentException("Ya existe otro anunciante con el nombre: " + dto.getName());
        }
        
        // Cleanup old logo if changed
        if (current.getLogoUrl() != null && !current.getLogoUrl().equals(dto.getLogoUrl())) {
            deleteCloudinaryImage(current.getLogoUrl());
        }
        // Cleanup old ad image if changed
        if (current.getAdImageUrl() != null && !current.getAdImageUrl().equals(dto.getAdImageUrl())) {
            deleteCloudinaryImage(current.getAdImageUrl());
        }

        current.setName(dto.getName());
        current.setContactEmail(dto.getContactEmail());
        current.setContactName(dto.getContactName());
        current.setPhone(dto.getPhone());
        current.setLogoUrl(dto.getLogoUrl());
        current.setWebsiteUrl(dto.getWebsiteUrl());
        current.setActive(dto.isActive());
        
        current.setAdImageUrl(dto.getAdImageUrl());
        current.setAdTitle(dto.getAdTitle());
        current.setAdDescription(dto.getAdDescription());
        current.setCompanyDescription(dto.getCompanyDescription());
        current.setRedirectUrl(dto.getRedirectUrl());
        current.setButtonText(dto.getButtonText() != null && !dto.getButtonText().trim().isEmpty() ? dto.getButtonText() : "VER MÁS");
        current.setAdOrder(dto.getAdOrder());
        current.setPlacement(dto.getPlacement());
        current.setDurationMonths(dto.getDurationMonths());
        if (dto.getStartDate() != null) current.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) current.setEndDate(dto.getEndDate());
        
        Advertiser saved = repository.save(current);

        // Audit log
        String currentAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        securityAuditService.log(
            com.productos.mari.domain.auth.SecurityAction.ADVERTISER_UPDATED,
            null,
            currentAdmin,
            "Anunciante actualizado: " + saved.getName() + " (ID: " + saved.getId() + ")"
        );

        notificationService.broadcastMarketingUpdate("ADVERTISER_UPDATED");
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public void deleteAdvertiser(Long id) {
        Advertiser current = repository.findById(id).orElse(null);
        if (current != null) {
            // Audit log
            String currentAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
            securityAuditService.log(
                com.productos.mari.domain.auth.SecurityAction.ADVERTISER_DELETED,
                null,
                currentAdmin,
                "Anunciante eliminado: " + current.getName() + " (ID: " + id + ")"
            );

            deleteCloudinaryImage(current.getLogoUrl());
            deleteCloudinaryImage(current.getAdImageUrl());
            repository.delete(current);
            notificationService.broadcastMarketingUpdate("ADVERTISER_DELETED");
        }
    }

    private void deleteCloudinaryImage(String url) {
        if (url == null || !url.contains("cloudinary.com")) return;
        String publicId = cloudinaryService.extractPublicId(url);
        if (publicId != null) {
            try {
                cloudinaryService.deleteFile(publicId);
            } catch (Exception e) {
                log.error("No se pudo eliminar imagen de anunciante en Cloudinary: " + e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void toggleAdvertiserStatus(Long id) {
        Advertiser current = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Anunciante no encontrado"));
        current.setActive(!current.isActive());
        repository.save(current);

        // Audit log
        String currentAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        securityAuditService.log(
            com.productos.mari.domain.auth.SecurityAction.ADVERTISER_UPDATED,
            null,
            currentAdmin,
            "Estado de anunciante cambiado: " + current.getName() + " -> " + (current.isActive() ? "ACTIVO" : "INACTIVO")
        );

        notificationService.broadcastMarketingUpdate("ADVERTISER_STATUS_TOGGLED");
    }

    @Override
    public String uploadLogo(MultipartFile file) throws IOException {
        return cloudinaryService.uploadFile(file, "belmarket/advertisers");
    }

    @Override
    @Transactional
    public void reorderAds(List<Long> rankedIds) {
        for (int i = 0; i < rankedIds.size(); i++) {
            Long id = rankedIds.get(i);
            Advertiser ad = repository.findById(id).orElse(null);
            if (ad != null) {
                ad.setAdOrder(i);
                repository.save(ad);
            }
        }
        notificationService.broadcastMarketingUpdate("ADVERTISERS_REORDERED");
    }

    @Override
    @Transactional
    public void recordAdMetric(Long advertiserId, AdMetricType type, String ipAddress) {
        // Incrementos atómicos mediante DB-level lock para prevenir deadlocks concurrentes
        if (type == AdMetricType.IMPRESSION) {
            repository.incrementViewsCount(advertiserId);
        } else if (type == AdMetricType.CLICK) {
            repository.incrementClicksCount(advertiserId);
        }

        // Usamos proxy ref en lugar de un SELECT asíncrono evitable
        Advertiser adProxy = repository.getReferenceById(advertiserId);

        // 2. Record dedicated historical metric for auditing/fraud detection
        AdMetric metric = AdMetric.builder()
                .advertiser(adProxy)
                .type(type)
                .ipAddress(ipAddress)
                .build();
        adMetricRepository.save(metric);
    }

    private AdvertiserDto mapToDto(Advertiser entity) {
        return AdvertiserDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .contactEmail(entity.getContactEmail())
                .contactName(entity.getContactName())
                .phone(entity.getPhone())
                .logoUrl(entity.getLogoUrl())
                .websiteUrl(entity.getWebsiteUrl())
                .adImageUrl(entity.getAdImageUrl())
                .adTitle(entity.getAdTitle())
                .adDescription(entity.getAdDescription())
                .companyDescription(entity.getCompanyDescription())
                .buttonText(entity.getButtonText())
                .redirectUrl(entity.getRedirectUrl())
                .adOrder(entity.getAdOrder())
                .viewsCount(entity.getViewsCount())
                .clicksCount(entity.getClicksCount())
                .active(entity.isActive())
                .placement(entity.getPlacement())
                .durationMonths(entity.getDurationMonths())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .build();
    }

    private Advertiser mapToEntity(AdvertiserDto dto) {
        return Advertiser.builder()
                .id(dto.getId())
                .name(dto.getName())
                .contactEmail(dto.getContactEmail())
                .contactName(dto.getContactName())
                .phone(dto.getPhone())
                .logoUrl(dto.getLogoUrl())
                .websiteUrl(dto.getWebsiteUrl())
                .adImageUrl(dto.getAdImageUrl())
                .adTitle(dto.getAdTitle())
                .adDescription(dto.getAdDescription())
                .companyDescription(dto.getCompanyDescription())
                .buttonText(dto.getButtonText())
                .redirectUrl(dto.getRedirectUrl())
                .adOrder(dto.getAdOrder())
                .viewsCount(dto.getViewsCount() != null ? dto.getViewsCount() : 0L)
                .clicksCount(dto.getClicksCount() != null ? dto.getClicksCount() : 0L)
                .active(dto.isActive())
                .placement(dto.getPlacement())
                .durationMonths(dto.getDurationMonths())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .build();
    }
}
