package com.productos.mari.domain.marketing;

import com.productos.mari.domain.infrastructure.audit.SecurityAuditService;
import com.productos.mari.domain.infrastructure.communication.EmailService;
import com.productos.mari.domain.notification.NotificationService;
import com.productos.mari.domain.product.Product;
import com.productos.mari.domain.product.ProductRepository;
import com.productos.mari.domain.reservation.ReservationItemRepository;
import com.productos.mari.domain.user.User;
import com.productos.mari.domain.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfferServiceImplTest {

    @Mock
    private OfferRepository offerRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ReservationItemRepository reservationItemRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private EmailService emailService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SecurityAuditService securityAuditService;

    @InjectMocks
    private OfferServiceImpl offerService;

    private Product activeProduct;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@test.com", "password")
        );
        activeProduct = Product.builder()
                .id(1L)
                .name("Galletas Mari")
                .price(BigDecimal.valueOf(5.0)) // Normalized 5000 in method
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createOffer_shouldDeactivateOldAndCreateNew() {
        // Arrange
        OfferDto dto = OfferDto.builder()
                .productId(1L)
                .discountType(Offer.DiscountType.FIXED)
                .discountValue(BigDecimal.valueOf(1000))
                .build();

        Offer oldOffer = Offer.builder().id(2L).active(true).build();
        User activeUser = User.builder().id(9L).enabled(true).build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(activeProduct));
        when(offerRepository.findAllByProductAndActiveTrue(activeProduct)).thenReturn(List.of(oldOffer));
        when(offerRepository.save(any(Offer.class))).thenAnswer(i -> {
            Offer o = i.getArgument(0);
            if(o.getId() == null) o.setId(3L);
            return o;
        });
        when(userRepository.findAll()).thenReturn(List.of(activeUser));

        // Act
        OfferDto result = offerService.createOffer(dto);

        // Assert
        assertNotNull(result);
        assertFalse(oldOffer.getActive()); // Old was deactivated
        verify(offerRepository, atLeast(2)).save(any(Offer.class));
        verify(emailService, times(1)).sendNewOfferEmail(any(), any(), any());
        verify(securityAuditService, times(1)).log(any(), any(), anyString(), anyString());
        verify(notificationService, times(1)).broadcastNotificationToScope(anyString(), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void createOffer_shouldThrowIfProductNotFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());
        OfferDto dto = OfferDto.builder().productId(1L).build();
        assertThrows(RuntimeException.class, () -> offerService.createOffer(dto));
    }

    @Test
    void deactivateOffer_shouldSetActiveFalse() {
        Offer offer = Offer.builder().id(5L).active(true).title("Black Friday").build();
        when(offerRepository.findById(5L)).thenReturn(Optional.of(offer));

        offerService.deactivateOffer(5L);

        assertFalse(offer.getActive());
        verify(offerRepository, times(1)).save(offer);
        verify(securityAuditService, times(1)).log(any(), any(), anyString(), anyString());
    }

    @Test
    void getAllOffers_shouldReturnMappedList() {
        Offer offer = Offer.builder()
            .id(1L)
            .product(activeProduct)
            .discountType(Offer.DiscountType.FIXED)
            .discountValue(BigDecimal.ZERO)
            .build();
        when(offerRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(offer));

        List<OfferDto> result = offerService.getAllOffers();
        assertEquals(1, result.size());
    }

    @Test
    void getActiveOfferForProduct_shouldReturnSingleOffer() {
        Offer offer = Offer.builder().id(1L).product(activeProduct).discountType(Offer.DiscountType.PERCENTAGE).discountValue(BigDecimal.valueOf(10)).build();
        when(offerRepository.findCurrentActiveByProductId(eq(1L), any(LocalDateTime.class))).thenReturn(List.of(offer));

        OfferDto result = offerService.getActiveOfferForProduct(1L);
        assertNotNull(result);
        assertEquals(0, BigDecimal.valueOf(4500).compareTo(result.getFinalPrice())); // 5000 - 10%
    }

    @Test
    void calculateFinalPriceForProduct_shouldReturnDiscountedIfActive() {
        Offer offer = Offer.builder().id(10L).product(activeProduct).discountType(Offer.DiscountType.FIXED).discountValue(BigDecimal.valueOf(500)).minQuantity(2).build();
        when(offerRepository.findCurrentActiveByProduct(eq(activeProduct), any(LocalDateTime.class))).thenReturn(List.of(offer));

        // Met min quantity
        BigDecimal finalPrice = offerService.calculateFinalPriceForProduct(activeProduct, 5);
        assertEquals(0, BigDecimal.valueOf(4500).compareTo(finalPrice)); // 5000 - 500

        // Not met min quantity
        BigDecimal fullPrice = offerService.calculateFinalPriceForProduct(activeProduct, 1);
        assertEquals(0, BigDecimal.valueOf(5000).compareTo(fullPrice)); 
    }

    @Test
    void autoDeactivateExpiredOffers_shouldDeactivateAndNotify() {
        Offer expired = Offer.builder()
                .id(1L)
                .active(true)
                .endDate(LocalDateTime.now().minusDays(1))
                .product(activeProduct)
                .title("Promo")
                .build();
        
        when(offerRepository.findAll()).thenReturn(List.of(expired));

        offerService.autoDeactivateExpiredOffers();

        assertFalse(expired.getActive());
        verify(offerRepository, times(1)).save(expired);
        verify(notificationService, times(1)).broadcastNotificationToScope(anyString(), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void autoDeactivateExpiredOffers_shouldDoNothingWhenNoExpired() {
        Offer activeNotExpired = Offer.builder()
                .id(2L)
                .active(true)
                .endDate(LocalDateTime.now().plusDays(1))
                .product(activeProduct)
                .title("Active Promo")
                .build();
        when(offerRepository.findAll()).thenReturn(List.of(activeNotExpired));

        offerService.autoDeactivateExpiredOffers();

        // No saves, no notifications when nothing expired
        verify(offerRepository, never()).save(any());
        verify(notificationService, never()).broadcastCatalogUpdate(anyString());
    }

    @Test
    void mapToDto_shouldReturnNullWhenProductIsNull() {
        Offer offer = Offer.builder().id(1L).product(null).build();

        OfferDto result = offerService.mapToDto(offer);

        assertNull(result);
    }

    @Test
    void getActiveOfferForProduct_shouldReturnNullWhenEmpty() {
        when(offerRepository.findCurrentActiveByProductId(eq(99L), any(LocalDateTime.class))).thenReturn(List.of());

        OfferDto result = offerService.getActiveOfferForProduct(99L);

        assertNull(result);
    }

    @Test
    void calculateFinalPriceForProduct_shouldReturnBasePriceWhenNoActiveOffer() {
        when(offerRepository.findCurrentActiveByProduct(eq(activeProduct), any(LocalDateTime.class))).thenReturn(List.of());

        BigDecimal result = offerService.calculateFinalPriceForProduct(activeProduct, 1);

        assertEquals(0, BigDecimal.valueOf(5000).compareTo(result)); // normalized 5*1000
    }

    @Test
    void calculateFinalPrice_shouldReturnPriceWhenNullInputs() {
        assertNull(OfferServiceImpl.calculateFinalPrice(null, null));
        assertNull(OfferServiceImpl.calculateFinalPrice(null, Offer.builder().build()));
    }

    @Test
    void createOffer_withPercentageDiscount_shouldBroadcast() {
        OfferDto dto = OfferDto.builder()
                .productId(1L)
                .title("Summer Sale")
                .discountType(Offer.DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(20))
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(activeProduct));
        when(offerRepository.findAllByProductAndActiveTrue(activeProduct)).thenReturn(List.of());
        when(offerRepository.save(any(Offer.class))).thenAnswer(i -> {
            Offer o = i.getArgument(0);
            if (o.getId() == null) o.setId(10L);
            return o;
        });
        when(userRepository.findAll()).thenReturn(List.of());

        OfferDto result = offerService.createOffer(dto);

        assertNotNull(result);
        verify(notificationService).broadcastCatalogUpdate("OFFER_CREATED");
    }

    @Test
    void calculateFinalPriceForProduct_shouldReturnZeroForNullProduct() {
        BigDecimal result = offerService.calculateFinalPriceForProduct(null, 1);
        assertEquals(BigDecimal.ZERO, result);
    }
}
