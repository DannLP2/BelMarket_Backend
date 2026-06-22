package com.productos.mari.domain.marketing;

import lombok.extern.slf4j.Slf4j;

import com.productos.mari.domain.marketing.Banner;
import com.productos.mari.domain.marketing.BannerRepository;
import com.productos.mari.domain.marketing.BannerService;
import com.productos.mari.domain.infrastructure.media.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class BannerServiceImpl implements BannerService {

    private final BannerRepository repository;
    private final BannerMetricRepository metricRepository;
    private final CloudinaryService cloudinaryService;
    private final com.productos.mari.domain.infrastructure.audit.SecurityAuditService securityAuditService;
    private final com.productos.mari.domain.notification.NotificationService notificationService;

    @Override
    public List<BannerDto> getActiveBanners() {
        return repository.findActiveByPlacementAndDate(BannerPlacement.HOME_CAROUSEL, LocalDateTime.now())
                .stream().map(b -> mapToDto(b)).toList();
    }

    @Override
    public List<BannerDto> getActiveBannersByPlacement(BannerPlacement placement) {
        return repository.findActiveByPlacementAndDate(placement, LocalDateTime.now())
                .stream().map(b -> mapToDto(b)).toList();
    }

    @Override
    public List<BannerDto> getAllBanners() {
        return repository.findAllByOrderByBannerOrderAsc()
                .stream().map(b -> mapToDto(b)).toList();
    }

    @Override
    @Transactional
    public BannerDto createBanner(BannerDto dto) {
        Banner banner = mapToEntity(dto);
        if (banner.getButtonText() == null || banner.getButtonText().isEmpty()) {
            banner.setButtonText("VER MÁS");
        }
        
        Banner saved = repository.save(banner);
        String currentAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        securityAuditService.log(
            com.productos.mari.domain.auth.SecurityAction.BANNER_CREATED,
            null,
            currentAdmin,
            "Banner creado: " + saved.getTitle() + " (ID: " + saved.getId() + ")"
        );
        notificationService.broadcastMarketingUpdate("BANNER_CREATED");
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public BannerDto updateBanner(Long id, BannerDto dto) {
        Banner current = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Banner no encontrado"));
        
        // Si la imagen cambia, borrar la anterior
        if (current.getImageUrl() != null && !current.getImageUrl().equals(dto.getImageUrl())) {
            deleteCloudinaryImage(current.getImageUrl());
        }

        current.setImageUrl(dto.getImageUrl());
        current.setTitle(dto.getTitle());
        current.setDescription(dto.getDescription());
        current.setButtonText(dto.getButtonText() != null && !dto.getButtonText().isEmpty() ? dto.getButtonText() : "VER MÁS");
        current.setLinkUrl(dto.getLinkUrl());
        current.setActive(dto.isActive());
        current.setBannerOrder(dto.getBannerOrder());
        current.setPlacement(dto.getPlacement());
        current.setStartDate(dto.getStartDate());
        current.setEndDate(dto.getEndDate());

        Banner saved = repository.save(current);
        String currentAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        securityAuditService.log(
            com.productos.mari.domain.auth.SecurityAction.BANNER_CREATED,
            null,
            currentAdmin,
            "Banner actualizado: " + saved.getTitle() + " (ID: " + saved.getId() + ")"
        );
        notificationService.broadcastMarketingUpdate("BANNER_UPDATED");
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public void deleteBanner(Long id) {
        Banner banner = repository.findById(id).orElse(null);
        if (banner != null) {
            String currentAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
            securityAuditService.log(
                com.productos.mari.domain.auth.SecurityAction.BANNER_DELETED,
                null,
                currentAdmin,
                "Banner eliminado: " + banner.getTitle() + " (ID: " + id + ")"
            );
            deleteCloudinaryImage(banner.getImageUrl());
            repository.delete(banner);
            notificationService.broadcastMarketingUpdate("BANNER_DELETED");
        }
    }

    private void deleteCloudinaryImage(String url) {
        if (url == null || !url.contains("cloudinary.com")) return;
        String publicId = cloudinaryService.extractPublicId(url);
        if (publicId != null) {
            try {
                cloudinaryService.deleteFile(publicId);
            } catch (Exception e) {
                log.error("No se pudo eliminar imagen de banner en Cloudinary: " + e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void toggleBannerStatus(Long id) {
        Banner banner = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Banner no encontrado"));
        banner.setActive(!banner.isActive());
        repository.save(banner);
        notificationService.broadcastMarketingUpdate("BANNER_STATUS_TOGGLED");
    }

    @Override
    @Transactional
    public void recordMetric(Long bannerId, AdMetricType type, String ipAddress) {
        // Incrementos atómicos mediante DB-level lock para prevenir deadlocks
        if (type == AdMetricType.IMPRESSION) {
            repository.incrementViewsCount(bannerId);
        } else if (type == AdMetricType.CLICK) {
            repository.incrementClicksCount(bannerId);
        }

        // Obtener proxy para evitar consultas SELECT innecesarias
        Banner bannerProxy = repository.getReferenceById(bannerId);

        // Record historical metric
        BannerMetric metric = BannerMetric.builder()
                .banner(bannerProxy)
                .type(type)
                .ipAddress(ipAddress)
                .build();
        metricRepository.save(metric);
    }

    private BannerDto mapToDto(Banner entity) {
        return BannerDto.builder()
                .id(entity.getId())
                .placement(entity.getPlacement())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .imageUrl(entity.getImageUrl())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .buttonText(entity.getButtonText())
                .linkUrl(entity.getLinkUrl())
                .active(entity.isActive())
                .bannerOrder(entity.getBannerOrder())
                .viewsCount(entity.getViewsCount())
                .clicksCount(entity.getClicksCount())
                .build();
    }

    private Banner mapToEntity(BannerDto dto) {
        return Banner.builder()
                .id(dto.getId())
                .placement(dto.getPlacement())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .imageUrl(dto.getImageUrl())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .buttonText(dto.getButtonText())
                .linkUrl(dto.getLinkUrl())
                .active(dto.isActive())
                .bannerOrder(dto.getBannerOrder())
                .viewsCount(dto.getViewsCount() != null ? dto.getViewsCount() : 0L)
                .clicksCount(dto.getClicksCount() != null ? dto.getClicksCount() : 0L)
                .build();
    }
}
