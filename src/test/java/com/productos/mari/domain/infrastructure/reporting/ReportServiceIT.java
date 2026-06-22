package com.productos.mari.domain.infrastructure.reporting;

import com.productos.mari.domain.product.Product;
import com.productos.mari.domain.product.ProductRepository;
import com.productos.mari.domain.reservation.DeliveryMethod;
import com.productos.mari.domain.reservation.Reservation;
import com.productos.mari.domain.reservation.ReservationRepository;
import com.productos.mari.domain.reservation.ReservationStatus;
import com.productos.mari.domain.user.User;
import com.productos.mari.domain.user.UserRepository;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReportServiceIT {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void generateInventoryReport_ShouldReturnValidExcel() throws IOException {
        // Given
        productRepository.save(Product.builder()
                .name("Producto Test")
                .price(new BigDecimal("10.00"))
                .stock(50)
                .isActive(true)
                .build());

        // When
        ByteArrayInputStream report = reportService.generateInventoryReport();

        // Then
        assertNotNull(report);
        try (Workbook workbook = new XSSFWorkbook(report)) {
            Sheet sheet = workbook.getSheet("Inventario");
            assertNotNull(sheet);
            assertEquals("ID", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("Producto Test", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals(50.0, sheet.getRow(1).getCell(4).getNumericCellValue());
        }
    }

    @Test
    void generateMonthlySalesReport_ShouldCalculateTotalCorrectily() throws IOException {
        // Given
        User user = userRepository.save(User.builder().name("John Doe").email("john@test.com").password("pass").build());
        
        LocalDateTime now = LocalDateTime.now();
        reservationRepository.save(Reservation.builder()
                .user(user)
                .total(new BigDecimal("100.00"))
                .status(ReservationStatus.COMPLETED)
                .deliveryMethod(DeliveryMethod.DELIVERY)
                .createdAt(now)
                .build());
        
        reservationRepository.save(Reservation.builder()
                .user(user)
                .total(new BigDecimal("50.00"))
                .status(ReservationStatus.COMPLETED)
                .deliveryMethod(DeliveryMethod.DELIVERY)
                .createdAt(now)
                .build());

        // When
        ByteArrayInputStream report = reportService.generateMonthlySalesReport(now.getMonthValue(), now.getYear());

        // Then
        assertNotNull(report);
        try (Workbook workbook = new XSSFWorkbook(report)) {
            Sheet sheet = workbook.getSheet("Ventas Mensuales");
            assertNotNull(sheet);
            
            // Grand total expected 150.0 at row 4, cell 4
            assertEquals(150.0, sheet.getRow(4).getCell(4).getNumericCellValue());
        }
    }
}
