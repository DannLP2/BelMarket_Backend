package com.productos.mari.domain.product;

import com.productos.mari.domain.brand.Brand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
class ProductRepositoryIT {

    @Autowired private ProductRepository productRepository;
    @Autowired private EntityManager em;

    private Product activeProduct;
    private Product inactiveProduct;

    @BeforeEach
    void setUp() {
        Brand brand = Brand.builder().name("BelBrand").build();
        em.persist(brand);
        em.flush();

        activeProduct = productRepository.save(Product.builder()
            .name("Galletas Activas")
            .price(BigDecimal.valueOf(5000))
            .stock(20)
            .brand(brand)
            .build());

        inactiveProduct = productRepository.save(Product.builder()
            .name("Producto Inactivo")
            .price(BigDecimal.valueOf(3000))
            .stock(5)
            .isActive(false)
            .brand(brand)
            .build());
    }

    @Test
    void findBySlug_shouldReturnCorrectProduct() {
        Optional<Product> result = productRepository.findBySlug(activeProduct.getSlug());

        assertTrue(result.isPresent());
        assertEquals("Galletas Activas", result.get().getName());
    }

    @Test
    void findBySlug_shouldReturnEmptyForUnknownSlug() {
        Optional<Product> result = productRepository.findBySlug("nonexistent-slug");
        assertFalse(result.isPresent());
    }

    @Test
    void findByIsActiveTrue_shouldOnlyReturnActiveProducts() {
        Page<Product> result = productRepository.findByIsActiveTrue(PageRequest.of(0, 10));

        assertTrue(result.getContent().stream().allMatch(Product::getIsActive));
        assertTrue(result.getContent().stream().noneMatch(p -> p.getName().equals("Producto Inactivo")));
    }

    @Test
    @Transactional
    void decrementStock_shouldReduceStockByGivenQty() {
        int affected = productRepository.decrementStock(activeProduct.getId(), 5);
        em.flush();
        em.clear();

        assertEquals(1, affected);
        Product updated = productRepository.findById(activeProduct.getId()).orElseThrow();
        assertEquals(15, updated.getStock());
    }

    @Test
    @Transactional
    void decrementStock_shouldNotGoNegative_whenStockInsufficient() {
        int affected = productRepository.decrementStock(activeProduct.getId(), 9999);
        assertEquals(0, affected, "Should not decrement insufficient stock");
    }

    @Test
    @Transactional
    void incrementStock_shouldAddToExistingStock() {
        productRepository.incrementStock(activeProduct.getId(), 10);
        em.flush();
        em.clear();

        Product updated = productRepository.findById(activeProduct.getId()).orElseThrow();
        assertEquals(30, updated.getStock());
    }

    @Test
    void countByBrand_shouldReturnCorrectCount() {
        Brand brand = em.createQuery("SELECT b FROM Brand b WHERE b.name = 'BelBrand'", Brand.class)
            .getSingleResult();

        long count = productRepository.countByBrand(brand);
        assertEquals(2, count); // activeProduct + inactiveProduct
    }

    @Test
    void findFiltered_byQuery_shouldReturnMatchingProducts() {
        Page<Product> result = productRepository.findFiltered(
            "Galletas", null, null, null, null, null, null, false, java.time.LocalDateTime.now(), PageRequest.of(0, 10));
        
        assertEquals(1, result.getTotalElements());
        assertEquals("Galletas Activas", result.getContent().get(0).getName());
    }

    @Test
    void findFiltered_byPriceRange_shouldReturnMatchingProducts() {
        // activeProduct: 5000, inactiveProduct: 3000 (but inactive)
        Page<Product> result = productRepository.findFiltered(
            null, null, null, null, new BigDecimal("4000"), new BigDecimal("6000"), null, false, java.time.LocalDateTime.now(), PageRequest.of(0, 10));
        
        assertEquals(1, result.getTotalElements());
        assertEquals("Galletas Activas", result.getContent().get(0).getName());
    }

    @Test
    void findFiltered_byBrand_shouldReturnMatchingProducts() {
        Page<Product> result = productRepository.findFiltered(
            null, null, "BelBrand", null, null, null, null, false, java.time.LocalDateTime.now(), PageRequest.of(0, 10));
        
        assertEquals(1, result.getTotalElements()); // Only the active one
    }

    @Test
    void findFiltered_withNoMatches_shouldReturnEmptyPage() {
        Page<Product> result = productRepository.findFiltered(
            "NonExistent", null, null, null, null, null, null, false, java.time.LocalDateTime.now(), PageRequest.of(0, 10));
        
        assertEquals(0, result.getTotalElements());
    }
}
