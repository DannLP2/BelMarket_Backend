package com.productos.mari.domain.reservation;

import com.productos.mari.domain.product.Product;
import com.productos.mari.domain.user.User;
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
class ReservationRepositoryIT {

    @Autowired private ReservationRepository reservationRepository;
    @Autowired private ReservationItemRepository reservationItemRepository;
    @Autowired private EntityManager em;

    private User user;
    private Product product;
    private Reservation completedReservation;

    @BeforeEach
    void setUp() {
        user = User.builder()
            .name("Test User").email("test@reservation.com")
            .password("pass")
            .roles(new java.util.HashSet<>(java.util.Set.of(com.productos.mari.domain.user.Role.CLIENT)))
            .enabled(true)
            .build();
        em.persist(user);

        product = Product.builder()
            .name("Producto Reserva").slug("producto-reserva")
            .price(BigDecimal.valueOf(5000)).stock(100)
            .build();
        em.persist(product);
        em.flush();

        completedReservation = reservationRepository.save(Reservation.builder()
            .user(user)
            .status(ReservationStatus.COMPLETED)
            .deliveryMethod(DeliveryMethod.PICKUP)
            .total(BigDecimal.valueOf(5000))
            .build());

        ReservationItem item = ReservationItem.builder()
            .reservation(completedReservation)
            .product(product)
            .quantity(2)
            .price(BigDecimal.valueOf(5000))
            .purchasePrice(BigDecimal.valueOf(3000))
            .productNameSnapshot("Producto Reserva")
            .build();
        reservationItemRepository.save(item);
        em.flush();
    }

    @Test
    void existsByUserAndStatusAndProductInItems_shouldReturnTrueIfUserPurchased() {
        boolean result = reservationRepository.existsByUserAndStatusAndProductInItems(
            user, ReservationStatus.COMPLETED, product);

        assertTrue(result);
    }

    @Test
    void existsByUserAndStatusAndProductInItems_shouldReturnFalseIfNotBought() {
        Product otherProduct = Product.builder()
            .name("Otro Producto").slug("otro-producto")
            .price(BigDecimal.valueOf(1000)).stock(5)
            .build();
        em.persist(otherProduct);
        em.flush();

        boolean result = reservationRepository.existsByUserAndStatusAndProductInItems(
            user, ReservationStatus.COMPLETED, otherProduct);

        assertFalse(result);
    }

    @Test
    void findByUserOrderByCreatedAtDesc_shouldReturnUserReservations() {
        List<Reservation> result = reservationRepository.findByUserOrderByCreatedAtDesc(user);
        assertFalse(result.isEmpty());
        assertEquals(user.getId(), result.get(0).getUser().getId());
    }

    @Test
    void countByStatus_shouldCountCorrectly() {
        assertEquals(1L, reservationRepository.countByStatus(ReservationStatus.COMPLETED));
        assertEquals(0L, reservationRepository.countByStatus(ReservationStatus.PENDING));
    }

    @Test
    void sumRevenueBetween_shouldSumCompletedReservationTotals() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        Double revenue = reservationRepository.sumRevenueBetween(start, end);

        assertNotNull(revenue);
        assertEquals(5000.0, revenue, 0.01);
    }

    @Test
    void countCompletedBetween_shouldCountCorrectly() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        Long count = reservationRepository.countCompletedBetween(start, end);
        assertEquals(1L, count);
    }
}
