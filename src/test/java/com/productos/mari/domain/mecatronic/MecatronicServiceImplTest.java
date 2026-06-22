package com.productos.mari.domain.mecatronic;

import com.productos.mari.domain.product.Product;
import com.productos.mari.domain.product.ProductRepository;
import com.productos.mari.domain.user.User;
import com.productos.mari.domain.user.UserRepository;
import com.productos.mari.domain.user.UserLinkedDevice;
import com.productos.mari.domain.user.UserLinkedDeviceRepository;
import com.productos.mari.domain.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MecatronicServiceImplTest {

    @Mock private MecatronicDeviceRepository deviceRepository;
    @Mock private DeviceVariableRepository variableRepository;
    @Mock private VariableReadingRepository readingRepository;
    @Mock private DeviceActionRepository actionRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserLinkedDeviceRepository userLinkedDeviceRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private MecatronicServiceImpl mecatronicService;

    private Product mockProduct;
    private MecatronicDevice mockDevice;

    @BeforeEach
    void setUp() {
        mockProduct = Product.builder()
                .id(1L)
                .name("IoT Device")
                .slug("iot-device")
                .mainImageUrl("image.jpg")
                .isMecatronic(true)
                .build();

        mockDevice = MecatronicDevice.builder()
                .id(10L)
                .product(mockProduct)
                .deviceSerial("SN-12345")
                .apiKey("test-api-key")
                .isEnabled(true)
                .variables(new ArrayList<>())
                .build();
    }

    @Test
    void getDashboard_ReturnsCompleteDto() {
        when(deviceRepository.findByProductId(1L)).thenReturn(Optional.of(mockDevice));
        
        DeviceVariable v1 = DeviceVariable.builder()
                .id(100L)
                .fieldKey("temp")
                .label("Temperature")
                .variableType("SENSOR")
                .build();
        mockDevice.setVariables(List.of(v1));

        VariableReading r1 = VariableReading.builder()
                .variable(v1)
                .value("25.5")
                .timestamp(LocalDateTime.now())
                .build();

        when(readingRepository.findTop10ReadingsPerVariableByDeviceId(10L))
                .thenReturn(List.of(r1));

        MecatronicDashboardDto result = mecatronicService.getDashboard(1L);

        assertNotNull(result);
        assertEquals("SN-12345", result.getDeviceSerial());
        assertEquals("IoT Device", result.getProductName());
        assertFalse(result.getVariables().isEmpty());
        assertEquals("temp", result.getVariables().get(0).getKey());
        assertEquals("25.5", result.getVariables().get(0).getLastValue());
        
        verify(readingRepository, times(1)).findTop10ReadingsPerVariableByDeviceId(10L);
    }

    @Test
    void processTelemetry_SavesReadingsAndNotifies() {
        when(deviceRepository.findByApiKey("test-api-key")).thenReturn(Optional.of(mockDevice));
        
        DeviceVariable v1 = DeviceVariable.builder().id(100L).fieldKey("temp").build();
        when(variableRepository.findByDeviceIdAndFieldKey(10L, "temp")).thenReturn(Optional.of(v1));

        Map<String, String> data = Map.of("temp", "26.7");
        mecatronicService.processTelemetry("test-api-key", data);

        verify(readingRepository, times(1)).save(any(VariableReading.class));
        verify(notificationService, times(1)).broadcastIotUpdate(eq(1L), eq("TELEMETRY_BATCH"));
        verify(deviceRepository, times(1)).save(mockDevice);
        assertNotNull(mockDevice.getLastConnection());
    }

    @Test
    void sendCommand_CreatesPendingAction() {
        DeviceVariable actuator = DeviceVariable.builder()
                .id(200L)
                .fieldKey("relay")
                .variableType("ACTUATOR")
                .device(mockDevice)
                .build();

        when(variableRepository.findById(200L)).thenReturn(Optional.of(actuator));

        mecatronicService.sendCommand(200L, "ON");

        verify(actionRepository, times(1)).save(any(DeviceAction.class));
        verify(notificationService, times(1)).broadcastIotUpdate(eq(1L), eq("COMMAND_PENDING"));
    }

    @Test
    void sendCommand_ThrowsException_IfSensor() {
        DeviceVariable sensor = DeviceVariable.builder()
                .id(200L)
                .variableType("SENSOR")
                .build();

        when(variableRepository.findById(200L)).thenReturn(Optional.of(sensor));

        RuntimeException ex = assertThrows(RuntimeException.class, 
            () -> mecatronicService.sendCommand(200L, "ON"));

        assertTrue(ex.getMessage().contains("Cannot send command to a SENSOR"));
        verify(actionRepository, never()).save(any());
    }

    @Test
    void sendCommand_shouldWorkForConfigType() {
        DeviceVariable configVar = DeviceVariable.builder()
                .id(300L)
                .fieldKey("threshold")
                .variableType("CONFIG")
                .device(mockDevice)
                .build();

        when(variableRepository.findById(300L)).thenReturn(Optional.of(configVar));

        mecatronicService.sendCommand(300L, "50");

        verify(actionRepository, times(1)).save(any(DeviceAction.class));
        verify(notificationService, times(1)).broadcastIotUpdate(eq(1L), eq("COMMAND_PENDING"));
    }

    @Test
    void getPendingCommands_shouldReturnAndMarkExecuted() {
        when(deviceRepository.findByApiKey("test-api-key")).thenReturn(Optional.of(mockDevice));

        DeviceVariable variable = DeviceVariable.builder().id(1L).fieldKey("relay").build();
        DeviceAction action = DeviceAction.builder()
                .id(1L)
                .variable(variable)
                .commandValue("ON")
                .status(DeviceAction.ActionStatus.PENDING)
                .build();

        when(actionRepository.findByVariableDeviceIdAndStatusOrderByCreatedAtAsc(
                mockDevice.getId(), DeviceAction.ActionStatus.PENDING))
                .thenReturn(List.of(action));

        Map<String, String> commands = mecatronicService.getPendingCommands("test-api-key");

        assertEquals("ON", commands.get("relay"));
        assertEquals(DeviceAction.ActionStatus.EXECUTED, action.getStatus());
        verify(actionRepository, times(1)).save(action);
    }

    @Test
    void getPendingCommands_shouldThrowIfDeviceNotFound() {
        when(deviceRepository.findByApiKey("bad-key")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> mecatronicService.getPendingCommands("bad-key"));
    }

    @Test
    void getDashboard_shouldCreateDefaultDeviceWhenNotFound() {
        when(deviceRepository.findByProductId(99L)).thenReturn(Optional.empty());
        Product product = Product.builder().id(99L).name("New Device").slug("new-device")
                .mainImageUrl("img.jpg").isMecatronic(true).build();
        when(productRepository.findById(99L)).thenReturn(Optional.of(product));

        MecatronicDevice newDevice = MecatronicDevice.builder()
                .id(50L)
                .product(product)
                .deviceSerial("SN-PENDING-99")
                .isEnabled(false)
                .variables(List.of())
                .build();
        when(deviceRepository.save(any(MecatronicDevice.class))).thenReturn(newDevice);
        when(readingRepository.findTop10ReadingsPerVariableByDeviceId(50L)).thenReturn(List.of());

        MecatronicDashboardDto result = mecatronicService.getDashboard(99L);

        assertNotNull(result);
        assertEquals("New Device", result.getProductName());
    }

    @Test
    void addVariable_shouldSaveVariableLinkedToDevice() {
        when(deviceRepository.findByProductId(1L)).thenReturn(Optional.of(mockDevice));

        DeviceVariable v = DeviceVariable.builder().fieldKey("humidity").variableType("SENSOR").build();
        mecatronicService.addVariable(1L, v);

        assertEquals(mockDevice, v.getDevice());
        verify(variableRepository, times(1)).save(v);
    }

    @Test
    void bindDevice_shouldThrowIfPinTooShort() {
        assertThrows(RuntimeException.class,
                () -> mecatronicService.bindDevice(1L, 1L, "SN-123", "12")); // pin < 4
    }

    @Test
    void bindDevice_shouldSetSerialAndApiKeyAndEnable() {
        when(deviceRepository.findByProductId(1L)).thenReturn(Optional.of(mockDevice));
        when(deviceRepository.save(any(MecatronicDevice.class))).thenReturn(mockDevice);

        mecatronicService.bindDevice(1L, 99L, "NEW-SERIAL", "1234");

        assertEquals("NEW-SERIAL", mockDevice.getDeviceSerial());
        assertTrue(mockDevice.getIsEnabled());
        verify(notificationService).broadcastDeviceListUpdate(99L);
    }

    @Test
    void bindExternalDevice_shouldThrowIfPinTooShort() {
        assertThrows(RuntimeException.class,
                () -> mecatronicService.bindExternalDevice(1L, 1L, "SN-EXT", "12"));
    }

    @Test
    void bindExternalDevice_shouldThrowIfAlreadyLinked() {
        when(userLinkedDeviceRepository.existsByUserIdAndProductId(1L, 1L)).thenReturn(true);
        assertThrows(RuntimeException.class,
                () -> mecatronicService.bindExternalDevice(1L, 1L, "SN-EXT", "1234"));
    }

    @Test
    void bindExternalDevice_shouldThrowIfProductNotMecatronic() {
        Product regularProduct = Product.builder().id(2L).name("Normal Product")
                .isMecatronic(false).build();
        when(userLinkedDeviceRepository.existsByUserIdAndProductId(1L, 2L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().id(1L).build()));
        when(productRepository.findById(2L)).thenReturn(Optional.of(regularProduct));

        assertThrows(RuntimeException.class,
                () -> mecatronicService.bindExternalDevice(2L, 1L, "SN-EXT", "1234"));
    }

    @Test
    void bindExternalDevice_shouldSaveLinkedDeviceAndNotify() {
        when(userLinkedDeviceRepository.existsByUserIdAndProductId(1L, 1L)).thenReturn(false);
        User user = User.builder().id(1L).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(deviceRepository.findByProductId(1L)).thenReturn(Optional.of(mockDevice));

        mecatronicService.bindExternalDevice(1L, 1L, "EXT-SERIAL", "9999");

        verify(userLinkedDeviceRepository, times(1)).save(any(UserLinkedDevice.class));
        verify(notificationService).broadcastDeviceListUpdate(1L);
    }

    @Test
    void getMyDevices_shouldReturnEmptyWhenNoDevices() {
        when(productRepository.findPurchasedMecatronicProducts(99L)).thenReturn(List.of());
        when(userLinkedDeviceRepository.findByUserId(99L)).thenReturn(List.of());

        List<MecatronicDashboardDto> result = mecatronicService.getMyDevices(99L, null, null, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void getMyDevices_shouldConsolidatePurchasedAndLinkedDevices() {
        Long userId = 1L;
        Product p1 = Product.builder().id(101L).name("Purchased").isMecatronic(true).build();
        Product p2 = Product.builder().id(102L).name("Linked").isMecatronic(true).mainImageUrl("p2.jpg").slug("p2").build();
        
        when(productRepository.findPurchasedMecatronicProducts(userId)).thenReturn(List.of(p1));
        
        UserLinkedDevice ld = UserLinkedDevice.builder().product(p2).serialNumber("EXT-SN").build();
        when(userLinkedDeviceRepository.findByUserId(userId)).thenReturn(List.of(ld));
        
        MecatronicDevice d1 = MecatronicDevice.builder().id(501L).product(p1).deviceSerial("SN-P1").build();
        MecatronicDevice d2 = MecatronicDevice.builder().id(502L).product(p2).deviceSerial("SN-P2").variables(List.of()).build();
        d1.setVariables(List.of());
        
        when(deviceRepository.findAllByProductIdIn(any())).thenReturn(List.of(d2)); // Only d2 found initially
        when(productRepository.findById(101L)).thenReturn(Optional.of(p1));
        when(deviceRepository.save(any())).thenReturn(d1); // Creating d1 lazily
        
        List<MecatronicDashboardDto> result = mecatronicService.getMyDevices(userId, null, null, null);
        
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(d -> d.getDeviceSerial().equals("EXT-SN"))); // External serial used
        assertTrue(result.stream().anyMatch(d -> d.getProductName().equals("Purchased")));
    }

    @Test
    void processTelemetry_shouldThrowIfDeviceNotFound() {
        when(deviceRepository.findByApiKey("wrong-key")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,
                () -> mecatronicService.processTelemetry("wrong-key", Map.of("temp", "25")));
    }
}
