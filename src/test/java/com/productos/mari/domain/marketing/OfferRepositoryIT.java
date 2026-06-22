package com.productos.mari.domain.marketing;

import com.productos.mari.domain.product.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
class OfferRepositoryIT {

    @Autowired private OfferRepository offerRepository;
    @Autowired private EntityManager em;

    private Product activeProduct;

    @BeforeEach
    void setUp() {
        activeProduct = Product.builder()
            .name("Producto Oferta Test")
            .slug("producto-oferta-test")
            .price(BigDecimal.valueOf(10000))
            .stock(20)
            .build();
        em.persist(activeProduct);
        em.flush();
    }

    @Test
    void findCurrentActiveByProduct_shouldReturnActiveOfferInsideDateRange() {
        savedOffer(true, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(10));

        List<Offer> result = offerRepository.findCurrentActiveByProduct(activeProduct, LocalDateTime.now());

        assertEquals(1, result.size());
        assertEquals("10% Off", result.get(0).getTitle());
    }

    @Test
    void findCurrentActiveByProduct_shouldNotReturnExpiredOffer() {
        Offer expired = Offer.builder()
            .product(activeProduct)
            .title("Expired")
            .discountType(Offer.DiscountType.PERCENTAGE)
            .discountValue(BigDecimal.TEN)
            .active(true)
            .startDate(LocalDateTime.now().minusDays(10))
            .endDate(LocalDateTime.now().minusDays(1))
            .build();
        offerRepository.save(expired);

        List<Offer> result = offerRepository.findCurrentActiveByProduct(activeProduct, LocalDateTime.now());
        assertTrue(result.isEmpty());
    }

    @Test
    void findCurrentActiveByProduct_shouldNotReturnInactiveOffer() {
        Offer inactive = Offer.builder()
            .product(activeProduct)
            .title("Inactive")
            .discountType(Offer.DiscountType.FIXED)
            .discountValue(BigDecimal.valueOf(500))
            .active(false)
            .build();
        offerRepository.save(inactive);

        List<Offer> result = offerRepository.findCurrentActiveByProduct(activeProduct, LocalDateTime.now());
        assertTrue(result.isEmpty());
    }

    @Test
    void findCurrentActiveByProductId_shouldReturnByProductId() {
        savedOffer(true, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(10));

        List<Offer> result = offerRepository.findCurrentActiveByProductId(
            activeProduct.getId(), LocalDateTime.now());

        assertEquals(1, result.size());
    }

    @Test
    void findAllCurrentActive_shouldReturnAllActiveOffersGlobally() {
        savedOffer(true, null, null); // No date restriction = always active

        List<Offer> result = offerRepository.findAllCurrentActive(LocalDateTime.now());

        assertFalse(result.isEmpty());
    }

    @Test
    void findAllByProductAndActiveTrue_shouldReturnOnlyActiveEntries() {
        savedOffer(true, null, null);
        Offer inactive = Offer.builder()
            .product(activeProduct).title("Old")
            .discountType(Offer.DiscountType.FIXED)
            .discountValue(BigDecimal.ONE)
            .active(false).build();
        offerRepository.save(inactive);

        List<Offer> result = offerRepository.findAllByProductAndActiveTrue(activeProduct);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getActive());
    }

    private Offer savedOffer(boolean active, LocalDateTime start, LocalDateTime end) {
        return offerRepository.save(Offer.builder()
            .product(activeProduct)
            .title("10% Off")
            .discountType(Offer.DiscountType.PERCENTAGE)
            .discountValue(BigDecimal.TEN)
            .active(active)
            .startDate(start)
            .endDate(end)
            .build());
    }
}
