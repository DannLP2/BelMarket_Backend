package com.productos.mari.domain.reservation;

import com.productos.mari.domain.infrastructure.audit.SecurityAuditService;
import com.productos.mari.domain.infrastructure.media.CloudinaryService;
import com.productos.mari.domain.infrastructure.reporting.PDFService;
import com.productos.mari.domain.product.Product;
import com.productos.mari.domain.product.ProductRepository;
import com.productos.mari.domain.product.ProductService;
import com.productos.mari.domain.reservation.util.ReservationNotificationDispatcher;
import com.productos.mari.domain.reservation.util.ReservationPriceCalculator;
import com.productos.mari.domain.reservation.util.ReservationValidator;
import com.productos.mari.domain.settings.AppSettingsService;
import com.productos.mari.domain.infrastructure.location.IpLocationService;
import com.productos.mari.domain.user.User;
import com.productos.mari.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PDFService pdfService;
    private final ProductService productService;
    private final ReservationItemRepository reservationItemRepository;
    private final AppSettingsService appSettingsService;
    private final CloudinaryService cloudinaryService;
    private final SecurityAuditService securityAuditService;
    private final ReservationMapper reservationMapper;

    // Decoupled Components
    private final ReservationValidator validator;
    private final ReservationPriceCalculator priceCalculator;
    private final ReservationNotificationDispatcher notificationDispatcher;
    private final IpLocationService ipLocationService;

    @Override
    @Transactional
    public ReservationDto createReservation(ReservationDto dto, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Reservation reservation = reservationMapper.toEntity(dto);
        reservation.setUser(user);

        if (dto.getDeliveryMethod() == null) {
            reservation.setDeliveryMethod((dto.getShippingAddress() != null && !dto.getShippingAddress().trim().isEmpty())
                    ? DeliveryMethod.DELIVERY
                    : DeliveryMethod.PICKUP);
        }

        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setCreatedAt(LocalDateTime.now());
        reservation.setDisplayCurrency(dto.getDisplayCurrency() != null ? dto.getDisplayCurrency() : "COP");
        
        if (reservation.getDeliveryMethod() == DeliveryMethod.PICKUP) {
            reservation.setPaymentMethod("EFECTIVO");
            reservation.setPaymentSubMethod("Pago al recoger");
        }

        List<ReservationItem> items = dto.getItems().stream().map(itemDto -> {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + itemDto.getProductId()));

            if (itemDto.getQuantity() == null || itemDto.getQuantity() <= 0) {
                throw new IllegalArgumentException("CANTIDAD_INVALIDA:" + product.getId());
            }

            if (productService.decrementStock(product.getId(), itemDto.getQuantity()) <= 0) {
                throw new IllegalArgumentException("STOCK_INSUFICIENTE:" + product.getId() + ":" + product.getName());
            }

            ReservationItem item = new ReservationItem();
            item.setProduct(product);
            item.setQuantity(itemDto.getQuantity());
            item.setReservation(reservation);

            validator.validatePrice(product, itemDto, email);

            item.setPrice(priceCalculator.normalizePrice(itemDto.getPrice()));
            item.setOriginalPrice(priceCalculator.normalizePrice(product.getPrice()));
            item.setPurchasePrice(product.getPurchasePrice());
            item.setProductNameSnapshot(product.getName());
            item.setProductImageSnapshot(product.getMainImageUrl());

            return item;
        }).collect(Collectors.toList());

        reservation.setItems(items);
        BigDecimal subtotal = items.stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        priceCalculator.calculateAndSetShippingAndTax(reservation, subtotal, appSettingsService.getSettings(), "Create");
        reservation.setTotal(subtotal.add(reservation.getShippingCost()).add(reservation.getTaxAmount()));

        Reservation savedReservation = reservationRepository.save(reservation);
        savedReservation.setReference(generateReference(savedReservation.getId()));
        savedReservation = reservationRepository.save(savedReservation);

        ReservationDto finalDto = mapToDto(savedReservation);
        byte[] pdfReceipt = pdfService.generateReservationReceipt(finalDto);
        notificationDispatcher.dispatchOrderCreated(savedReservation, finalDto, user, pdfReceipt);

        return finalDto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationDto> getMyReservations(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        return reservationRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationDto> getAllReservations() {
        return reservationRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getReservationPdf(Long id, String email, String targetCurrency) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));
        validator.validateOwnership(reservation, email);
        
        ReservationDto dto = mapToDto(reservation);
        
        // Apply conversion if requested
        String finalCurrency = targetCurrency != null ? targetCurrency : reservation.getDisplayCurrency();
        if (finalCurrency == null) finalCurrency = "COP";
        
        if (!"COP".equals(finalCurrency)) {
            applyCurrencyConversion(dto, finalCurrency);
        } else {
            dto.setDisplayCurrency("COP");
            dto.setExchangeRate(BigDecimal.ONE);
        }
        
        return pdfService.generateReservationReceipt(dto);
    }

    private void applyCurrencyConversion(ReservationDto dto, String targetCurrency) {
        try {
            Map<String, Object> rateResponse = ipLocationService.getExchangeRates("COP");
            if (rateResponse != null && rateResponse.containsKey("rates")) {
                Map<String, Object> rates = (Map<String, Object>) rateResponse.get("rates");
                Object rateObj = rates.get(targetCurrency);
                if (rateObj != null) {
                    BigDecimal rate = new BigDecimal(rateObj.toString());
                    dto.setExchangeRate(rate);
                    dto.setDisplayCurrency(targetCurrency);
                    
                    // Convert all money fields
                    dto.setTotal(dto.getTotal().multiply(rate));
                    dto.setShippingCost(dto.getShippingCost().multiply(rate));
                    dto.setTaxAmount(dto.getTaxAmount().multiply(rate));
                    
                    if (dto.getItems() != null) {
                        for (ReservationItemDto item : dto.getItems()) {
                            item.setPrice(item.getPrice().multiply(rate));
                            if (item.getOriginalPrice() != null) {
                                item.setOriginalPrice(item.getOriginalPrice().multiply(rate));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not apply currency conversion to PDF: {}", e.getMessage());
            dto.setDisplayCurrency("COP");
            dto.setExchangeRate(BigDecimal.ONE);
        }
    }

    @Override
    @Transactional
    public ReservationDto updateReservationStatus(Long id, ReservationStatus status) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

        ReservationStatus oldStatus = reservation.getStatus();
        if (oldStatus == status) {
            return mapToDto(reservation);
        }

        handleStockUpdate(reservation, oldStatus, status);
        reservation.setStatus(status);

        if ((status == ReservationStatus.SHIPPED || status == ReservationStatus.READY_FOR_PICKUP) &&
                (reservation.getDeliveryCode() == null || reservation.getDeliveryCode().isEmpty())) {
            String code = String.format("%04d", new java.util.Random().nextInt(10000));
            reservation.setDeliveryCode(code);
            reservation.setShippedAt(LocalDateTime.now());
        }

        notificationDispatcher.dispatchStatusUpdate(reservation, status);

        if (status == ReservationStatus.SHIPPED && reservation.getShippedAt() == null) {
            reservation.setShippedAt(LocalDateTime.now());
        } else if (status == ReservationStatus.COMPLETED && reservation.getCompletedAt() == null) {
            reservation.setCompletedAt(LocalDateTime.now());
        }

        Reservation saved = reservationRepository.save(reservation);
        String operator = "SYSTEM";
        if (org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null) {
            operator = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        }

        securityAuditService.log(com.productos.mari.domain.auth.SecurityAction.RESERVATION_STATUS_CHANGED,
                null, operator,
                "Estado de reserva #" + saved.getReference() + " cambiado a: " + status);

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public ReservationDto updateReservation(Long id, ReservationDto dto, String email) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

        validator.validateOwnership(reservation, email);
        validator.validateStatusForEdit(reservation);

        for (ReservationItem oldItem : reservation.getItems()) {
            productService.incrementStock(oldItem.getProduct().getId(), oldItem.getQuantity());
        }

        reservation.getItems().clear();

        List<ReservationItem> items = dto.getItems().stream().map(itemDto -> {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + itemDto.getProductId()));

            if (itemDto.getQuantity() == null || itemDto.getQuantity() <= 0) {
                throw new IllegalArgumentException("CANTIDAD_INVALIDA:" + product.getId());
            }

            if (productService.decrementStock(product.getId(), itemDto.getQuantity()) <= 0) {
                throw new IllegalArgumentException("STOCK_INSUFICIENTE:" + product.getId() + ":" + product.getName());
            }

            ReservationItem item = new ReservationItem();
            item.setProduct(product);
            item.setQuantity(itemDto.getQuantity());
            item.setReservation(reservation);

            validator.validatePrice(product, itemDto, email);

            item.setPrice(priceCalculator.normalizePrice(itemDto.getPrice()));
            item.setOriginalPrice(priceCalculator.normalizePrice(product.getPrice()));
            item.setPurchasePrice(product.getPurchasePrice());
            item.setProductNameSnapshot(product.getName());
            item.setProductImageSnapshot(product.getMainImageUrl());

            return item;
        }).collect(Collectors.toList());

        reservation.getItems().addAll(items);
        BigDecimal subtotal = items.stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        priceCalculator.calculateAndSetShippingAndTax(reservation, subtotal, appSettingsService.getSettings(), "Update");
        reservation.setTotal(subtotal.add(reservation.getShippingCost()).add(reservation.getTaxAmount()));
        reservation.setDeliveryNotes(dto.getDeliveryNotes());

        Reservation saved = reservationRepository.save(reservation);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public ReservationDto cancelReservation(Long id, String email) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));
        validator.validateOwnership(reservation, email);
        if (reservation.getStatus() == ReservationStatus.CANCELLED) return mapToDto(reservation);

        handleStockUpdate(reservation, reservation.getStatus(), ReservationStatus.CANCELLED);
        reservation.setStatus(ReservationStatus.CANCELLED);
        notificationDispatcher.dispatchStatusUpdate(reservation, ReservationStatus.CANCELLED);
        return mapToDto(reservationRepository.save(reservation));
    }

    @Override
    @Transactional
    public int deleteAllCancelled() {
        List<Reservation> cancelled = reservationRepository.findByStatus(ReservationStatus.CANCELLED);
        int count = cancelled.size();
        reservationRepository.deleteAll(cancelled);
        return count;
    }

    private ReservationDto mapToDto(Reservation reservation) {
        ReservationDto dto = reservationMapper.toDto(reservation);
        dto.setReference(reservation.getReference() != null ? reservation.getReference() : generateReference(reservation.getId()));
        dto.setTotal(priceCalculator.normalizePrice(reservation.getTotal()));
        dto.setShippingCost(priceCalculator.normalizePrice(reservation.getShippingCost()));
        dto.setTaxAmount(priceCalculator.normalizePrice(reservation.getTaxAmount()));
        dto.setDisplayCurrency(reservation.getDisplayCurrency() != null ? reservation.getDisplayCurrency() : "COP");
        dto.setItems(reservation.getItems().stream().map(this::mapItemToDto).collect(Collectors.toList()));
        return dto;
    }

    private ReservationItemDto mapItemToDto(ReservationItem item) {
        return ReservationItemDto.builder()
                .id(item.getId())
                .productId(item.getProduct() != null ? item.getProduct().getId() : 0L)
                .productName(item.getProductNameSnapshot())
                .quantity(item.getQuantity())
                .price(priceCalculator.normalizePrice(item.getPrice()))
                .purchasePrice(priceCalculator.normalizePrice(item.getPurchasePrice()))
                .originalPrice(priceCalculator.normalizePrice(item.getOriginalPrice()))
                .imageUrl(item.getProductImageSnapshot())
                .build();
    }

    private void handleStockUpdate(Reservation reservation, ReservationStatus oldStatus, ReservationStatus newStatus) {
        if (oldStatus == newStatus) return;
        boolean isDeductedOld = isStatusDeducted(oldStatus);
        boolean isDeductedNew = isStatusDeducted(newStatus);

        if (!isDeductedOld && isDeductedNew) {
            for (ReservationItem item : reservation.getItems()) {
                productService.decrementStock(item.getProduct().getId(), item.getQuantity());
            }
        } else if (isDeductedOld && !isDeductedNew) {
            for (ReservationItem item : reservation.getItems()) {
                productService.incrementStock(item.getProduct().getId(), item.getQuantity());
            }
        }
    }

    private boolean isStatusDeducted(ReservationStatus status) {
        return status != ReservationStatus.CANCELLED;
    }

    private String generateReference(Long id) {
        String storeName = appSettingsService.getSettings().getStoreName();
        String prefix = storeName != null && storeName.length() >= 3 
            ? storeName.substring(0, 3).toUpperCase() 
            : "BEL";
        return id == null ? prefix + "-000000" : String.format(prefix + "-%06d", id);
    }

    @Override
    public boolean hasUserPurchasedProduct(String email, Long productId) {
        User user = userRepository.findByEmail(email).orElse(null);
        Product product = productRepository.findById(productId).orElse(null);
        return user != null && product != null && reservationRepository.existsByUserAndStatusAndProductInItems(user, ReservationStatus.COMPLETED, product);
    }

    @Override
    public ReservationItemDto getReservationItem(Long itemId, String email) {
        ReservationItem item = reservationItemRepository.findById(itemId).orElseThrow(() -> new IllegalArgumentException("Item no encontrado"));
        return mapItemToDto(item);
    }

    @Override
    public List<ReservationDto> getAvailableOrders() {
        return reservationRepository.findByStatusIn(List.of(ReservationStatus.READY_FOR_DELIVERY)).stream()
                .filter(r -> r.getDeliverer() == null && r.getDeliveryMethod() == DeliveryMethod.DELIVERY)
                .map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public List<ReservationDto> getAssignedOrders(String delivererEmail) {
        User deliverer = userRepository.findByEmail(delivererEmail).orElseThrow(() -> new IllegalArgumentException("Not found"));
        return reservationRepository.findByDelivererAndStatusIn(deliverer, List.of(ReservationStatus.SHIPPED, ReservationStatus.COMPLETED)).stream()
                .map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReservationDto assignToMe(Long reservationId, String delivererEmail) {
        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow(() -> new IllegalArgumentException("Not found"));
        User deliverer = userRepository.findByEmail(delivererEmail).orElseThrow(() -> new IllegalArgumentException("Not found"));
        reservation.setDeliverer(deliverer);
        reservation.setStatus(ReservationStatus.SHIPPED);
        reservation.setDeliveryCode(String.format("%04d", new java.util.Random().nextInt(10000)));
        reservation.setShippedAt(LocalDateTime.now());
        notificationDispatcher.dispatchStatusUpdate(reservation, ReservationStatus.SHIPPED);
        return mapToDto(reservationRepository.save(reservation));
    }

    @Override
    @Transactional
    public ReservationDto completeReservationWithProof(Long id, String code, org.springframework.web.multipart.MultipartFile image) {
        Reservation reservation = reservationRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
        if (!reservation.getDeliveryCode().equals(code)) throw new IllegalArgumentException("Código incorrecto");
        if (image != null && !image.isEmpty()) {
            try { reservation.setDeliveryImageUrl(cloudinaryService.uploadFile(image, "proofs")); } catch (Exception e) { /* log error */ }
        }
        reservation.setStatus(ReservationStatus.COMPLETED);
        reservation.setCompletedAt(LocalDateTime.now());
        notificationDispatcher.dispatchStatusUpdate(reservation, ReservationStatus.COMPLETED);
        return mapToDto(reservationRepository.save(reservation));
    }
}
