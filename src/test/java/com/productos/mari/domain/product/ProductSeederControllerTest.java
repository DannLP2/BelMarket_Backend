package com.productos.mari.domain.product;

import com.productos.mari.domain.brand.Brand;
import com.productos.mari.domain.brand.BrandRepository;
import com.productos.mari.domain.category.Category;
import com.productos.mari.domain.category.CategoryRepository;
import com.productos.mari.domain.marketing.OfferRepository;
import com.productos.mari.domain.mecatronic.DeviceActionRepository;
import com.productos.mari.domain.mecatronic.DeviceVariableRepository;
import com.productos.mari.domain.mecatronic.MecatronicDeviceRepository;
import com.productos.mari.domain.mecatronic.VariableReadingRepository;
import com.productos.mari.domain.reservation.ReservationItemRepository;
import com.productos.mari.domain.review.ReviewRepository;
import com.productos.mari.domain.user.UserLinkedDeviceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductSeederControllerTest {

    @Mock private ProductRepository productRepository;
    @Mock private BrandRepository brandRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductDetailListRepository detailListRepository;
    @Mock private OfferRepository offerRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private ReservationItemRepository reservationItemRepository;
    @Mock private MecatronicDeviceRepository mecatronicDeviceRepository;
    @Mock private UserLinkedDeviceRepository userLinkedDeviceRepository;
    @Mock private DeviceVariableRepository deviceVariableRepository;
    @Mock private VariableReadingRepository variableReadingRepository;
    @Mock private DeviceActionRepository deviceActionRepository;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private ProductSeederController seederController;

    private Query mockQuery;

    @BeforeEach
    void setUp() {
        mockQuery = mock(Query.class);
    }

    @Test
    void purgeCatalog_FailsWithoutConfirmation() {
        ResponseEntity<String> response = seederController.purgeCatalog("MISTAKE");
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("confirm=CONFIRMAR"));
        
        // Ensure no delete operations happened
        verify(productRepository, never()).hardDeleteAllProducts();
    }

    @Test
    void purgeCatalog_ExecutesFullPurgeWithConfirmation() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.executeUpdate()).thenReturn(1);

        ResponseEntity<String> response = seederController.purgeCatalog("CONFIRMAR");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Catálogo purgado"));

        // Verify IoT cascade deletes
        verify(variableReadingRepository).deleteAllInBatch();
        verify(deviceActionRepository).deleteAllInBatch();
        verify(deviceVariableRepository).deleteAllInBatch();
        verify(userLinkedDeviceRepository).deleteAllInBatch();
        verify(mecatronicDeviceRepository).deleteAllInBatch();

        // Verify product relation deletes
        verify(offerRepository).deleteAllInBatch();
        verify(reviewRepository).deleteAllInBatch();
        verify(reservationItemRepository).deleteAllInBatch();
        verify(detailListRepository).deleteAllInBatch();

        // Verify direct SQL cleanups
        verify(entityManager, times(3)).createNativeQuery(anyString());
        
        // Verify final physical product destruction
        verify(productRepository).hardDeleteAllProducts();
    }

    @Test
    void seedFullCatalog_ExecutesSuccessfully() {
        // Mock getBrand internals to safely return created objects
        when(brandRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(brandRepository.save(any(Brand.class))).then(AdditionalAnswers.returnsFirstArg());

        // Mock getCategory internals to safely return created objects
        when(categoryRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).then(AdditionalAnswers.returnsFirstArg());

        // Mock getting products to safely say they don't exist yet
        when(productRepository.findBySlug(anyString())).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).then(AdditionalAnswers.returnsFirstArg());

        // We use lenient for purgeCatalog which is called by seedFullCatalog internally
        lenient().when(entityManager.createNativeQuery(anyString())).thenReturn(mockQuery);
        lenient().when(mockQuery.executeUpdate()).thenReturn(1);

        ResponseEntity<String> response = seederController.seedFullCatalog();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("6 Productos"));

        // Verified product creations (6 base products saved)
        verify(productRepository, times(6)).save(any(Product.class));
        
        // Verified details lists added
        verify(detailListRepository, atLeastOnce()).save(any(ProductDetailList.class));
    }
}
