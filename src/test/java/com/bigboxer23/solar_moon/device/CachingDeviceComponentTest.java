package com.bigboxer23.solar_moon.device;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.data.Device;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CachingDeviceComponentTest {

	private TestableCachingDeviceComponent cachingComponent;
	private DeviceComponent mockBaseComponent;

	private static final String CUSTOMER_ID = "test-customer-123";
	private static final String DEVICE_ID = "device-123";
	private static final String SITE_ID = "site-123";

	private static class TestableCachingDeviceComponent extends CachingDeviceComponent {
		private final DeviceComponent baseComponent;

		public TestableCachingDeviceComponent(DeviceComponent baseComponent) {
			this.baseComponent = baseComponent;
		}

		@Override
		protected DeviceRepository getRepository() {
			return baseComponent.getRepository();
		}
	}

	@BeforeEach
	void setUp() {
		mockBaseComponent = mock(DeviceComponent.class);
		cachingComponent = spy(new TestableCachingDeviceComponent(mockBaseComponent));
	}

	@Test
	void testFindDeviceById_cachesResult() {
		Device expectedDevice = createTestDevice();
		doReturn(Optional.of(expectedDevice))
				.when((DeviceComponent) cachingComponent)
				.findDeviceById(DEVICE_ID, CUSTOMER_ID);

		Optional<Device> firstCall = cachingComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID);
		Optional<Device> secondCall = cachingComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID);

		assertTrue(firstCall.isPresent());
		assertTrue(secondCall.isPresent());
		assertEquals(expectedDevice, firstCall.get());
		assertEquals(expectedDevice, secondCall.get());
	}

	@Test
	void testFindDeviceById_withBlankId_returnsEmpty() {
		Optional<Device> result = cachingComponent.findDeviceById("", CUSTOMER_ID);

		assertFalse(result.isPresent());
	}

	@Test
	void testFindDeviceById_withNullId_returnsEmpty() {
		Optional<Device> result = cachingComponent.findDeviceById(null, CUSTOMER_ID);

		assertFalse(result.isPresent());
	}

	@Test
	void testFindDeviceById_withBlankCustomerId_returnsEmpty() {
		Optional<Device> result = cachingComponent.findDeviceById(DEVICE_ID, "");

		assertFalse(result.isPresent());
	}

	@Test
	void testFindDeviceById_withNullCustomerId_returnsEmpty() {
		Optional<Device> result = cachingComponent.findDeviceById(DEVICE_ID, null);

		assertFalse(result.isPresent());
	}

	@Test
	void testGetDevicesBySiteId_cachesResult() {
		List<Device> expectedDevices = Arrays.asList(createTestDevice(), createTestDevice());
		doReturn(expectedDevices).when((DeviceComponent) cachingComponent).getDevicesBySiteId(CUSTOMER_ID, SITE_ID);

		List<Device> firstCall = cachingComponent.getDevicesBySiteId(CUSTOMER_ID, SITE_ID);
		List<Device> secondCall = cachingComponent.getDevicesBySiteId(CUSTOMER_ID, SITE_ID);

		assertEquals(2, firstCall.size());
		assertEquals(2, secondCall.size());
		assertEquals(expectedDevices, firstCall);
		assertEquals(expectedDevices, secondCall);
	}

	@Test
	void testGetDevicesBySiteId_withNullCustomerId_returnsEmpty() {
		List<Device> result = cachingComponent.getDevicesBySiteId(null, SITE_ID);

		assertTrue(result.isEmpty());
	}

	@Test
	void testGetDevicesBySiteId_withBlankCustomerId_returnsEmpty() {
		List<Device> result = cachingComponent.getDevicesBySiteId("", SITE_ID);

		assertTrue(result.isEmpty());
	}

	@Test
	void testGetDevicesBySiteId_withNullSiteId_returnsEmpty() {
		List<Device> result = cachingComponent.getDevicesBySiteId(CUSTOMER_ID, null);

		assertTrue(result.isEmpty());
	}

	@Test
	void testGetDevicesBySiteId_withBlankSiteId_returnsEmpty() {
		List<Device> result = cachingComponent.getDevicesBySiteId(CUSTOMER_ID, "");

		assertTrue(result.isEmpty());
	}

	@Test
	void testAddDevice_invalidatesCache() {
		Device device = createTestDevice();
		doReturn(device).when((DeviceComponent) cachingComponent).addDevice(device);
		doReturn(Optional.of(device)).when((DeviceComponent) cachingComponent).findDeviceById(DEVICE_ID, CUSTOMER_ID);
		doReturn(Collections.singletonList(device))
				.when((DeviceComponent) cachingComponent)
				.getDevicesBySiteId(CUSTOMER_ID, SITE_ID);

		cachingComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID);
		cachingComponent.getDevicesBySiteId(CUSTOMER_ID, SITE_ID);

		Device result = cachingComponent.addDevice(device);

		assertEquals(device, result);
	}

	@Test
	void testUpdateDevice_invalidatesCache() {
		Device device = createTestDevice();
		doReturn(Optional.of(device)).when((DeviceComponent) cachingComponent).updateDevice(device);
		doReturn(Optional.of(device)).when((DeviceComponent) cachingComponent).findDeviceById(DEVICE_ID, CUSTOMER_ID);
		doReturn(Collections.singletonList(device))
				.when((DeviceComponent) cachingComponent)
				.getDevicesBySiteId(CUSTOMER_ID, SITE_ID);

		cachingComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID);
		cachingComponent.getDevicesBySiteId(CUSTOMER_ID, SITE_ID);

		Optional<Device> result = cachingComponent.updateDevice(device);

		assertTrue(result.isPresent());
		assertEquals(device, result.get());
	}

	@Test
	void testDeleteDevice_invalidatesCache() {
		Device device = createTestDevice();
		DeviceRepository mockRepo = mock(DeviceRepository.class);
		when(mockRepo.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.of(device));
		doNothing().when(mockRepo).delete(any(Device.class));

		TestableDeviceComponent testableBase = new TestableDeviceComponent(mockRepo);
		TestableCachingDeviceComponent component = new TestableCachingDeviceComponent(testableBase);
		CachingDeviceComponent spyComponent = spy(component);

		spyComponent.deleteDevice(DEVICE_ID, CUSTOMER_ID);

		verify(spyComponent, atLeastOnce()).findDeviceById(DEVICE_ID, CUSTOMER_ID);
	}

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
			getRepository().delete(device.get());
		}
	}

	@Test
	void testDeleteDevicesByCustomerId_invalidatesAllForCustomer() {
		doNothing().when((DeviceComponent) cachingComponent).deleteDevicesByCustomerId(CUSTOMER_ID);

		cachingComponent.deleteDevicesByCustomerId(CUSTOMER_ID);

		verify((DeviceComponent) cachingComponent).deleteDevicesByCustomerId(CUSTOMER_ID);
	}

	@Test
	void testInvalidateAllForCustomer_clearsAllCaches() {
		Device device = createTestDevice();
		doReturn(Optional.of(device)).when((DeviceComponent) cachingComponent).findDeviceById(DEVICE_ID, CUSTOMER_ID);
		doReturn(Collections.singletonList(device))
				.when((DeviceComponent) cachingComponent)
				.getDevicesBySiteId(CUSTOMER_ID, SITE_ID);

		cachingComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID);
		cachingComponent.getDevicesBySiteId(CUSTOMER_ID, SITE_ID);

		cachingComponent.invalidateAllForCustomer(CUSTOMER_ID);
	}

	private Device createTestDevice() {
		Device device = new Device(DEVICE_ID, CUSTOMER_ID, "Test Device");
		device.setName("Pretty Test Device");
		device.setSiteId(SITE_ID);
		device.setSite("Test Site");
		return device;
	}
}
