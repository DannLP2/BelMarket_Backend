package com.productos.mari.domain.product;

import com.productos.mari.domain.brand.Brand;
import com.productos.mari.domain.brand.BrandRepository;
import com.productos.mari.domain.reservation.Reservation;
import com.productos.mari.domain.reservation.ReservationItem;
import com.productos.mari.domain.reservation.ReservationStatus;
import com.productos.mari.domain.user.User;
import com.productos.mari.domain.user.UserRepository;
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
class ProductAuditIT {

    @Autowired private ProductRepository productRepository;
    @Autowired private BrandRepository brandRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EntityManager em;

    private Product product;

    @BeforeEach
    void setUp() {
        Brand brand = brandRepository.save(Brand.builder().name("Audit Brand").build());
        
        product = productRepository.save(Product.builder()
                .name("Audit Product")
                .slug("audit-product")
                .brand(brand)
                .purchasePrice(new BigDecimal("50.00"))
                .price(new BigDecimal("100.00"))
                .stock(10)
                .isActive(true)
                .build());

        User user = userRepository.save(User.builder()
                .name("Buyer")
                .email("buyer@test.com")
                .password("pass")
                .build());

        // Create a completed reservation
        Reservation reservation = Reservation.builder()
                .user(user)
                .status(ReservationStatus.COMPLETED)
                .total(new BigDecimal("200.00"))
                .shippingCost(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .deliveryMethod(com.productos.mari.domain.reservation.DeliveryMethod.PICKUP)
                .build();
        em.persist(reservation);

        ReservationItem item = ReservationItem.builder()
                .reservation(reservation)
                .product(product)
                .quantity(2)
                .price(new BigDecimal("100.00"))
                .purchasePrice(new BigDecimal("50.00"))
                .build();
        em.persist(item);

        em.flush();
        em.clear();
    }

    @Test
    void findProductAuditSummary_shouldCalculateCorrectStats() {
        List<Object[]> results = productRepository.findProductAuditSummary();
        
        assertFalse(results.isEmpty());
        Object[] row = results.stream()
                .filter(r -> r[1].equals("Audit Product"))
                .findFirst()
                .orElseThrow();

        // Indices from query:
        // 0: id, 1: name, 2: brandName, 3: mainImageUrl, 4: stock, 5: purchasePrice, 6: price,
        // 7: SUM(quantity), 8: SUM(revenue), 9: SUM(profit), 10: createdAt
        
        assertEquals(10, row[4]); // stock
        assertEquals(new BigDecimal("50.00"), row[5]); // purchase price
        assertEquals(new BigDecimal("100.00"), row[6]); // price
        
        // Stats for COMPLETED orders
        assertEquals(2L, ((Number) row[7]).longValue()); // quantity sold
        assertEquals(0, new BigDecimal("200.00").compareTo((BigDecimal) row[8])); // revenue (2 * 100)
        assertEquals(0, new BigDecimal("100.00").compareTo((BigDecimal) row[9])); // profit (2 * (100 - 50))
    }
}
