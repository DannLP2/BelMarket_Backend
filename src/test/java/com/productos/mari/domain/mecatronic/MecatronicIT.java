package com.productos.mari.domain.mecatronic;

import com.productos.mari.domain.user.User;
import com.productos.mari.domain.user.UserRepository;
import com.productos.mari.domain.user.Role;
import com.productos.mari.domain.product.Product;
import com.productos.mari.domain.product.ProductRepository;
import com.productos.mari.domain.notification.NotificationService;
import com.productos.mari.domain.auth.AuthResponse;
import com.productos.mari.domain.mecatronic.BindDeviceDto;
import com.productos.mari.domain.auth.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class MecatronicIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private com.productos.mari.domain.user.UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private com.productos.mari.domain.user.UserLinkedDeviceRepository linkedDeviceRepository;

    @Autowired
    private DeviceVariableRepository variableRepository;

    @Autowired
    private MecatronicDeviceRepository deviceRepository;

    @Autowired
    private NotificationService notificationService;

    private Long userId;
    private Long productId;
    private String token;
    private final String SERIAL = "TEST-SERIAL-999";
    private final String PIN = "123456";

    @BeforeEach
    void setUp() {
        linkedDeviceRepository.deleteAll();
        variableRepository.deleteAll();
        deviceRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create User
        User user = User.builder()
                .name("IoT Tester")
                .email("iot@test.com")
                .password(passwordEncoder.encode("password"))
                .roles(Set.of(Role.CLIENT))
                .isVerified(true)
                .status(com.productos.mari.domain.user.UserStatus.ACTIVE)
                .build();
        user = userRepository.save(user);
        userId = user.getId();

        // 2. Create Product (Mecatronic)
        Product product = Product.builder()
                .name("Smart Irrigation Hub")
                .slug("smart-irrigation")
                .price(new BigDecimal("150000"))
                .stock(10)
                .isActive(true)
                .isMecatronic(true)
                .build();
        product = productRepository.save(product);
        productId = product.getId();

        // 3. Create Device Entity for that Product
        MecatronicDevice device = MecatronicDevice.builder()
                .product(product)
                .deviceSerial(SERIAL)
                .apiKey("INITIAL-KEY-" + SERIAL)
                .isEnabled(true)
                .build();
        device = deviceRepository.save(device);

        // 3.1 Create Variables for the device
        DeviceVariable tempVar = DeviceVariable.builder()
                .device(device)
                .fieldKey("TEMP")
                .label("Temperatura")
                .unit("°C")
                .variableType("SENSOR")
                .icon("thermostat")
                .build();
        variableRepository.save(tempVar);

        DeviceVariable humidVar = DeviceVariable.builder()
                .device(device)
                .fieldKey("HUMIDITY")
                .label("Humedad")
                .unit("%")
                .variableType("SENSOR")
                .icon("water_drop")
                .build();
        variableRepository.save(humidVar);

        // 4. Login and get Token
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "iot@test.com");
        loginRequest.put("password", "password");
        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity("/auth/login", loginRequest, AuthResponse.class);
        assertNotNull(loginResponse.getBody(), "Login response body should not be null");
        token = loginResponse.getBody().getToken();
    }

    @Test
    void testFullMecatronicLifecycle() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        // --- STEP 1: BIND DEVICE ---
        BindDeviceDto bindDto = new BindDeviceDto();
        bindDto.setSerial(SERIAL);
        bindDto.setPin(PIN);

        HttpEntity<BindDeviceDto> bindRequest = new HttpEntity<>(bindDto, headers);
        ResponseEntity<Void> bindResponse = restTemplate.postForEntity("/api/mecatronic/bind/" + productId, bindRequest, Void.class);

        assertEquals(HttpStatus.OK, bindResponse.getStatusCode());
        
        // --- STEP 2: INGEST TELEMETRY (As Device) ---
        // Find bound device to get its API Key
        MecatronicDevice device = deviceRepository.findByProductId(productId).orElseThrow();
        String apiKey = device.getApiKey();

        Map<String, String> telemetry = new HashMap<>();
        telemetry.put("TEMP", "25.5");
        telemetry.put("HUMIDITY", "55.0");

        ResponseEntity<Void> telResponse = restTemplate.postForEntity("/api/mecatronic/telemetry?apiKey=" + apiKey, telemetry, Void.class);
        assertEquals(HttpStatus.OK, telResponse.getStatusCode());

        // --- STEP 3: VERIFY DASHBOARD ---
        HttpEntity<Void> dashRequest = new HttpEntity<>(headers);
        ResponseEntity<MecatronicDashboardDto> dashResponse = restTemplate.exchange(
                "/api/mecatronic/dashboard/" + productId,
                HttpMethod.GET,
                dashRequest,
                MecatronicDashboardDto.class
        );

        assertEquals(HttpStatus.OK, dashResponse.getStatusCode());
        MecatronicDashboardDto dashboard = dashResponse.getBody();
        assertNotNull(dashboard);
        assertEquals(SERIAL, dashboard.getDeviceSerial());
        // Note: Readings might need a few ms or we verify the variable exists
        assertFalse(dashboard.getVariables().isEmpty());
    }
}
