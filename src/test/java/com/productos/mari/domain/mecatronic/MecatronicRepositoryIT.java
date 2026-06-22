package com.productos.mari.domain.mecatronic;

import com.productos.mari.domain.brand.Brand;
import com.productos.mari.domain.brand.BrandRepository;
import com.productos.mari.domain.product.Product;
import com.productos.mari.domain.product.ProductRepository;
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
class MecatronicRepositoryIT {

    @Autowired
    private VariableReadingRepository variableReadingRepository;

    @Autowired
    private MecatronicDeviceRepository deviceRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private EntityManager em;

    private MecatronicDevice device;
    private DeviceVariable tempVar;
    private DeviceVariable humVar;

    @BeforeEach
    void setUp() {
        // Create dependencies
        Brand brand = brandRepository.save(new Brand());
        
        Product product = productRepository.save(Product.builder()
                .name("IoT Device Product")
                .slug("iot-device")
                .price(BigDecimal.TEN)
                .stock(10)
                .isMecatronic(true)
                .brand(brand)
                .build());

        device = deviceRepository.save(MecatronicDevice.builder()
                .product(product)
                .deviceSerial("SN-123456")
                .apiKey("test-api-key")
                .isEnabled(true)
                .build());

        tempVar = DeviceVariable.builder()
                .device(device)
                .fieldKey("temp")
                .label("Temperature")
                .variableType("SENSOR")
                .build();
        
        humVar = DeviceVariable.builder()
                .device(device)
                .fieldKey("hum")
                .label("Humidity")
                .variableType("SENSOR")
                .build();

        em.persist(tempVar);
        em.persist(humVar);

        // Add 15 readings for temp (to test top 10)
        for (int i = 1; i <= 15; i++) {
            variableReadingRepository.save(VariableReading.builder()
                    .variable(tempVar)
                    .value(String.valueOf(20 + i))
                    .timestamp(LocalDateTime.now().minusMinutes(i))
                    .build());
        }

        // Add 5 readings for humidity
        for (int i = 1; i <= 5; i++) {
            variableReadingRepository.save(VariableReading.builder()
                    .variable(humVar)
                    .value(String.valueOf(40 + i))
                    .timestamp(LocalDateTime.now().minusMinutes(i))
                    .build());
        }

        em.flush();
        em.clear();
    }

    @Test
    void findTop10ReadingsPerVariableByDeviceId_shouldReturnCorrectAmount() {
        List<VariableReading> result = variableReadingRepository.findTop10ReadingsPerVariableByDeviceId(device.getId());

        // Temp has 15, should return 10
        // Hum has 5, should return 5
        // Total = 15
        assertEquals(15, result.size());

        long tempCount = result.stream().filter(r -> r.getVariable().getId().equals(tempVar.getId())).count();
        long humCount = result.stream().filter(r -> r.getVariable().getId().equals(humVar.getId())).count();

        assertEquals(10, tempCount);
        assertEquals(5, humCount);
    }

    @Test
    void findTop10ReadingsPerVariableByDeviceId_shouldReturnMostRecentOnes() {
        List<VariableReading> result = variableReadingRepository.findTop10ReadingsPerVariableByDeviceId(device.getId());

        // For temp, the most recent should have value "21" (minus 1 min)
        // Values were 20+1 to 20+15. Recent ones are lower 'i' because minusMinutes(i).
        
        boolean hasOldestTemp = result.stream()
                .filter(r -> r.getVariable().getId().equals(tempVar.getId()))
                .anyMatch(r -> r.getValue().equals("35")); // i = 15
        
        assertFalse(hasOldestTemp, "Should not include the oldest readings (>10)");
    }
}
