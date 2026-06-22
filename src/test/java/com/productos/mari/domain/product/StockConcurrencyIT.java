package com.productos.mari.domain.product;

import com.productos.mari.domain.reservation.ReservationService;
import com.productos.mari.domain.reservation.ReservationDto;
import com.productos.mari.domain.reservation.ReservationItemDto;
import com.productos.mari.domain.reservation.DeliveryMethod;
import com.productos.mari.domain.user.User;
import com.productos.mari.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class StockConcurrencyIT {

    @Autowired private ProductRepository productRepository;
    @Autowired private ReservationService reservationService;
    @Autowired private UserRepository userRepository;

    private Long productId;
    private String userEmail;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        userRepository.deleteAll();

        Product product = productRepository.save(Product.builder()
                .name("Limited Item")
                .slug("limited-item")
                .stock(1) // Only 1 in stock
                .price(new BigDecimal("100.00"))
                .isActive(true)
                .build());
        productId = product.getId();

        User user = userRepository.save(User.builder()
                .name("Buyer")
                .email("buyer@test.com")
                .password("password")
                .enabled(true)
                .build());
        userEmail = user.getEmail();
    }

    @Test
    void simultaneousPurchases_shouldOnlyAllowOneSuccess() throws InterruptedException {
        int numberOfThreads = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        ReservationDto request = ReservationDto.builder()
                .deliveryMethod("PICKUP")
                .items(List.of(ReservationItemDto.builder()
                        .productId(productId)
                        .quantity(1)
                        .build()))
                .build();

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.execute(() -> {
                try {
                    latch.await(); // Wait for all threads to be ready
                    reservationService.createReservation(request, userEmail);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    // One of these should be ObjectOptimisticLockingFailureException 
                    // or IllegalArgumentException if the first one finishes and stock is 0
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        latch.countDown(); // Starts the race!
        doneLatch.await();
        executorService.shutdown();

        // Exact assertions depend on transactional boundaries
        // But total success MUST be 1
        assertEquals(1, successCount.get(), "Only one purchase should succeed when stock is 1");
        assertEquals(numberOfThreads - 1, failureCount.get());

        Product updatedProduct = productRepository.findById(productId).orElseThrow();
        assertEquals(0, updatedProduct.getStock());
    }
}
