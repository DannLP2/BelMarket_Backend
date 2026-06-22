package com.productos.mari.domain.marketing;

import com.productos.mari.domain.marketing.OfferDto;
import com.productos.mari.domain.notification.NotificationCategory;
import com.productos.mari.domain.marketing.Offer;
import com.productos.mari.domain.product.Product;
import com.productos.mari.domain.marketing.OfferRepository;
import com.productos.mari.domain.product.ProductRepository;
import com.productos.mari.domain.reservation.ReservationItemRepository;
import com.productos.mari.domain.notification.NotificationService;
import com.productos.mari.domain.marketing.OfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OfferServiceImpl implements OfferService {

    private final OfferRepository offerRepository;
    private final ProductRepository productRepository;
    private final ReservationItemRepository reservationItemRepository;
    private final NotificationService notificationService;
    private final com.productos.mari.domain.infrastructure.communication.EmailService emailService;
    private final com.productos.mari.domain.user.UserRepository userRepository;
    private final com.productos.mari.domain.infrastructure.audit.SecurityAuditService securityAuditService;

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public OfferDto createOffer(OfferDto dto) {
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Desactivar cualquier oferta previa del mismo producto
        offerRepository.findAllByProductAndActiveTrue(product)
                .forEach(prev -> {
                    prev.setActive(false);
                    offerRepository.save(prev);
                });

        Offer offer = Offer.builder()
                .product(product)
                .title(dto.getTitle() != null && !dto.getTitle().isEmpty() ? dto.getTitle() : "Oferta Especial")
                .discountType(dto.getDiscountType() != null ? dto.getDiscountType() : Offer.DiscountType.PERCENTAGE)
                .discountValue(dto.getDiscountValue() != null ? dto.getDiscountValue() : BigDecimal.ZERO)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .minQuantity(dto.getMinQuantity() != null ? dto.getMinQuantity() : 1)
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();

        Offer savedOffer = offerRepository.save(offer);

        String discountSuffix = dto.getDiscountType() == Offer.DiscountType.PERCENTAGE ? "%" : "$";
        String offerTitle = "¡Oferta Especial! " + dto.getDiscountValue() + discountSuffix + " de descuento en " + product.getName();
        String offerDesc = "¡Aprovecha nuestra nueva oferta en " + product.getName() + "! Disponible por tiempo limitado.";

        // 1. Notificación in-app (Broadcast a Clientes)
        notificationService.broadcastNotificationToScope(
                offerTitle,
                offerDesc,
                "local_offer",
                "/product/" + product.getSlug(),
                NotificationCategory.PROMO,
                com.productos.mari.domain.notification.NotificationScope.CLIENT_SHARED
        );

        // 2. Notificación por Email (A todos los usuarios)
        userRepository.findAll().forEach(user -> {
            if (user.isEnabled()) {
                emailService.sendNewOfferEmail(user, product, savedOffer);
            }
        });

        String currentAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        securityAuditService.log(
            com.productos.mari.domain.auth.SecurityAction.OFFER_CREATED,
            null,
            currentAdmin,
            "Oferta creada/activada: '" + dto.getTitle() + "' para producto " + product.getName()
        );

        notificationService.broadcastCatalogUpdate("OFFER_CREATED");

        return mapToDto(savedOffer);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void deactivateOffer(Long id) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Offer not found"));
        offer.setActive(false);
        offerRepository.save(offer);

        String currentAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        securityAuditService.log(
            com.productos.mari.domain.auth.SecurityAction.OFFER_DELETED,
            null,
            currentAdmin,
            "Oferta desactivada manualmente: '" + offer.getTitle() + "'"
        );

        notificationService.broadcastCatalogUpdate("OFFER_DEACTIVATED");
    }

    @Override
    public List<OfferDto> getAllOffers() {
        return offerRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public OfferDto getActiveOfferForProduct(Long productId) {
        List<Offer> offers = offerRepository.findCurrentActiveByProductId(productId, java.time.LocalDateTime.now());
        return offers.isEmpty() ? null : mapToDto(offers.get(0));
    }

    @Override
    public List<OfferDto> getActiveOffers() {
        return offerRepository.findAllCurrentActive(java.time.LocalDateTime.now()).stream()
                .map(this::mapToDto)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    public OfferDto mapToDto(Offer offer) {
        if (offer.getProduct() == null) return null;
        BigDecimal original = offer.getProduct().getPrice();
        if (original != null && original.compareTo(BigDecimal.valueOf(1000)) < 0
                && original.compareTo(BigDecimal.ZERO) > 0) {
            original = original.multiply(BigDecimal.valueOf(1000));
        }

        BigDecimal finalPrice = calculateFinalPrice(original, offer);

        LocalDateTime startCalculationDate = offer.getStartDate() != null ? offer.getStartDate() : offer.getCreatedAt();
        Integer unitsSold = 0;
        if (offer.getId() != null && startCalculationDate != null) {
            unitsSold = reservationItemRepository.sumQuantitySold(
                    offer.getProduct().getId(),
                    startCalculationDate,
                    offer.getEndDate());
        }

        return OfferDto.builder()
                .id(offer.getId())
                .productId(offer.getProduct().getId())
                .productName(offer.getProduct().getName())
                .productImageUrl(offer.getProduct().getMainImageUrl())
                .title(offer.getTitle())
                .discountType(offer.getDiscountType())
                .discountValue(offer.getDiscountValue())
                .startDate(offer.getStartDate())
                .endDate(offer.getEndDate())
                .minQuantity(offer.getMinQuantity())
                .active(offer.getActive())
                .originalPrice(original)
                .finalPrice(finalPrice)
                .createdAt(offer.getCreatedAt())
                .unitsSold(unitsSold)
                .build();
    }

    public static BigDecimal calculateFinalPrice(BigDecimal price, Offer offer) {
        if (price == null || offer == null) return price;
        if (offer.getDiscountType() == Offer.DiscountType.PERCENTAGE) {
            BigDecimal discount = price.multiply(offer.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            return price.subtract(discount);
        } else {
            // FIXED — the discountValue is in full units (e.g. 5000 COP)
            return price.subtract(offer.getDiscountValue()).max(BigDecimal.ZERO);
        }
    }

    @Override
    public java.math.BigDecimal calculateFinalPriceForProduct(Product product, Integer quantity) {
        if (product == null) return java.math.BigDecimal.ZERO;
        
        // Normalize base price to match catalog/frontend logic
        java.math.BigDecimal basePrice = product.getPrice();
        if (basePrice != null && basePrice.compareTo(java.math.BigDecimal.valueOf(1000)) < 0 
                && basePrice.compareTo(java.math.BigDecimal.ZERO) > 0) {
            basePrice = basePrice.multiply(java.math.BigDecimal.valueOf(1000));
        }
        
        List<Offer> activeOffers = offerRepository.findCurrentActiveByProduct(product, LocalDateTime.now());
        Offer activeOffer = activeOffers.isEmpty() ? null : activeOffers.get(0);
        
        int qty = (quantity != null) ? quantity : 1;
        
        if (activeOffer != null && qty >= activeOffer.getMinQuantity()) {
            return calculateFinalPrice(basePrice, activeOffer);
        }
        
        return basePrice;
    }

    /**
     * Tarea Programada: Desactiva ofertas expiradas automáticamente cada hora.
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 3600000)
    @Transactional
    public void autoDeactivateExpiredOffers() {
        LocalDateTime now = LocalDateTime.now();
        List<Offer> expired = offerRepository.findAll().stream()
                .filter(o -> o.getActive() && o.getEndDate() != null && o.getEndDate().isBefore(now))
                .collect(Collectors.toList());

        for (Offer offer : expired) {
            offer.setActive(false);
            offerRepository.save(offer);
            
            // Notificar a los Admins sobre el fin de la promoción
            notifyAdmins(
                "OFERTA FINALIZADA",
                "La promoción '" + offer.getTitle() + "' del producto " + offer.getProduct().getName() + " ha concluido automáticamente.",
                "event_busy",
                "/admin/offers",
                NotificationCategory.INFO
            );
        }
        
        if (!expired.isEmpty()) {
            notificationService.broadcastCatalogUpdate("OFFER_EXPIRED");
        }
    }

    private void notifyAdmins(String title, String description, String icon, String link, NotificationCategory category) {
        notificationService.broadcastNotificationToScope(title, description, icon, link, category, com.productos.mari.domain.notification.NotificationScope.ADMIN_SHARED);
    }
}
