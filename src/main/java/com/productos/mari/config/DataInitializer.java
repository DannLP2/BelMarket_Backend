package com.productos.mari.config;

import lombok.extern.slf4j.Slf4j;

import com.productos.mari.domain.settings.AppSettings;
import com.productos.mari.domain.notification.NotificationCategory;
import com.productos.mari.domain.user.Role;
import com.productos.mari.domain.user.User;
import com.productos.mari.domain.settings.AppSettingsRepository;
import com.productos.mari.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppSettingsRepository appSettingsRepository;
    private final com.productos.mari.domain.notification.NotificationService notificationService;

    @Value("${app.admin.email:admin@belmarket.com}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.store.contact-phone:3186283576}")
    private String contactPhone;

    @Value("${app.store.latitude:2.444433}")
    private Double storeLatitude;

    @Value("${app.store.longitude:-76.614031}")
    private Double storeLongitude;

    @Override
    public void run(String... args) {
        // Crear usuario Admin super user si no existe
        java.util.Optional<User> adminOpt = userRepository.findByEmail(adminEmail);
        if (adminOpt.isEmpty()) {
            User admin = User.builder()
                    .name("BelMarket Admin")
                    .email(adminEmail)
                    .phone(contactPhone)
                    .password(passwordEncoder.encode(adminPassword))
                    .roles(new java.util.HashSet<>(java.util.Arrays.asList(Role.ADMIN, Role.CLIENT, Role.DELIVERER, Role.SUPER_ADMIN)))
                    .documentType("CC")
                    .documentNumber("12345678")
                    .gender("Masculino")
                    .isVerified(true)
                    .status(com.productos.mari.domain.user.UserStatus.ACTIVE)
                    .build();
            User savedAdmin = userRepository.save(admin);
            log.info("Usuario SUPER ADMIN creado: " + adminEmail);

            // Generar notificación de bienvenida inicial
            notificationService.createNotification(
                savedAdmin.getId(),
                "¡Bienvenido, Administrador!",
                "El sistema BelMarket ha sido inicializado. Tu cuenta de control total está lista para usarse.",
                "admin_panel_settings",
                "/admin",
                NotificationCategory.SUCCESS,
                true
            );
        } else {
            User existAdmin = adminOpt.get();
            if (existAdmin.getRoles().size() < 4) {
                 java.util.Set<Role> roles = new java.util.HashSet<>(existAdmin.getRoles());
                 roles.add(Role.ADMIN);
                 roles.add(Role.CLIENT);
                 roles.add(Role.DELIVERER);
                 roles.add(Role.SUPER_ADMIN);
                 existAdmin.setRoles(roles);
                 userRepository.save(existAdmin);
                 log.info("Roles SUPER ADMIN parcheados exitosamente.");
            }
        }

        // Initial or upgrade AppSettings
        AppSettings settings = appSettingsRepository.findAll().stream().findFirst()
                .orElse(AppSettings.builder()
                        .storeName("BelMarket")
                        .contactEmail("soporte@belmarket.com")
                        .contactPhone("3186283576")
                        .whatsappNumber("3186283576")
                        .instagramUrl("https://instagram.com/belmarket")
                        .facebookUrl("https://facebook.com/belmarket")
                        .address("Popayán, Cauca, Colombia")
                        .metaTitle("BelMarket | Tu Tienda de Belleza Favorita")
                        .metaDescription("Encuentra los mejores productos de belleza, maquillaje y cuidado personal en BelMarket. Calidad y elegancia en cada envío.")
                        .metaKeywords("maquillaje, skin care, belleza, colombia, cosméticos, belmarket")
                        .freeShippingThreshold(new BigDecimal("100000"))
                        .defaultShippingCost(new BigDecimal("10000"))
                        .footerText("Tu plataforma de compras en línea donde encuentras productos de calidad de forma rápida, segura y confiable.")
                        .tagline("Elegancia & Calidad")
                        .copyrightText("Tu mercado en línea")
                        .storeLatitude(storeLatitude)
                        .storeLongitude(storeLongitude)
                        .baseDistanceKm(5.0)
                        .costPerKm(new BigDecimal("2000"))
                        .distanceShippingEnabled(true)
                        .build());

        // Aggressive Migration/Patching for AppSettings
        boolean updated = false;
        
        // Target coordinates from configuration
        double targetLat = storeLatitude;
        double targetLng = storeLongitude;

        // SEO Patching
        String currentStore = settings.getStoreName() != null ? settings.getStoreName() : "Tu Tienda";
        
        if (settings.getMetaTitle() == null || settings.getMetaTitle().isEmpty() || settings.getMetaTitle().contains("BelMarket")) {
            settings.setMetaTitle(currentStore + " | Tu Tienda Favorita");
            updated = true;
        }
        if (settings.getMetaDescription() == null || settings.getMetaDescription().isEmpty() || settings.getMetaDescription().contains("BelMarket")) {
            settings.setMetaDescription("Encuentra los mejores productos en " + currentStore + ". Calidad y elegancia en cada envío.");
            updated = true;
        }
        if (settings.getMetaKeywords() == null || settings.getMetaKeywords().isEmpty() || settings.getMetaKeywords().contains("belmarket")) {
            settings.setMetaKeywords("compras, tienda, calidad, " + currentStore.toLowerCase());
            updated = true;
        }

        // Patch hardcoded BelMarket in footer text
        if (settings.getFooterText() != null && settings.getFooterText().startsWith("BelMarket es")) {
            settings.setFooterText(currentStore + " es tu plataforma de compras en línea donde encuentras productos de calidad de forma rápida, segura y confiable.");
            updated = true;
        }
        
        if (settings.getCopyrightText() != null && settings.getCopyrightText().contains("BelMarket")) {
            settings.setCopyrightText(currentStore + " | Tu mercado en línea");
            updated = true;
        }

        // Force update if coordinates are null, zero, or near the various old defaults
        if (settings.getStoreLatitude() == null || settings.getStoreLatitude() < 0.1 || 
            (Math.abs(settings.getStoreLatitude() - 2.4419) < 0.01) || 
            (Math.abs(settings.getStoreLatitude() - 2.477438) < 0.01)) {
            
            log.info("DataInitializer: Patching Store Coordinates to " + targetLat + ", " + targetLng);
            settings.setStoreLatitude(targetLat);
            settings.setStoreLongitude(targetLng);
            settings.setAddress("Popayán, Cauca, Colombia"); 
            updated = true;
        }

        if (settings.getStoreName() == null || settings.getStoreName().isEmpty()) { 
            settings.setStoreName("BelMarket"); 
            updated = true; 
        }
        
        if (settings.getDistanceShippingEnabled() == null) { 
            settings.setDistanceShippingEnabled(true); 
            updated = true; 
        }

        if (settings.getBaseDistanceKm() == null) { 
            settings.setBaseDistanceKm(5.0); 
            updated = true; 
        }

        if (settings.getCostPerKm() == null) { 
            settings.setCostPerKm(new BigDecimal("2000")); 
            updated = true; 
        }

        if (settings.getInstagramUrl() == null) { settings.setInstagramUrl("https://instagram.com/belmarket"); updated = true; }
        if (settings.getFacebookUrl() == null) { settings.setFacebookUrl("https://facebook.com/belmarket"); updated = true; }

        if (settings.getCopyrightText() == null || settings.getCopyrightText().isEmpty()) {
            settings.setCopyrightText("BelMarket | Tu mercado en línea");
            updated = true;
        }
        if (settings.getTagline() == null || settings.getTagline().isEmpty()) {
            settings.setTagline("Elegancia & Calidad");
            updated = true;
        }

        // Patch Regional Tax Rates with current standard values
        if (settings.getRegionalTaxRates() == null || settings.getRegionalTaxRates().isEmpty()) {
            log.info("DataInitializer: Initializing regional tax rates (COP: 19%, MXN: 16%)");
            java.util.Map<String, BigDecimal> rates = new java.util.HashMap<>();
            rates.put("COP", new BigDecimal("19.00"));
            rates.put("MXN", new BigDecimal("16.00"));
            rates.put("USD", new BigDecimal("0.00"));
            rates.put("EUR", new BigDecimal("0.00"));
            settings.setRegionalTaxRates(rates);
            updated = true;
        }

        if (settings.getId() == null || updated) {
            appSettingsRepository.save(settings);
            log.info("DataInitializer: AppSettings " + (settings.getId() == null ? "created" : "synchronized/patched") + " successfully.");
        }
    }
}

