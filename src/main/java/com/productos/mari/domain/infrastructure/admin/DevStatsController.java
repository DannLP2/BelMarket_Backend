package com.productos.mari.domain.infrastructure.admin;
import com.productos.mari.domain.mecatronic.MecatronicDeviceRepository;
import com.productos.mari.domain.mecatronic.DeviceVariableRepository;
import com.productos.mari.domain.mecatronic.VariableReadingRepository;
import com.productos.mari.domain.mecatronic.DeviceActionRepository;
import com.productos.mari.domain.auth.SecurityLogRepository;
import com.productos.mari.domain.product.ProductDetailListRepository;
import com.productos.mari.domain.mecatronic.MecatronicDevice;
import com.productos.mari.domain.mecatronic.DeviceVariable;
import com.productos.mari.domain.mecatronic.VariableReading;
import com.productos.mari.domain.mecatronic.DeviceAction;
import com.productos.mari.domain.user.UserLinkedDeviceRepository;
import com.productos.mari.domain.user.UserRepository;
import com.productos.mari.domain.user.UserRepository;
import com.productos.mari.domain.notification.NotificationRepository;
import com.productos.mari.domain.review.ReviewRepository;
import com.productos.mari.domain.reservation.ReservationRepository;
import com.productos.mari.domain.product.ProductRepository;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * [DEV] Endpoint de estadísticas en tiempo real para el panel de desarrolladores.
 * Solo accesible con rol ADMIN.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/dev")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class DevStatsController {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final ReviewRepository reviewRepository;
    private final MecatronicDeviceRepository mecatronicDeviceRepository;
    private final UserLinkedDeviceRepository userLinkedDeviceRepository;
    private final SecurityLogRepository securityLogRepository;
    private final NotificationRepository notificationRepository;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDevStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        // ── Productos
        try {
            Map<String, Object> products = new LinkedHashMap<>();
            products.put("total", productRepository.count());
            products.put("active", safeCount(() -> productRepository.countActiveProducts()));
            products.put("mecatronic", safeCount(() -> productRepository.countMecatronicProducts()));
            stats.put("products", products);
        } catch (Exception e) {
            log.error("[DevStats] Error counting products: {}", e.getMessage(), e);
            Map<String, Object> errorMap = new LinkedHashMap<>();
            errorMap.put("total", -1);
            errorMap.put("active", -1);
            errorMap.put("mecatronic", -1);
            errorMap.put("error", e.getMessage() != null ? e.getMessage() : "Unknown error");
            stats.put("products", errorMap);
        }

        // ── Usuarios
        try {
            stats.put("users", Map.of("total", userRepository.count()));
        } catch (Exception e) {
            log.error("[DevStats] Error counting users: {}", e.getMessage());
            stats.put("users", Map.of("total", -1));
        }

        // ── Reservas
        try {
            stats.put("reservations", Map.of("total", reservationRepository.count()));
        } catch (Exception e) {
            log.error("[DevStats] Error counting reservations: {}", e.getMessage());
            stats.put("reservations", Map.of("total", -1));
        }

        // ── Reseñas
        try {
            stats.put("reviews", Map.of("total", reviewRepository.count()));
        } catch (Exception e) {
            log.error("[DevStats] Error counting reviews: {}", e.getMessage());
            stats.put("reviews", Map.of("total", -1));
        }

        // ── IoT / Dispositivos
        try {
            long internal = mecatronicDeviceRepository.count();
            long external = userLinkedDeviceRepository.count();
            stats.put("iot", Map.of("internal", internal, "external", external, "total", internal + external));
        } catch (Exception e) {
            log.error("[DevStats] Error counting IoT devices: {}", e.getMessage());
            stats.put("iot", Map.of("internal", 0, "external", 0, "total", 0));
        }

        // ── Seguridad
        try {
            Map<String, Object> security = new LinkedHashMap<>();
            security.put("totalLogs", securityLogRepository.count());
            security.put("logsLastHour", securityLogRepository.countLogsSince(LocalDateTime.now().minusHours(1)));
            stats.put("security", security);
        } catch (Exception e) {
            log.error("[DevStats] Error counting security logs: {}", e.getMessage());
            stats.put("security", Map.of("totalLogs", -1, "logsLastHour", 0));
        }

        // ── Notificaciones
        try {
            stats.put("notifications", Map.of("total", notificationRepository.count()));
        } catch (Exception e) {
            log.error("[DevStats] Error counting notifications: {}", e.getMessage());
            stats.put("notifications", Map.of("total", -1));
        }

        // ── Metadata
        stats.put("_meta", Map.of(
            "generatedAt", LocalDateTime.now().toString(),
            "environment", "development"
        ));

        return ResponseEntity.ok(stats);
    }

    /**
     * Obtiene las últimas 100 líneas del archivo de log.
     */
    @GetMapping("/logs")
    public ResponseEntity<Map<String, Object>> getLogs() {
        Map<String, Object> response = new LinkedHashMap<>();
        Path logPath = Paths.get("logs", "belmarket.log");

        if (!Files.exists(logPath)) {
            response.put("error", "Archivo de log no encontrado en: " + logPath.toAbsolutePath());
            response.put("lines", Collections.emptyList());
            return ResponseEntity.ok(response);
        }

        try (Stream<String> stream = Files.lines(logPath)) {
            // Leemos todo y tomamos las últimas 100
            List<String> allLines = stream.collect(Collectors.toList());
            int start = Math.max(0, allLines.size() - 100);
            List<String> lastLines = allLines.subList(start, allLines.size());
            
            response.put("lines", lastLines);
            response.put("totalLines", allLines.size());
            response.put("lastUpdate", LocalDateTime.now().toString());
        } catch (Exception e) {
            log.error("[DevStats] Error reading log file: {}", e.getMessage());
            response.put("error", "Error al leer logs: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Prueba de Estrés Real: Carga de CPU y Memoria (150MB aprox).
     * El objetivo es estresar la JVM para que el monitor visual reaccione.
     */
    @GetMapping("/stress")
    public ResponseEntity<Map<String, Object>> performStressTest() {
        log.warn("[STRESS-TEST] Iniciando inyección de carga pesada...");
        
        long start = System.currentTimeMillis();
        
        // 1. Stress de CPU (Cálculos matemáticos complejos)
        double a = 0;
        for (int i = 0; i < 5000000; i++) {
            a += Math.sqrt(i) * Math.sin(i);
        }

        // 2. Stress de Memoria (Asignamos ~600MB de objetos temporales)
        List<byte[]> stressList = new ArrayList<>();
        try {
            log.warn("[STRESS-TEST] Reservando 600MB de memoria RAM...");
            for (int i = 0; i < 600; i++) {
                stressList.add(new byte[1024 * 1024]); // 1MB cada fragmento
            }
            
            // 3. Pausa Táctica (3 segundos para que el monitor visual refresque)
            log.info("[STRESS-TEST] Carga máxima alcanzada. Manteniendo 3 segundos...");
            Thread.sleep(3000);
            
        } catch (OutOfMemoryError e) {
            log.error("[STRESS-TEST] El servidor alcanzó el límite de memoria real.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long end = System.currentTimeMillis();
        log.info("[STRESS-TEST] Carga completada en {}ms. Resultado CPU: {}", (end - start), a);
        
        // Obtenemos la salud actual para que el frontend vea el pico inmediatamente
        Map<String, Object> freshHealth = (Map<String, Object>) getSystemHealth().getBody();
        if (freshHealth != null) {
            freshHealth.put("stressDurationMs", (end - start));
        }
        
        // NOTA: Al salir de este método, 'stressList' queda huérfano y el GC lo limpiará pronto.
        return ResponseEntity.ok(freshHealth);
    }

    /**
     * Obtiene métricas de salud del sistema (RAM, Uptime).
     */
    @GetMapping("/system-health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        Map<String, Object> health = new LinkedHashMap<>();
        Runtime runtime = Runtime.getRuntime();

        long maxMemory = runtime.maxMemory();
        long allocatedMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = allocatedMemory - freeMemory;

        health.put("memory", Map.of(
            "used", usedMemory / (1024 * 1024), // MB
            "allocated", allocatedMemory / (1024 * 1024), // MB
            "max", maxMemory / (1024 * 1024), // MB
            "percentUsed", (double) usedMemory / maxMemory * 100
        ));

        health.put("processors", runtime.availableProcessors());
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("os", System.getProperty("os.name") + " (" + System.getProperty("os.version") + ")");

        return ResponseEntity.ok(health);
    }

    /**
     * Helper para ejecutar un count de forma segura sin propagar excepciones.
     */
    private long safeCount(CountSupplier supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.warn("[DevStats] safeCount failed: {}", e.getMessage());
            return -1;
        }
    }

    @FunctionalInterface
    private interface CountSupplier {
        long get() throws Exception;
    }
}


