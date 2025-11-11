package com.bigboxer23.solar_moon.device;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.data.Device;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DeviceComponentTest {

	@Mock
	private DeviceRepository mockRepository;

	private TestableDeviceComponent deviceComponent;

	private static final String CUSTOMER_ID = "test-customer-123";
	private static final String DEVICE_ID = "device-123";
	private static final String DEVICE_NAME = "Test Device";

	private static class TestableDeviceComponent extends DeviceComponent {
		private final DeviceRepository repository;

		public TestableDeviceComponent(DeviceRepository repository) {
			this.repository = repository;
		}

		@Override
		protected DeviceRepository getRepository() {
			return repository;
		}

		@Override
		public void deleteDevice(String id, String customerId) {
			Optional<Device> device = findDeviceById(id, customerId);
			if (device.isEmpty()) {
				return;
			}
			if (device.get().isDeviceSite()) {
				getDevicesBySiteId(customerId, device.get().getId()).forEach(childDevice -> {
					childDevice.setSite(NO_SITE);
					childDevice.setSiteId(NO_SITE);
					updateDevice(childDevice);
				});
			}
			getRepository().delete(device.get());
		}
	}

	@BeforeEach
	void setUp() {
		deviceComponent = new TestableDeviceComponent(mockRepository);
	}

	@Test
	void testFindDeviceByDeviceName_delegatesToRepository() {
		Device expectedDevice = createTestDevice();
		when(mockRepository.findDeviceByDeviceName(CUSTOMER_ID, DEVICE_NAME)).thenReturn(Optional.of(expectedDevice));

		Optional<Device> result = deviceComponent.findDeviceByDeviceName(CUSTOMER_ID, DEVICE_NAME);

		assertTrue(result.isPresent());
		assertEquals(expectedDevice, result.get());
		verify(mockRepository).findDeviceByDeviceName(CUSTOMER_ID, DEVICE_NAME);
	}

	@Test
	void testFindDeviceByName_delegatesToRepository() {
		Device expectedDevice = createTestDevice();
		when(mockRepository.findDeviceByName(CUSTOMER_ID, "Pretty Name")).thenReturn(Optional.of(expectedDevice));

		Optional<Device> result = deviceComponent.findDeviceByName(CUSTOMER_ID, "Pretty Name");

		assertTrue(result.isPresent());
		assertEquals(expectedDevice, result.get());
		verify(mockRepository).findDeviceByName(CUSTOMER_ID, "Pretty Name");
	}

	@Test
	void testFindDeviceByDeviceKey_delegatesToRepository() {
		Device expectedDevice = createTestDevice();
		String deviceKey = "device-key-123";
		when(mockRepository.findDeviceByDeviceKey(deviceKey)).thenReturn(Optional.of(expectedDevice));

		Device result = deviceComponent.findDeviceByDeviceKey(deviceKey);

		assertNotNull(result);
		assertEquals(expectedDevice, result);
		verify(mockRepository).findDeviceByDeviceKey(deviceKey);
	}

	@Test
	void testFindDeviceByDeviceKey_returnsNullWhenNotFound() {
		when(mockRepository.findDeviceByDeviceKey("non-existent")).thenReturn(Optional.empty());

		Device result = deviceComponent.findDeviceByDeviceKey("non-existent");

		assertNull(result);
		verify(mockRepository).findDeviceByDeviceKey("non-existent");
	}

	@Test
	void testGetDevicesBySiteId_delegatesToRepository() {
		String siteId = "site-123";
		List<Device> expectedDevices = Arrays.asList(createTestDevice(), createTestDevice());
		when(mockRepository.getDevicesBySiteId(CUSTOMER_ID, siteId)).thenReturn(expectedDevices);

		List<Device> result = deviceComponent.getDevicesBySiteId(CUSTOMER_ID, siteId);

		assertEquals(2, result.size());
		assertEquals(expectedDevices, result);
		verify(mockRepository).getDevicesBySiteId(CUSTOMER_ID, siteId);
	}

	@Test
	void testGetDevicesForCustomerId_delegatesToRepository() {
		List<Device> expectedDevices = Arrays.asList(createTestDevice(), createTestDevice(), createTestDevice());
		when(mockRepository.getDevicesForCustomerId(CUSTOMER_ID)).thenReturn(expectedDevices);

		List<Device> result = deviceComponent.getDevicesForCustomerId(CUSTOMER_ID);

		assertEquals(3, result.size());
		assertEquals(expectedDevices, result);
		verify(mockRepository).getDevicesForCustomerId(CUSTOMER_ID);
	}

	@Test
	void testFindDeviceById_withIdOnly_delegatesToRepository() {
		Device expectedDevice = createTestDevice();
		when(mockRepository.findDeviceById(DEVICE_ID)).thenReturn(Optional.of(expectedDevice));

		Optional<Device> result = deviceComponent.findDeviceById(DEVICE_ID);

		assertTrue(result.isPresent());
		assertEquals(expectedDevice, result.get());
		verify(mockRepository).findDeviceById(DEVICE_ID);
	}

	@Test
	void testFindDeviceById_withIdAndCustomerId_delegatesToRepository() {
		Device expectedDevice = createTestDevice();
		when(mockRepository.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.of(expectedDevice));

		Optional<Device> result = deviceComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID);

		assertTrue(result.isPresent());
		assertEquals(expectedDevice, result.get());
		verify(mockRepository).findDeviceById(DEVICE_ID, CUSTOMER_ID);
	}

	@Test
	void testGetDevices_virtual_delegatesToRepository() {
		List<Device> expectedDevices = Arrays.asList(createVirtualDevice());
		when(mockRepository.getDevices(true)).thenReturn(expectedDevices);

		List<Device> result = deviceComponent.getDevices(true);

		assertEquals(1, result.size());
		assertEquals(expectedDevices, result);
		verify(mockRepository).getDevices(true);
	}

	@Test
	void testGetDevices_nonVirtual_delegatesToRepository() {
		List<Device> expectedDevices = Arrays.asList(createTestDevice(), createTestDevice());
		when(mockRepository.getDevices(false)).thenReturn(expectedDevices);

		List<Device> result = deviceComponent.getDevices(false);

		assertEquals(2, result.size());
		assertEquals(expectedDevices, result);
		verify(mockRepository).getDevices(false);
	}

	@Test
	void testGetSites_delegatesToRepository() {
		List<Device> expectedSites = Arrays.asList(createSiteDevice());
		when(mockRepository.getSites()).thenReturn(expectedSites);

		List<Device> result = deviceComponent.getSites();

		assertEquals(1, result.size());
		assertEquals(expectedSites, result);
		verify(mockRepository).getSites();
	}

	@Test
	void testIsValidAdd_withValidDevice_returnsTrue() {
		Device device = createTestDevice();

		boolean result = deviceComponent.isValidAdd(device);

		assertTrue(result);
	}

	@Test
	void testIsValidAdd_withBlankClientId_returnsFalse() {
		Device device = createTestDevice();
		device.setClientId("");

		boolean result = deviceComponent.isValidAdd(device);

		assertFalse(result);
	}

	@Test
	void testIsValidAdd_withBlankDeviceName_returnsFalse() {
		Device device = createTestDevice();
		device.setDeviceName("");

		boolean result = deviceComponent.isValidAdd(device);

		assertFalse(result);
	}

	@Test
	void testIsValidAdd_withBlankId_returnsFalse() {
		Device device = createTestDevice();
		device.setId("");

		boolean result = deviceComponent.isValidAdd(device);

		assertFalse(result);
	}

	@Test
	void testIsValidUpdate_withValidDevice_returnsTrue() {
		Device device = createTestDevice();

		boolean result = deviceComponent.isValidUpdate(device);

		assertTrue(result);
	}

	@Test
	void testDeleteDevicesByCustomerId_deletesAllCustomerDevices() {
		List<Device> devices = Arrays.asList(
				createTestDevice("device-1", CUSTOMER_ID, "Device 1"),
				createTestDevice("device-2", CUSTOMER_ID, "Device 2"));

		when(mockRepository.getDevicesForCustomerId(CUSTOMER_ID)).thenReturn(devices);
		when(mockRepository.findDeviceById("device-1", CUSTOMER_ID)).thenReturn(Optional.of(devices.get(0)));
		when(mockRepository.findDeviceById("device-2", CUSTOMER_ID)).thenReturn(Optional.of(devices.get(1)));

		deviceComponent.deleteDevicesByCustomerId(CUSTOMER_ID);

		verify(mockRepository).getDevicesForCustomerId(CUSTOMER_ID);
		verify(mockRepository, times(2)).delete(any(Device.class));
	}

	private Device createTestDevice() {
		return createTestDevice(DEVICE_ID, CUSTOMER_ID, DEVICE_NAME);
	}

	private Device createTestDevice(String id, String clientId, String deviceName) {
		Device device = new Device(id, clientId, deviceName);
		device.setName("Pretty " + deviceName);
		device.setLatitude(45.0);
		device.setLongitude(-93.0);
		device.setSiteId("site-123");
		device.setSite("Test Site");
		return device;
	}

	private Device createVirtualDevice() {
		Device device = createTestDevice();
		device.setVirtual(true);
		return device;
	}

	private Device createSiteDevice() {
		Device device = createTestDevice();
		device.setIsSite("1");
		device.setSiteId(device.getId());
		device.setSite(device.getDeviceName());
		return device;
	}
}
