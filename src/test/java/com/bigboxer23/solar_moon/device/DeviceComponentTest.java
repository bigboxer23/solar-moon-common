package com.bigboxer23.solar_moon.device;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.alarm.AlarmComponent;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.Subscription;
import com.bigboxer23.solar_moon.location.LocationComponent;
import com.bigboxer23.solar_moon.subscription.SubscriptionComponent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.location.model.Place;
import software.amazon.awssdk.services.location.model.PlaceGeometry;
import software.amazon.awssdk.services.location.model.SearchForTextResult;

@ExtendWith(MockitoExtension.class)
public class DeviceComponentTest {

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

	private TestableDeviceComponent deviceComponent;

	private static final String CUSTOMER_ID = "test-customer-123";
	private static final String DEVICE_ID = "device-123";
	private static final String DEVICE_NAME = "Test Device";

	private class TestableDeviceComponent extends DeviceComponent {

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
		deviceComponent = new TestableDeviceComponent();
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

	@Test
	void testAddDevice_whenSubscriptionLimitReached_returnsNullWithoutPersisting() {
		Device device = createTestDevice();
		when(mockSubscriptionComponent.canAddAnotherDevice(CUSTOMER_ID)).thenReturn(false);
		when(mockSubscriptionComponent.getSubscription(CUSTOMER_ID)).thenReturn(Optional.empty());
		when(mockRepository.getDevicesForCustomerId(CUSTOMER_ID)).thenReturn(Collections.emptyList());

		assertNull(deviceComponent.addDevice(device));

		verify(mockRepository, never()).add(any(Device.class));
	}

	@Test
	void testAddDevice_whenSubscriptionAvailable_reportsLicensedDeviceCount() {
		Device device = createTestDevice();
		Subscription subscription = new Subscription(CUSTOMER_ID, 2, 0);
		when(mockSubscriptionComponent.canAddAnotherDevice(CUSTOMER_ID)).thenReturn(false);
		when(mockSubscriptionComponent.getSubscription(CUSTOMER_ID)).thenReturn(Optional.of(subscription));
		when(mockRepository.getDevicesForCustomerId(CUSTOMER_ID)).thenReturn(Collections.emptyList());

		assertNull(deviceComponent.addDevice(device));

		verify(mockSubscriptionComponent).getSubscription(CUSTOMER_ID);
		verify(mockRepository, never()).add(any(Device.class));
	}

	@Test
	void testAddDevice_whenIdAlreadyExists_returnsNullWithoutPersisting() {
		Device device = createTestDevice();
		when(mockSubscriptionComponent.canAddAnotherDevice(CUSTOMER_ID)).thenReturn(true);
		when(mockRepository.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.of(device));

		assertNull(deviceComponent.addDevice(device));

		verify(mockRepository, never()).add(any(Device.class));
	}

	@Test
	void testAddDevice_whenDeviceNameAlreadyExists_returnsNullWithoutPersisting() {
		Device device = createTestDevice();
		when(mockSubscriptionComponent.canAddAnotherDevice(CUSTOMER_ID)).thenReturn(true);
		when(mockRepository.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.empty());
		when(mockRepository.findDeviceByDeviceName(CUSTOMER_ID, DEVICE_NAME))
				.thenReturn(Optional.of(createTestDevice("other-id", CUSTOMER_ID, DEVICE_NAME)));

		assertNull(deviceComponent.addDevice(device));

		verify(mockRepository, never()).add(any(Device.class));
	}

	@Test
	void testAddDevice_whenSiteNameMatchesDeviceName_assignsOwnIdAsSiteId() {
		Device device = createTestDevice();
		device.setIsSite("1");
		device.setSite(DEVICE_NAME);
		device.setSiteId(null);
		allowAdd(device);

		deviceComponent.addDevice(device);

		assertEquals(DEVICE_ID, device.getSiteId());
		verify(mockRepository).add(device);
	}

	@Test
	void testAddDevice_withNullSite_persistsWithoutAssigningSiteId() {
		Device device = createTestDevice();
		device.setIsSite("1");
		device.setSite(null);
		device.setSiteId(null);
		allowAdd(device);

		deviceComponent.addDevice(device);

		assertNull(device.getSiteId());
		verify(mockRepository).add(device);
	}

	@Test
	void testAddDevice_withNoSiteId_clearsLatLong() {
		Device device = createTestDevice();
		device.setSiteId(DeviceComponent.NO_SITE);
		allowAdd(device);

		deviceComponent.addDevice(device);

		assertEquals(-1, device.getLatitude());
		assertEquals(-1, device.getLongitude());
	}

	@Test
	void testAddDevice_withNullSiteId_clearsLatLong() {
		Device device = createTestDevice();
		device.setSiteId(null);
		device.setSite(null);
		allowAdd(device);

		deviceComponent.addDevice(device);

		assertEquals(-1, device.getLatitude());
		assertEquals(-1, device.getLongitude());
	}

	@Test
	void testAddDevice_newDeviceWithSite_inheritsLatLongFromSite() {
		Device device = createTestDevice();
		device.setLatitude(-1);
		device.setLongitude(-1);
		Device site = createSiteDevice("site-123", CUSTOMER_ID, "Site");
		site.setLatitude(12.5);
		site.setLongitude(-71.25);
		allowAdd(device);
		when(mockRepository.findDeviceById("site-123", CUSTOMER_ID)).thenReturn(Optional.of(site));

		deviceComponent.addDevice(device);

		assertEquals(12.5, device.getLatitude());
		assertEquals(-71.25, device.getLongitude());
	}

	@Test
	void testAddDevice_siteWithoutCoordinates_geocodesAndPropagatesToChildren() {
		Device site = createSiteDevice(DEVICE_ID, CUSTOMER_ID, DEVICE_NAME);
		site.setLatitude(-1);
		site.setLongitude(-1);
		site.setCity("Minneapolis");
		site.setState("MN");
		site.setCountry("USA");
		Device child = createTestDevice("child-1", CUSTOMER_ID, "Child");
		child.setSiteId(DEVICE_ID);

		allowAdd(site);
		when(mockLocationComponent.getLatLongFromText("Minneapolis", "MN", "USA"))
				.thenReturn(Optional.of(searchResult(-93.26, 44.97)));
		when(mockRepository.getDevicesBySiteId(CUSTOMER_ID, DEVICE_ID)).thenReturn(List.of(child));
		when(mockRepository.findDeviceByDeviceName(CUSTOMER_ID, "Child")).thenReturn(Optional.of(child));
		when(mockRepository.findDeviceById("child-1", CUSTOMER_ID)).thenReturn(Optional.of(child));

		deviceComponent.addDevice(site);

		assertEquals(44.97, site.getLatitude());
		assertEquals(-93.26, site.getLongitude());
		assertEquals(44.97, child.getLatitude());
		assertEquals(-93.26, child.getLongitude());
		verify(mockRepository).update(child);
	}

	@Test
	void testAddDevice_siteGeocodeMiss_leavesCoordinatesUnset() {
		Device site = createSiteDevice(DEVICE_ID, CUSTOMER_ID, DEVICE_NAME);
		site.setLatitude(-1);
		site.setLongitude(-1);
		site.setCity("Nowhere");
		site.setState("XX");
		site.setCountry("ZZ");
		allowAdd(site);
		when(mockLocationComponent.getLatLongFromText("Nowhere", "XX", "ZZ")).thenReturn(Optional.empty());

		deviceComponent.addDevice(site);

		assertEquals(-1, site.getLatitude());
		assertEquals(-1, site.getLongitude());
		verify(mockRepository).add(site);
	}

	@Test
	void testUpdateDevice_whenDeviceNameBelongsToAnotherDevice_returnsEmpty() {
		Device device = createTestDevice();
		when(mockRepository.findDeviceByDeviceName(CUSTOMER_ID, DEVICE_NAME))
				.thenReturn(Optional.of(createTestDevice("a-different-id", CUSTOMER_ID, DEVICE_NAME)));

		assertTrue(deviceComponent.updateDevice(device).isEmpty());

		verify(mockRepository, never()).update(any(Device.class));
	}

	@Test
	void testUpdateDevice_renamingSite_propagatesNewNameToChildren() {
		Device site = createSiteDevice(DEVICE_ID, CUSTOMER_ID, DEVICE_NAME);
		site.setName("New Site Name");
		Device persistedSite = createSiteDevice(DEVICE_ID, CUSTOMER_ID, DEVICE_NAME);
		persistedSite.setName("Old Site Name");
		Device child = createTestDevice("child-1", CUSTOMER_ID, "Child");
		child.setSiteId(DEVICE_ID);
		child.setSite("Old Site Name");

		when(mockRepository.findDeviceByDeviceName(CUSTOMER_ID, DEVICE_NAME)).thenReturn(Optional.of(site));
		when(mockRepository.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.of(persistedSite));
		when(mockRepository.getDevicesBySiteId(CUSTOMER_ID, DEVICE_ID)).thenReturn(List.of(child));
		when(mockRepository.findDeviceByDeviceName(CUSTOMER_ID, "Child")).thenReturn(Optional.of(child));
		when(mockRepository.findDeviceById("child-1", CUSTOMER_ID)).thenReturn(Optional.of(child));

		deviceComponent.updateDevice(site);

		assertEquals("New Site Name", child.getSite());
		verify(mockRepository).update(child);
		verify(mockRepository).update(site);
	}

	@Test
	void testUpdateDevice_siteNameUnchanged_leavesChildrenAlone() {
		Device site = createSiteDevice(DEVICE_ID, CUSTOMER_ID, DEVICE_NAME);
		site.setName("Same Name");
		Device persistedSite = createSiteDevice(DEVICE_ID, CUSTOMER_ID, DEVICE_NAME);
		persistedSite.setName("Same Name");

		when(mockRepository.findDeviceByDeviceName(CUSTOMER_ID, DEVICE_NAME)).thenReturn(Optional.of(site));
		when(mockRepository.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.of(persistedSite));

		deviceComponent.updateDevice(site);

		verify(mockRepository, never()).getDevicesBySiteId(CUSTOMER_ID, DEVICE_ID);
		verify(mockRepository).update(site);
	}

	@Test
	void testUpdateDevice_siteNotYetPersisted_updatesWithoutRenamingChildren() {
		Device site = createSiteDevice(DEVICE_ID, CUSTOMER_ID, DEVICE_NAME);
		when(mockRepository.findDeviceByDeviceName(CUSTOMER_ID, DEVICE_NAME)).thenReturn(Optional.empty());
		when(mockRepository.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.empty());

		deviceComponent.updateDevice(site);

		verify(mockRepository, never()).getDevicesBySiteId(anyString(), anyString());
		verify(mockRepository).update(site);
	}

	@Test
	void testDeleteDevice_whenDeviceMissing_doesNothing() {
		when(mockRepository.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.empty());

		deviceComponent.deleteDevice(DEVICE_ID, CUSTOMER_ID);

		verify(mockRepository, never()).delete(any(Device.class));
		verify(mockDeviceUpdateComponent, never()).delete(anyString());
		verify(mockAlarmComponent, never()).deleteAlarmByDeviceId(anyString(), anyString());
	}

	@Test
	void testDeleteDevice_cascadesToDeviceUpdatesAndAlarms() {
		Device device = createTestDevice();
		when(mockRepository.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.of(device));

		deviceComponent.deleteDevice(DEVICE_ID, CUSTOMER_ID);

		verify(mockRepository).delete(device);
		verify(mockDeviceUpdateComponent).delete(DEVICE_ID);
		verify(mockAlarmComponent).deleteAlarmByDeviceId(CUSTOMER_ID, DEVICE_ID);
	}

	@Test
	void testDeleteDevice_site_reassignsChildrenToNoSite() {
		Device site = createSiteDevice(DEVICE_ID, CUSTOMER_ID, DEVICE_NAME);
		Device child = createTestDevice("child-1", CUSTOMER_ID, "Child");
		child.setSiteId(DEVICE_ID);

		when(mockRepository.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.of(site));
		when(mockRepository.getDevicesBySiteId(CUSTOMER_ID, DEVICE_ID)).thenReturn(List.of(child));
		when(mockRepository.findDeviceByDeviceName(CUSTOMER_ID, "Child")).thenReturn(Optional.of(child));
		when(mockRepository.findDeviceById("child-1", CUSTOMER_ID)).thenReturn(Optional.of(child));

		deviceComponent.deleteDevice(DEVICE_ID, CUSTOMER_ID);

		assertEquals(DeviceComponent.NO_SITE, child.getSite());
		assertEquals(DeviceComponent.NO_SITE, child.getSiteId());
		assertEquals(-1, child.getLatitude());
		assertEquals(-1, child.getLongitude());
		verify(mockRepository).update(child);
		verify(mockRepository).delete(site);
	}

	@Test
	void testGetDevicesBySite_delegatesToRepository() {
		List<Device> expectedDevices = Arrays.asList(createTestDevice());
		when(mockRepository.getDevicesBySite(CUSTOMER_ID, "Test Site")).thenReturn(expectedDevices);

		List<Device> result = deviceComponent.getDevicesBySite(CUSTOMER_ID, "Test Site");

		assertEquals(expectedDevices, result);
		verify(mockRepository).getDevicesBySite(CUSTOMER_ID, "Test Site");
	}

	@Test
	void testIsValidUpdate_withBlankClientId_returnsFalse() {
		Device device = createTestDevice();
		device.setClientId("");

		assertFalse(deviceComponent.isValidUpdate(device));
	}

	@Test
	void testIsValidUpdate_withBlankDeviceName_returnsFalse() {
		Device device = createTestDevice();
		device.setDeviceName("");

		assertFalse(deviceComponent.isValidUpdate(device));
	}

	@Test
	void testIsValidUpdate_withBlankId_returnsFalse() {
		Device device = createTestDevice();
		device.setId("");

		assertFalse(deviceComponent.isValidUpdate(device));
	}

	private void allowAdd(Device device) {
		when(mockSubscriptionComponent.canAddAnotherDevice(device.getClientId()))
				.thenReturn(true);
		when(mockRepository.findDeviceById(device.getId(), device.getClientId()))
				.thenReturn(Optional.empty());
		when(mockRepository.findDeviceByDeviceName(device.getClientId(), device.getDeviceName()))
				.thenReturn(Optional.empty());
	}

	private SearchForTextResult searchResult(double longitude, double latitude) {
		return SearchForTextResult.builder()
				.place(Place.builder()
						.geometry(PlaceGeometry.builder()
								.point(longitude, latitude)
								.build())
						.build())
				.build();
	}

	private Device createSiteDevice(String id, String clientId, String deviceName) {
		Device device = createTestDevice(id, clientId, deviceName);
		device.setIsSite("1");
		device.setSiteId(id);
		device.setSite(deviceName);
		return device;
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
