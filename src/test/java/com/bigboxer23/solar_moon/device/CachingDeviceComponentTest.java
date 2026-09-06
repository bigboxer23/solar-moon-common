package com.bigboxer23.solar_moon.device;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.alarm.AlarmComponent;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.location.LocationComponent;
import com.bigboxer23.solar_moon.subscription.SubscriptionComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CachingDeviceComponentTest {

	@Mock
	private DeviceRepository mockRepository;

	@Mock
	private SubscriptionComponent mockSubscriptionComponent;

	@Mock
	private LocationComponent mockLocationComponent;

	@Mock
	private DeviceUpdateComponent mockDeviceUpdateComponent;

	@Mock
	private AlarmComponent mockAlarmComponent;

	private CachingDeviceComponent cachingComponent;

	private static final String CUSTOMER_ID = "test-customer-123";
	private static final String DEVICE_ID = "device-123";
	private static final String SITE_ID = "site-123";

	private class TestableCachingDeviceComponent extends CachingDeviceComponent {

		@Override
		protected DeviceRepository getRepository() {
			return mockRepository;
		}

		@Override
		protected SubscriptionComponent getSubscriptionComponent() {
			return mockSubscriptionComponent;
		}

		@Override
		protected LocationComponent getLocationComponent() {
			return mockLocationComponent;
		}

		@Override
		protected DeviceUpdateComponent getDeviceUpdateComponent() {
			return mockDeviceUpdateComponent;
		}

		@Override
		protected AlarmComponent getAlarmComponent() {
			return mockAlarmComponent;
		}
	}

	@BeforeEach
	void setUp() {
		cachingComponent = new TestableCachingDeviceComponent();
	}

	@Test
	void testFindDeviceById_repeatedLookupsHitRepositoryOnce() {
		Device device = createTestDevice();
		when(mockRepository.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.of(device));

		Optional<Device> first = cachingComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID);
		Optional<Device> second = cachingComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID);

		assertEquals(device, first.orElseThrow());
		assertEquals(device, second.orElseThrow());
		verify(mockRepository, times(1)).findDeviceById(DEVICE_ID, CUSTOMER_ID);
	}

	@Test
	void testFindDeviceById_cachesPerCustomer() {
		Device device = createTestDevice();
		when(mockRepository.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.of(device));
		when(mockRepository.findDeviceById(DEVICE_ID, "other-customer")).thenReturn(Optional.empty());

		cachingComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID);
		Optional<Device> otherCustomer = cachingComponent.findDeviceById(DEVICE_ID, "other-customer");

		assertTrue(otherCustomer.isEmpty());
		verify(mockRepository).findDeviceById(DEVICE_ID, CUSTOMER_ID);
		verify(mockRepository).findDeviceById(DEVICE_ID, "other-customer");
	}

	@Test
	void testFindDeviceById_cachesMissesToo() {
		when(mockRepository.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.empty());

		assertTrue(cachingComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID).isEmpty());
		assertTrue(cachingComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID).isEmpty());

		verify(mockRepository, times(1)).findDeviceById(DEVICE_ID, CUSTOMER_ID);
	}

	@Test
	void testFindDeviceById_withBlankId_doesNotTouchRepository() {
		assertTrue(cachingComponent.findDeviceById("", CUSTOMER_ID).isEmpty());

		verifyNoInteractions(mockRepository);
	}

	@Test
	void testFindDeviceById_withNullId_doesNotTouchRepository() {
		assertTrue(cachingComponent.findDeviceById(null, CUSTOMER_ID).isEmpty());

		verifyNoInteractions(mockRepository);
	}

	@Test
	void testFindDeviceById_withBlankCustomerId_doesNotTouchRepository() {
		assertTrue(cachingComponent.findDeviceById(DEVICE_ID, "").isEmpty());

		verifyNoInteractions(mockRepository);
	}

	@Test
	void testFindDeviceById_withNullCustomerId_doesNotTouchRepository() {
		assertTrue(cachingComponent.findDeviceById(DEVICE_ID, null).isEmpty());

		verifyNoInteractions(mockRepository);
	}

	@Test
	void testGetDevicesBySiteId_repeatedLookupsHitRepositoryOnce() {
		List<Device> devices = Arrays.asList(createTestDevice(), createTestDevice());
		when(mockRepository.getDevicesBySiteId(CUSTOMER_ID, SITE_ID)).thenReturn(devices);

		List<Device> first = cachingComponent.getDevicesBySiteId(CUSTOMER_ID, SITE_ID);
		List<Device> second = cachingComponent.getDevicesBySiteId(CUSTOMER_ID, SITE_ID);

		assertEquals(devices, first);
		assertEquals(devices, second);
		verify(mockRepository, times(1)).getDevicesBySiteId(CUSTOMER_ID, SITE_ID);
	}

	@Test
	void testGetDevicesBySiteId_withNullCustomerId_doesNotTouchRepository() {
		assertTrue(cachingComponent.getDevicesBySiteId(null, SITE_ID).isEmpty());

		verifyNoInteractions(mockRepository);
	}

	@Test
	void testGetDevicesBySiteId_withBlankCustomerId_doesNotTouchRepository() {
		assertTrue(cachingComponent.getDevicesBySiteId("", SITE_ID).isEmpty());

		verifyNoInteractions(mockRepository);
	}

	@Test
	void testGetDevicesBySiteId_withNullSiteId_doesNotTouchRepository() {
		assertTrue(cachingComponent.getDevicesBySiteId(CUSTOMER_ID, null).isEmpty());

		verifyNoInteractions(mockRepository);
	}

	@Test
	void testGetDevicesBySiteId_withBlankSiteId_doesNotTouchRepository() {
		assertTrue(cachingComponent.getDevicesBySiteId(CUSTOMER_ID, "").isEmpty());

		verifyNoInteractions(mockRepository);
	}

	@Test
	void testAddDevice_invalidatesDeviceCache() {
		Device device = createTestDevice();
		when(mockRepository.findDeviceById(DEVICE_ID, CUSTOMER_ID))
				.thenReturn(Optional.empty())
				.thenReturn(Optional.of(device));
		when(mockRepository.findDeviceByDeviceName(CUSTOMER_ID, "Test Device")).thenReturn(Optional.empty());
		when(mockSubscriptionComponent.canAddAnotherDevice(CUSTOMER_ID)).thenReturn(true);
		when(mockRepository.add(device)).thenReturn(device);

		cachingComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID);
		cachingComponent.addDevice(device);

		assertEquals(
				device, cachingComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID).orElseThrow());
	}

	@Test
	void testAddDevice_invalidatesSiteCache() {
		Device device = createTestDevice();
		when(mockRepository.getDevicesBySiteId(CUSTOMER_ID, SITE_ID))
				.thenReturn(List.of())
				.thenReturn(List.of(device));
		when(mockRepository.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.empty());
		when(mockRepository.findDeviceByDeviceName(CUSTOMER_ID, "Test Device")).thenReturn(Optional.empty());
		when(mockSubscriptionComponent.canAddAnotherDevice(CUSTOMER_ID)).thenReturn(true);
		when(mockRepository.add(device)).thenReturn(device);

		assertTrue(cachingComponent.getDevicesBySiteId(CUSTOMER_ID, SITE_ID).isEmpty());
		cachingComponent.addDevice(device);

		assertEquals(
				1, cachingComponent.getDevicesBySiteId(CUSTOMER_ID, SITE_ID).size());
	}

	@Test
	void testUpdateDevice_invalidatesCacheSoNextReadIsFresh() {
		Device stale = createTestDevice();
		stale.setName("Old Name");
		Device updated = createTestDevice();
		updated.setName("New Name");

		when(mockRepository.findDeviceById(DEVICE_ID, CUSTOMER_ID))
				.thenReturn(Optional.of(stale))
				.thenReturn(Optional.of(updated));
		when(mockRepository.findDeviceByDeviceName(CUSTOMER_ID, "Test Device")).thenReturn(Optional.empty());
		when(mockRepository.update(updated)).thenReturn(Optional.of(updated));

		assertEquals(
				"Old Name",
				cachingComponent
						.findDeviceById(DEVICE_ID, CUSTOMER_ID)
						.orElseThrow()
						.getName());
		cachingComponent.updateDevice(updated);

		assertEquals(
				"New Name",
				cachingComponent
						.findDeviceById(DEVICE_ID, CUSTOMER_ID)
						.orElseThrow()
						.getName());
	}

	@Test
	void testDeleteDevice_invalidatesCacheAndCascades() {
		Device device = createTestDevice();
		when(mockRepository.findDeviceById(DEVICE_ID, CUSTOMER_ID))
				.thenReturn(Optional.of(device))
				.thenReturn(Optional.of(device))
				.thenReturn(Optional.empty());

		cachingComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID);
		cachingComponent.deleteDevice(DEVICE_ID, CUSTOMER_ID);

		assertTrue(cachingComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID).isEmpty());
		verify(mockRepository).delete(device);
		verify(mockDeviceUpdateComponent).delete(DEVICE_ID);
		verify(mockAlarmComponent).deleteAlarmByDeviceId(CUSTOMER_ID, DEVICE_ID);
	}

	@Test
	void testDeleteDevice_invalidatesSiteCacheUsingPreDeletionSiteId() {
		Device device = createTestDevice();
		when(mockRepository.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.of(device));
		when(mockRepository.getDevicesBySiteId(CUSTOMER_ID, SITE_ID))
				.thenReturn(List.of(device))
				.thenReturn(List.of());

		assertEquals(
				1, cachingComponent.getDevicesBySiteId(CUSTOMER_ID, SITE_ID).size());
		cachingComponent.deleteDevice(DEVICE_ID, CUSTOMER_ID);

		assertTrue(cachingComponent.getDevicesBySiteId(CUSTOMER_ID, SITE_ID).isEmpty());
	}

	@Test
	void testDeleteDevice_whenDeviceMissing_doesNothing() {
		when(mockRepository.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.empty());

		cachingComponent.deleteDevice(DEVICE_ID, CUSTOMER_ID);

		verify(mockRepository, never()).delete(any(Device.class));
		verify(mockDeviceUpdateComponent, never()).delete(anyString());
	}

	@Test
	void testDeleteDevicesByCustomerId_deletesEachAndClearsCaches() {
		Device first = createTestDevice();
		Device second = new Device("device-456", CUSTOMER_ID, "Second Device");
		second.setSiteId(SITE_ID);

		when(mockRepository.getDevicesForCustomerId(CUSTOMER_ID)).thenReturn(Arrays.asList(first, second));
		when(mockRepository.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.of(first));
		when(mockRepository.findDeviceById("device-456", CUSTOMER_ID)).thenReturn(Optional.of(second));

		cachingComponent.deleteDevicesByCustomerId(CUSTOMER_ID);

		verify(mockRepository).delete(first);
		verify(mockRepository).delete(second);
	}

	@Test
	void testInvalidateAllForCustomer_forcesRefetch() {
		Device device = createTestDevice();
		when(mockRepository.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.of(device));

		cachingComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID);
		cachingComponent.invalidateAllForCustomer(CUSTOMER_ID);
		cachingComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID);

		verify(mockRepository, times(2)).findDeviceById(DEVICE_ID, CUSTOMER_ID);
	}

	private Device createTestDevice() {
		Device device = new Device(DEVICE_ID, CUSTOMER_ID, "Test Device");
		device.setName("Pretty Test Device");
		device.setSiteId(SITE_ID);
		device.setSite("Test Site");
		return device;
	}
}
