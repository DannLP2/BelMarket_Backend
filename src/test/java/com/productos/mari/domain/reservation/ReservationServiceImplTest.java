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
import com.productos.mari.domain.user.User;
import com.productos.mari.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservationServiceImplTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private PDFService pdfService;
    @Mock private ProductService productService;
    @Mock private ReservationItemRepository reservationItemRepository;
    @Mock private AppSettingsService appSettingsService;
    @Mock private CloudinaryService cloudinaryService;
    @Mock private SecurityAuditService securityAuditService;
    @Mock private ReservationMapper reservationMapper;
    @Mock private ReservationValidator validator;
    @Mock private ReservationPriceCalculator priceCalculator;
    @Mock private ReservationNotificationDispatcher notificationDispatcher;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    private User mockUser;
    private Product mockProduct;
    private Reservation mockReservation;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@test.com");

        mockProduct = Product.builder()
                .id(1L)
                .name("Test Product")
                .price(new BigDecimal("100.00"))
                .purchasePrice(new BigDecimal("50.00"))
                .stock(10)
                .build();

        mockReservation = new Reservation();
        mockReservation.setId(1L);
        mockReservation.setUser(mockUser);
        mockReservation.setStatus(ReservationStatus.PENDING);
        mockReservation.setReference("BEL-000001");
        mockReservation.setItems(new java.util.ArrayList<>());
        mockReservation.setShippingCost(BigDecimal.ZERO);
        mockReservation.setTaxAmount(BigDecimal.ZERO);
        mockReservation.setTotal(new BigDecimal("100.00"));

        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(auth.getName()).thenReturn("admin@test.com");
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void createReservation_Success() {
        ReservationDto dto = new ReservationDto();
        dto.setItems(List.of(ReservationItemDto.builder().productId(1L).quantity(1).price(new BigDecimal("100.00")).build()));

        Reservation entity = new Reservation();
        entity.setShippingCost(BigDecimal.ZERO);
        entity.setTaxAmount(BigDecimal.ZERO);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(reservationMapper.toEntity(any())).thenReturn(entity);
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(productService.decrementStock(anyLong(), anyInt())).thenReturn(1);
        when(reservationRepository.save(any())).thenReturn(mockReservation);
        when(reservationMapper.toDto(any())).thenReturn(dto);
        when(priceCalculator.normalizePrice(any())).thenReturn(new BigDecimal("100.00"));

        ReservationDto result = reservationService.createReservation(dto, "test@test.com");

        assertNotNull(result);
        verify(notificationDispatcher).dispatchOrderCreated(any(), any(), any(), any());
    }

    @Test
    void createReservation_ThrowsOnStockOut() {
        ReservationDto dto = new ReservationDto();
        dto.setItems(List.of(ReservationItemDto.builder().productId(1L).quantity(1).build()));

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(reservationMapper.toEntity(any())).thenReturn(new Reservation());
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(productService.decrementStock(anyLong(), anyInt())).thenReturn(0);

        assertThrows(IllegalArgumentException.class, () -> reservationService.createReservation(dto, "test@test.com"));
    }

    @Test
    void updateReservationStatus_TriggersStockReplenishmentOnCancel() {
        mockReservation.setStatus(ReservationStatus.PENDING);
        ReservationItem item = new ReservationItem();
        item.setProduct(mockProduct);
        item.setQuantity(2);
        mockReservation.setItems(List.of(item));

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(mockReservation));
        when(reservationRepository.save(any())).thenReturn(mockReservation);
        when(reservationMapper.toDto(any())).thenReturn(new ReservationDto());

        reservationService.updateReservationStatus(1L, ReservationStatus.CANCELLED);

        verify(productService).incrementStock(eq(1L), eq(2));
        verify(notificationDispatcher).dispatchStatusUpdate(any(), eq(ReservationStatus.CANCELLED));
    }

    @Test
    void assignToMe_Success() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(mockReservation));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(reservationRepository.save(any())).thenReturn(mockReservation);
        when(reservationMapper.toDto(any())).thenReturn(new ReservationDto());

        ReservationDto result = reservationService.assignToMe(1L, "deliverer@test.com");

        assertNotNull(result);
        assertEquals(ReservationStatus.SHIPPED, mockReservation.getStatus());
        assertNotNull(mockReservation.getDeliverer());
    }

    @Test
    void completeReservationWithProof_Success() {
        mockReservation.setDeliveryCode("1234");
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(mockReservation));
        when(reservationRepository.save(any())).thenReturn(mockReservation);
        when(reservationMapper.toDto(any())).thenReturn(new ReservationDto());

        ReservationDto result = reservationService.completeReservationWithProof(1L, "1234", null);

        assertNotNull(result);
        assertEquals(ReservationStatus.COMPLETED, mockReservation.getStatus());
    }

    @Test
    void completeReservationWithProof_IncorrectCode_ThrowsException() {
        mockReservation.setDeliveryCode("1234");
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(mockReservation));

        assertThrows(IllegalArgumentException.class, () -> 
            reservationService.completeReservationWithProof(1L, "9999", null));
    }

    @Test
    void createReservation_InvalidQuantity_ThrowsException() {
        ReservationDto dto = new ReservationDto();
        dto.setItems(List.of(ReservationItemDto.builder().productId(1L).quantity(0).build()));

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(reservationMapper.toEntity(any())).thenReturn(new Reservation());
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));

        assertThrows(IllegalArgumentException.class, () -> reservationService.createReservation(dto, "test@test.com"));
    }

    @Test
    void cancelReservation_Idempotency() {
        mockReservation.setStatus(ReservationStatus.CANCELLED);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(mockReservation));
        when(reservationMapper.toDto(any())).thenReturn(new ReservationDto());

        ReservationDto result = reservationService.cancelReservation(1L, "test@test.com");

        assertNotNull(result);
        verify(reservationRepository, never()).save(any());
    }
}
