package com.productos.mari.domain.mecatronic;

import com.productos.mari.domain.user.User;
import com.productos.mari.domain.user.UserLinkedDevice;
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

import com.productos.mari.domain.product.ProductRepository;
import com.productos.mari.domain.mecatronic.MecatronicDashboardDto;

import com.productos.mari.domain.product.Product;
import com.productos.mari.domain.mecatronic.MecatronicService;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MecatronicServiceImpl implements MecatronicService {

        private final MecatronicDeviceRepository deviceRepository;
        private final DeviceVariableRepository variableRepository;
        private final VariableReadingRepository readingRepository;
        private final DeviceActionRepository actionRepository;
        private final ProductRepository productRepository;
        private final UserLinkedDeviceRepository userLinkedDeviceRepository;
        private final UserRepository userRepository;
        private final com.productos.mari.domain.notification.NotificationService notificationService;

        @Override
        @Transactional
        public void processTelemetry(String apiKey, Map<String, String> data) {
                MecatronicDevice device = deviceRepository.findByApiKey(apiKey)
                                .orElseThrow(() -> new RuntimeException("Device not found with provided API Key"));

                device.setLastConnection(LocalDateTime.now());
                deviceRepository.save(device);

                for (Map.Entry<String, String> entry : data.entrySet()) {
                        variableRepository.findByDeviceIdAndFieldKey(device.getId(), entry.getKey())
                                         .ifPresent(variable -> {
                                                 VariableReading reading = VariableReading.builder()
                                                                 .variable(variable)
                                                                 .value(entry.getValue())
                                                                 .timestamp(LocalDateTime.now())
                                                                 .build();
                                                 readingRepository.save(reading);
                                         });
                }

                // Real-Time ⚡: Notify active dashboards about new telemetry
                notificationService.broadcastIotUpdate(device.getProduct().getId(), "TELEMETRY_BATCH");
        }

        @Override
        @Transactional
        public Map<String, String> getPendingCommands(String apiKey) {
                MecatronicDevice device = deviceRepository.findByApiKey(apiKey)
                                .orElseThrow(() -> new RuntimeException("Device not found"));

                List<DeviceAction> pendingActions = actionRepository.findByVariableDeviceIdAndStatusOrderByCreatedAtAsc(
                                device.getId(), DeviceAction.ActionStatus.PENDING);

                Map<String, String> commands = new HashMap<>();
                for (DeviceAction action : pendingActions) {
                        commands.put(action.getVariable().getFieldKey(), action.getCommandValue());
                        action.setStatus(DeviceAction.ActionStatus.EXECUTED);
                        action.setExecutedAt(LocalDateTime.now());
                        actionRepository.save(action);
                }
                return commands;
        }

        @Override
        @Transactional
        public MecatronicDashboardDto getDashboard(Long productId) {
                MecatronicDevice device = deviceRepository.findByProductId(productId)
                                .orElseGet(() -> createDefaultDevice(productId));

                // Mejora: 1 sola query para las últimas 10 lecturas de cada variable
                // (antes era 1 query por cada variable → N+1 problem)
                // (luego era "traer todo" → Out of Memory risk)
                Map<Long, List<VariableReading>> readingsByVariable = readingRepository
                                .findTop10ReadingsPerVariableByDeviceId(device.getId())
                                .stream()
                                .collect(Collectors.groupingBy(
                                                r -> r.getVariable().getId()));

                return MecatronicDashboardDto.builder()
                                .id(device.getId())
                                .productId(productId)
                                .deviceSerial(device.getDeviceSerial())
                                .apiKey(device.getApiKey())
                                .productName(device.getProduct().getName())
                                .productImageUrl(device.getProduct().getMainImageUrl())
                                .productSlug(device.getProduct().getSlug())
                                .variables(device.getVariables() != null
                                                ? device.getVariables().stream()
                                                                .map(v -> mapToVariableDto(v, readingsByVariable))
                                                                .collect(Collectors.toList())
                                                : List.of())
                                .build();
        }

        private MecatronicDevice createDefaultDevice(Long productId) {
                Product product = productRepository.findById(productId)
                                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + productId));

                MecatronicDevice device = MecatronicDevice.builder()
                                .product(product)
                                .deviceSerial("SN-PENDING-" + product.getId())
                                .apiKey(java.util.UUID.randomUUID().toString().replace("-", ""))
                                .isEnabled(false)
                                .build();

                return deviceRepository.save(device);
        }

        @Override
        @Transactional
        public void addVariable(Long productId, DeviceVariable variable) {
                MecatronicDevice device = deviceRepository.findByProductId(productId)
                                .orElseGet(() -> createDefaultDevice(productId));

                variable.setDevice(device);
                variableRepository.save(variable);
        }

        @Override
        @Transactional
        public void sendCommand(Long variableId, String value) {
                DeviceVariable variable = variableRepository.findById(variableId)
                                .orElseThrow(() -> new RuntimeException("Variable not found"));

                String type = variable.getVariableType();
                if (!"ACTUATOR".equals(type) && !"CONFIG".equals(type)) {
                        throw new RuntimeException(
                                        "Cannot send command to a SENSOR variable. Only actuators and config parameters are allowed.");
                }

                DeviceAction action = DeviceAction.builder()
                                .variable(variable)
                                .commandValue(value)
                                .status(DeviceAction.ActionStatus.PENDING)
                                .createdAt(LocalDateTime.now())
                                .build();
                actionRepository.save(action);

                // Real-Time ⚡: Notify UI that a command is pending
                notificationService.broadcastIotUpdate(variable.getDevice().getProduct().getId(), "COMMAND_PENDING");
        }

        private MecatronicDashboardDto.VariableDto mapToVariableDto(DeviceVariable v,
                        Map<Long, List<VariableReading>> readingsByVariable) {
                // Lecturas ya pre-cargadas en memoria, sin ir a la BD
                List<VariableReading> lastReadings = readingsByVariable.getOrDefault(v.getId(), List.of());

                return MecatronicDashboardDto.VariableDto.builder()
                                .id(v.getId())
                                .key(v.getFieldKey())
                                .label(v.getLabel())
                                .unit(v.getUnit())
                                .type(v.getVariableType())
                                .icon(v.getIcon())
                                .lastValue(lastReadings.isEmpty() ? "N/A" : lastReadings.get(0).getValue())
                                .minValue(v.getMinValue())
                                .maxValue(v.getMaxValue())
                                .history(lastReadings.stream().map(r -> MecatronicDashboardDto.ReadingDto.builder()
                                                .value(r.getValue())
                                                .timestamp(r.getTimestamp().toString())
                                                .build()).collect(Collectors.toList()))
                                .build();
        }

        @Override
        @Transactional
        public List<MecatronicDashboardDto> getMyDevices(Long userId, String q, String type, String status) {
                // 1. Obtener todos los productos comprados y vinculados
                List<Product> purchasedProducts = productRepository.findPurchasedMecatronicProducts(userId);
                List<UserLinkedDevice> linkedDevices = userLinkedDeviceRepository.findByUserId(userId);

                // Consolidar lista de IDs de producto únicos
                java.util.Set<Long> productIds = purchasedProducts.stream().map(Product::getId)
                                .collect(Collectors.toSet());
                linkedDevices.forEach(ld -> productIds.add(ld.getProduct().getId()));

                if (productIds.isEmpty())
                        return List.of();

                // 2. Cargar dispositivos existentes (con sus variables precargadas)
                List<MecatronicDevice> devices = new java.util.ArrayList<>(deviceRepository.findAllByProductIdIn(new java.util.ArrayList<>(productIds)));
                
                // 2.1 Inicializar dispositivos faltantes (Lazy creation para compras nuevas)
                java.util.Set<Long> foundProductIds = devices.stream().map(d -> d.getProduct().getId()).collect(java.util.stream.Collectors.toSet());
                for (Long pid : productIds) {
                    if (!foundProductIds.contains(pid)) {
                        devices.add(createDefaultDevice(pid));
                    }
                }

                List<Long> deviceIds = devices.stream()
                        .map(MecatronicDevice::getId)
                        .filter(id -> id != null)
                        .collect(java.util.stream.Collectors.toList());

                // 3. Cargar Top 10 lecturas para TODOS los dispositivos en una sola query (Protegido contra lista vacía)
                final java.util.Map<Long, java.util.List<VariableReading>> readingsByVariable = deviceIds.isEmpty() 
                    ? java.util.Collections.emptyMap()
                    : readingRepository.findTop10ReadingsPerVariableByDeviceIds(deviceIds)
                                .stream()
                                .collect(java.util.stream.Collectors.groupingBy(r -> r.getVariable().getId()));

                // 4. Mapear a DTOs y aplicar filtros
                return devices.stream().map(device -> {
                        boolean isExternallyLinked = linkedDevices.stream()
                                        .anyMatch(ld -> ld.getProduct().getId().equals(device.getProduct().getId()));

                        MecatronicDashboardDto dto = MecatronicDashboardDto.builder()
                                        .id(device.getId())
                                        .productId(device.getProduct().getId())
                                        .deviceSerial(device.getDeviceSerial())
                                        .apiKey(device.getApiKey())
                                        .productName(device.getProduct().getName())
                                        .productImageUrl(device.getProduct().getMainImageUrl())
                                        .productSlug(device.getProduct().getSlug())
                                        .isExternallyLinked(isExternallyLinked)
                                        .variables(device.getVariables().stream()
                                                        .map(v -> mapToVariableDto(v, readingsByVariable))
                                                        .collect(Collectors.toList()))
                                        .build();

                        // Ajustar serial si es vinculado externamente
                        if (isExternallyLinked) {
                                linkedDevices.stream()
                                                .filter(ld -> ld.getProduct().getId()
                                                                .equals(device.getProduct().getId()))
                                                .findFirst()
                                                .ifPresent(ld -> dto.setDeviceSerial(ld.getSerialNumber()));
                        }

                        return dto;
                }).filter(dto -> {
                        // Aplicar búsqueda por texto (q)
                        if (q != null && !q.isEmpty()) {
                                String searchLower = q.toLowerCase();
                                boolean matchesName = dto.getProductName().toLowerCase().contains(searchLower);
                                boolean matchesSerial = dto.getDeviceSerial() != null && dto.getDeviceSerial().toLowerCase().contains(searchLower);
                                if (!matchesName && !matchesSerial) return false;
                        }

                        // Filtrar por tipo (INTERNAL / EXTERNAL)
                        if (type != null && !type.isEmpty()) {
                                if ("EXTERNAL".equalsIgnoreCase(type) && !Boolean.TRUE.equals(dto.getIsExternallyLinked())) return false;
                                if ("INTERNAL".equalsIgnoreCase(type) && Boolean.TRUE.equals(dto.getIsExternallyLinked())) return false;
                        }

                        // Filtrar por estado (BOUND / PENDING)
                        if (status != null && !status.isEmpty()) {
                                String s = dto.getDeviceSerial();
                                boolean isPending = s == null || s.isEmpty() || s.equals("---") || s.equals("PENDING") || s.startsWith("SN-PENDING-");
                                if ("PENDING".equalsIgnoreCase(status) && !isPending) return false;
                                if ("BOUND".equalsIgnoreCase(status) && isPending) return false;
                        }

                        return true;
                }).collect(Collectors.toList());
        }

        @Override
        @Transactional
        public void bindDevice(Long productId, Long userId, String serial, String pin) {
                // Validate PIN (simulation of standard IoT secure PIN binding)
                if (pin == null || pin.length() < 4) {
                        throw new RuntimeException("PIN de vinculación inválido.");
                }

                // Find the device purchased by the user (simulated via productId)
                MecatronicDevice device = deviceRepository.findByProductId(productId)
                                .orElseThrow(() -> new RuntimeException("Dispositivo no encontrado o no adquirido."));

                // Generate a real API key once bound
                String newApiKey = java.util.UUID.randomUUID().toString().replace("-", "");

                device.setDeviceSerial(serial);
                device.setApiKey(newApiKey);
                device.setIsEnabled(true);
                deviceRepository.save(device);

                // Real-Time ⚡: Notify user to refresh device list
                notificationService.broadcastDeviceListUpdate(userId);
        }

        @Override
        @Transactional
        public void bindExternalDevice(Long productId, Long userId, String serial, String pin) {
                if (pin == null || pin.length() < 4) {
                        throw new RuntimeException("PIN de vinculación inválido. Debe contener al menos 4 dígitos.");
                }

                if (userLinkedDeviceRepository.existsByUserIdAndProductId(userId, productId)) {
                        throw new RuntimeException("El dispositivo ya ha sido vinculado a esta cuenta.");
                }

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

                Product product = productRepository.findById(productId)
                                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

                if (!Boolean.TRUE.equals(product.getIsMecatronic())) {
                        throw new RuntimeException(
                                        "El producto seleccionado no pertenece a la línea de Mecatrónica/IoT.");
                }

                // Initialize shared singleton device to ensure standard dashboard loading can
                // occur
                deviceRepository.findByProductId(productId)
                                .orElseGet(() -> createDefaultDevice(productId));

                UserLinkedDevice linkedDevice = UserLinkedDevice.builder()
                                .user(user)
                                .product(product)
                                .serialNumber(serial)
                                .build();

                userLinkedDeviceRepository.save(linkedDevice);

                // Real-Time ⚡: Notify user to refresh device list
                notificationService.broadcastDeviceListUpdate(userId);
        }
}
