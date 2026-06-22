package com.productos.mari.domain.product;

import com.productos.mari.domain.brand.Brand;
import com.productos.mari.domain.brand.BrandRepository;
import com.productos.mari.domain.category.Category;
import com.productos.mari.domain.category.CategoryRepository;
import com.productos.mari.domain.infrastructure.audit.SecurityAuditService;
import com.productos.mari.domain.infrastructure.reporting.PageResponse;
import com.productos.mari.domain.marketing.Offer;
import com.productos.mari.domain.marketing.OfferDto;
import com.productos.mari.domain.marketing.OfferRepository;
import com.productos.mari.domain.marketing.OfferService;
import com.productos.mari.domain.notification.NotificationService;
import com.productos.mari.domain.product.util.ProductMediaManager;
import com.productos.mari.domain.product.util.ProductSlugGenerator;
import com.productos.mari.domain.reservation.ReservationItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductServiceImplTest {

    @Mock private ProductRepository productRepository;
    @Mock private OfferRepository offerRepository;
    @Mock private OfferService offerService;
    @Mock private CategoryRepository categoryRepository;
    @Mock private BrandRepository brandRepository;
    @Mock private ReservationItemRepository reservationItemRepository;
    @Mock private NotificationService notificationService;
    @Mock private SecurityAuditService securityAuditService;
    @Mock private ProductMapper productMapper;
    @Mock private ProductMediaManager productMediaManager;
    @Mock private ProductSlugGenerator productSlugGenerator;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product mockProduct;

    @BeforeEach
    void setUp() {
        mockProduct = Product.builder()
                .id(1L)
                .name("Test Product")
                .slug("test-product")
                .stock(10)
                .isActive(true)
                .build();

        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(auth.getName()).thenReturn("admin@test.com");
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void getAllProducts_ReturnsPageResponse() {
        Page<Product> page = new PageImpl<>(List.of(mockProduct));
        when(productRepository.findByIsActiveTrue(any())).thenReturn(page);
        when(productMapper.toDto(any())).thenReturn(new ProductDto());

        PageResponse<ProductDto> response = productService.getAllProducts(PageRequest.of(0, 10));

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }

    @Test
    void searchFiltered_FiltersCorrectly() {
        Page<Product> page = new PageImpl<>(List.of(mockProduct));
        when(productRepository.findFiltered(any(), any(), any(), any(), any(), any(), any(), anyBoolean(), any(), any()))
                .thenReturn(page);
        when(productMapper.toDto(any())).thenReturn(new ProductDto());

        PageResponse<ProductDto> response = productService.searchFiltered("query", "cat", "brand", false, null, null, null, false, PageRequest.of(0, 10));

        assertNotNull(response);
        verify(productRepository).findFiltered(eq("query"), eq("cat"), eq("brand"), eq(false), any(), any(), any(), eq(false), any(), any());
    }

    @Test
    void createProduct_Success() {
        ProductDto dto = new ProductDto();
        dto.setName("New Product");
        dto.setBrand("New Brand");
        dto.setCategories(List.of("Cat1"));

        Product entity = new Product();
        entity.setName("New Product");

        when(productMapper.toEntity(any())).thenReturn(entity);
        when(productSlugGenerator.generateSlug(anyString())).thenReturn("new-product");
        when(brandRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(brandRepository.save(any())).thenReturn(new Brand());
        when(categoryRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(categoryRepository.save(any())).thenReturn(new Category());
        when(productRepository.save(any())).thenReturn(mockProduct);
        when(productMapper.toDto(any())).thenReturn(new ProductDto());

        ProductDto result = productService.createProduct(dto, null, null);

        assertNotNull(result);
        verify(securityAuditService).log(any(), any(), anyString(), contains("creado"));
        verify(notificationService).broadcastCatalogUpdate("PRODUCT_CREATED");
    }

    @Test
    void getProductById_ReturnsDto() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(productMapper.toDto(any())).thenReturn(new ProductDto());

        ProductDto result = productService.getProductById(1L);
        assertNotNull(result);
    }

    @Test
    void getProductById_ThrowsNotFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(com.productos.mari.exception.ResourceNotFoundException.class, () -> productService.getProductById(1L));
    }

    @Test
    void getRelatedProducts_ReturnsList() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(productRepository.findRelatedProducts(anyLong(), any(), any(), any())).thenReturn(List.of(mockProduct));
        when(productMapper.toDto(any())).thenReturn(new ProductDto());

        List<ProductDto> result = productService.getRelatedProducts(1L, 5);
        assertEquals(1, result.size());
    }

    @Test
    void mapToDto_IncludesActiveOffer() {
        Offer activeOffer = new Offer();
        activeOffer.setDiscountValue(new BigDecimal("10.00"));
        activeOffer.setDiscountType(Offer.DiscountType.FIXED);
        
        when(offerRepository.findCurrentActiveByProduct(any(), any())).thenReturn(List.of(activeOffer));
        when(productMapper.toDto(any())).thenReturn(new ProductDto());
        when(offerService.mapToDto(any())).thenReturn(OfferDto.builder().finalPrice(new BigDecimal("90.00")).build());

        // We use reflection or call a method that uses mapToDto internally
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        ProductDto result = productService.getProductById(1L); 

        assertNotNull(result.getActiveOffer());
        assertEquals(new BigDecimal("90.00"), result.getDiscountedPrice());
    }
    @Test
    void updateProduct_Success() {
        ProductDto dto = new ProductDto();
        dto.setName("Updated Product");

        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(productRepository.save(any())).thenReturn(mockProduct);
        when(productMapper.toDto(any())).thenReturn(new ProductDto());

        ProductDto result = productService.updateProduct(1L, dto, null, null, null);

        assertNotNull(result);
        verify(securityAuditService).log(any(), any(), anyString(), contains("actualizado"));
        verify(notificationService).broadcastCatalogUpdate("PRODUCT_UPDATED");
    }

    @Test
    void deleteProduct_CallsSoftDelete() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));

        productService.deleteProduct(1L);

        verify(productRepository).deleteById(1L);
        verify(securityAuditService).log(any(), any(), anyString(), contains("eliminado"));
    }

    @Test
    void incrementStock_LogsAndSaves() {
        when(productRepository.incrementStock(1L, 5)).thenReturn(1);
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));

        int result = productService.incrementStock(1L, 5);

        assertEquals(1, result);
        verify(securityAuditService).log(any(), any(), anyString(), contains("Stock incrementado"));
    }

    @Test
    void decrementStock_AlertsOnLowStock() {
        mockProduct.setStock(3);
        when(productRepository.decrementStock(1L, 1)).thenReturn(1);
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));

        int result = productService.decrementStock(1L, 1);

        assertEquals(1, result);
        verify(notificationService).broadcastNotification(contains("ALERTA DE STOCK BAJO"), anyString(), any(), any(), any(), anyBoolean());
    }

    @Test
    void searchFiltered_HandlesEmptyStringsAsNull() {
        Page<Product> page = new PageImpl<>(List.of(mockProduct));
        when(productRepository.findFiltered(any(), any(), any(), any(), any(), any(), any(), anyBoolean(), any(), any()))
                .thenReturn(page);
        when(productMapper.toDto(any())).thenReturn(new ProductDto());

        // Call with empty strings that should be converted to null
        productService.searchFiltered("  ", " ", "", false, null, null, null, true, PageRequest.of(0, 10));

        verify(productRepository).findFiltered(isNull(), isNull(), isNull(), eq(false), any(), any(), any(), eq(true), any(), any());
    }

    @Test
    void searchProducts_ReturnsPageResponse() {
        Page<Product> page = new PageImpl<>(List.of(mockProduct));
        when(productRepository.findByQuery(anyString(), any())).thenReturn(page);
        when(productMapper.toDto(any())).thenReturn(new ProductDto());

        PageResponse<ProductDto> response = productService.searchProducts("test", PageRequest.of(0, 10));

        assertNotNull(response);
        verify(productRepository).findByQuery(eq("test"), any());
    }

    @Test
    void getProductBySlug_Success() {
        when(productRepository.findBySlug("test-slug")).thenReturn(Optional.of(mockProduct));
        when(productMapper.toDto(any())).thenReturn(ProductDto.builder().slug("test-slug").build());

        ProductDto result = productService.getProductBySlug("test-slug");

        assertNotNull(result);
        assertEquals("test-slug", result.getSlug());
    }

    @Test
    void getProductBySlug_NotFound_ThrowsException() {
        when(productRepository.findBySlug("missing")).thenReturn(Optional.empty());

        assertThrows(com.productos.mari.exception.ResourceNotFoundException.class, 
                () -> productService.getProductBySlug("missing"));
    }

    @Test
    void createProduct_WithEmptyBrandAndCategories_HandlesCorrectly() {
        ProductDto dto = ProductDto.builder()
                .name("Standard Product")
                .brand("  ") // Empty string
                .categories(List.of(" ", "Valid"))
                .build();

        Product entity = new Product();
        when(productMapper.toEntity(any())).thenReturn(entity);
        when(productSlugGenerator.generateSlug(anyString())).thenReturn("standard-product");
        when(categoryRepository.findByNameIgnoreCase("Valid")).thenReturn(Optional.of(new Category()));
        when(productRepository.save(any())).thenReturn(mockProduct);
        when(productMapper.toDto(any())).thenReturn(new ProductDto());

        productService.createProduct(dto, null, null);

        verify(brandRepository, never()).save(any());
        verify(categoryRepository).findByNameIgnoreCase("Valid");
    }

    @Test
    void createProduct_WithDetailLists_SavesCorrectly() {
        ProductDto dto = ProductDto.builder()
                .name("Detailed Product")
                .detailLists(List.of(ProductDetailListDto.builder()
                        .title("Specs")
                        .items(List.of("Spec 1"))
                        .build()))
                .build();

        Product entity = new Product();
        when(productMapper.toEntity(any())).thenReturn(entity);
        when(productRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(productMapper.toDto(any())).thenReturn(new ProductDto());

        ProductDto result = productService.createProduct(dto, null, null);

        assertNotNull(result);
        verify(productRepository).save(argThat(p -> p.getDetailLists() != null && p.getDetailLists().size() == 1));
    }

    @Test
    void updateProduct_WhenProductHasReservations_ChecksFlag() {
        ProductDto dto = new ProductDto();
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(reservationItemRepository.existsByProductId(1L)).thenReturn(true);
        when(productRepository.save(any())).thenReturn(mockProduct);
        when(productMapper.toDto(any())).thenReturn(new ProductDto());

        productService.updateProduct(1L, dto, null, null, null);

        // Verify that hasRev (true) was used in media manager calls
        verify(productMediaManager).handleMainImageUpdate(any(), any(), eq(true));
        verify(productMediaManager).updateGallery(any(), any(), any(), eq(true));
    }

    @Test
    void decrementStock_NoNotification_WhenStockHigh() {
        mockProduct.setStock(10);
        when(productRepository.decrementStock(1L, 1)).thenReturn(1);
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));

        productService.decrementStock(1L, 1);

        verify(notificationService, never()).broadcastNotification(anyString(), anyString(), any(), any(), any(), anyBoolean());
    }

    @Test
    void decrementStock_NoAction_WhenUpdateFails() {
        when(productRepository.decrementStock(1L, 1)).thenReturn(0);

        int result = productService.decrementStock(1L, 1);

        assertEquals(0, result);
        verify(productRepository, never()).findById(anyLong());
    }

    @Test
    void incrementStock_NoLog_WhenUpdateFails() {
        when(productRepository.incrementStock(1L, 1)).thenReturn(0);

        int result = productService.incrementStock(1L, 1);

        assertEquals(0, result);
        verify(securityAuditService, never()).log(any(), any(), anyString(), anyString());
    }

    @Test
    void getRelatedProducts_HandlesMissingCategory() {
        mockProduct.setCategories(null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(productRepository.findRelatedProducts(anyLong(), isNull(), any(), any())).thenReturn(List.of());

        List<ProductDto> result = productService.getRelatedProducts(1L, 5);

        assertNotNull(result);
        verify(productRepository).findRelatedProducts(eq(1L), isNull(), any(), any());
    }
}
