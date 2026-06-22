package com.productos.mari.domain.settings;

import com.productos.mari.domain.settings.AppSettings;
import com.productos.mari.domain.settings.AppSettingsRepository;
import com.productos.mari.domain.settings.AppSettingsService;
import com.productos.mari.domain.infrastructure.media.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppSettingsServiceImpl implements AppSettingsService {

    private final AppSettingsRepository repository;
    private final CloudinaryService cloudinaryService;
    private final com.productos.mari.domain.infrastructure.media.FileUploadValidationService fileValidationService;
    private final com.productos.mari.domain.infrastructure.audit.SecurityAuditService securityAuditService;

    @Override
    public AppSettings getSettings() {
        AppSettings settings = repository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Configuración no encontrada"));
        
        // Sync limits from application.properties (source of truth)
        settings.setMaxImageSizeMb(fileValidationService.getMaxImageSizeMb());
        settings.setMaxPdfSizeMb(fileValidationService.getMaxPdfSizeMb());
        
        return settings;
    }

    @Override
    @Transactional
    public AppSettings updateSettings(AppSettings settings) {
        AppSettings current = getSettings();
        
        deleteOldImageIfNeeded(current.getLogoUrl(), settings.getLogoUrl());
        current.setLogoUrl(settings.getLogoUrl());
        
        deleteOldImageIfNeeded(current.getFaviconUrl(), settings.getFaviconUrl());
        current.setFaviconUrl(settings.getFaviconUrl());
        
        deleteOldImageIfNeeded(current.getBgLightUrl(), settings.getBgLightUrl());
        current.setBgLightUrl(settings.getBgLightUrl());
        
        deleteOldImageIfNeeded(current.getBgDarkUrl(), settings.getBgDarkUrl());
        current.setBgDarkUrl(settings.getBgDarkUrl());
        
        current.setStoreName(settings.getStoreName());
        current.setContactEmail(settings.getContactEmail());
        current.setContactPhone(settings.getContactPhone());
        current.setWhatsappNumber(settings.getWhatsappNumber());
        current.setInstagramUrl(settings.getInstagramUrl());
        current.setFacebookUrl(settings.getFacebookUrl());
        current.setAddress(settings.getAddress());
        current.setFreeShippingThreshold(settings.getFreeShippingThreshold());
        current.setDefaultShippingCost(settings.getDefaultShippingCost());
        current.setFooterText(settings.getFooterText());
        current.setMetaTitle(settings.getMetaTitle());
        current.setMetaDescription(settings.getMetaDescription());
        current.setMetaKeywords(settings.getMetaKeywords());
        current.setCopyrightText(settings.getCopyrightText());
        current.setTagline(settings.getTagline());
        current.setTaxEnabled(settings.getTaxEnabled());
        current.setTaxRate(settings.getTaxRate());
        
        // Delivery Routing System
        current.setDistanceShippingEnabled(settings.getDistanceShippingEnabled());
        current.setStoreLatitude(settings.getStoreLatitude());
        current.setStoreLongitude(settings.getStoreLongitude());
        current.setBaseDistanceKm(settings.getBaseDistanceKm());
        current.setCostPerKm(settings.getCostPerKm());
        current.setChatbotEnabled(settings.getChatbotEnabled());
        
        current.setStoreCurrency(settings.getStoreCurrency());
        current.setStoreCurrencySymbol(settings.getStoreCurrencySymbol());
        current.setSmartCurrencyEnabled(settings.getSmartCurrencyEnabled());
        
        // Sync regional tax rates map
        current.getRegionalTaxRates().clear();
        if (settings.getRegionalTaxRates() != null) {
            current.getRegionalTaxRates().putAll(settings.getRegionalTaxRates());
        }

        AppSettings saved = repository.save(current);

        String currentAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        securityAuditService.log(
            com.productos.mari.domain.auth.SecurityAction.SETTING_CHANGED,
            null,
            currentAdmin,
            "Configuraciones del sistema ('AppSettings') actualizadas manualmente por el administrador."
        );

        return saved;
    }

    private void deleteOldImageIfNeeded(String oldUrl, String newUrl) {
        if (oldUrl != null && !oldUrl.isEmpty() && !oldUrl.equals(newUrl)) {
            try {
                String publicId = cloudinaryService.extractPublicId(oldUrl);
                if (publicId != null && !publicId.isEmpty()) {
                    cloudinaryService.deleteFile(publicId);
                    log.info("Deleted old settings image from Cloudinary: {}", publicId);
                }
            } catch (Exception e) {
                log.error("Failed to delete replacing image from Cloudinary: {}", oldUrl, e);
            }
        }
    }
}
