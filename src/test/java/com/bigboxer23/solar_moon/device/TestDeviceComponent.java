package com.bigboxer23.solar_moon.device;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.TestConstants;
import com.bigboxer23.solar_moon.TestUtils;
import com.bigboxer23.solar_moon.data.Device;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** */
// @ActiveProfiles("test")
public class TestDeviceComponent implements IComponentRegistry, TestConstants {
	@Test
	public void testFindDeviceByDeviceKey() {
		assertNull(deviceComponent.findDeviceByDeviceKey(null));
		assertNull(deviceComponent.findDeviceByDeviceKey(""));
		assertNull(deviceComponent.findDeviceByDeviceKey("1234"));
		Device device = TestUtils.getDevice();
		device.setDeviceKey("2459786f-74c6-42e0-bc37-a501cb87297a");
		deviceComponent.updateDevice(device);
		assertNotNull(
				deviceComponent.findDeviceByDeviceKey(TestUtils.getDevice().getDeviceKey()));
	}

	@Test
	public void testGetDevicesForCustomerId() {
		assertEquals(0, deviceComponent.getDevicesForCustomerId(null).size());
		assertEquals(0, deviceComponent.getDevicesForCustomerId("").size());
		assertEquals(6, deviceComponent.getDevicesForCustomerId(CUSTOMER_ID).size());
		assertEquals(0, deviceComponent.getDevicesForCustomerId("tacoClient").size());
	}

	@Test
	public void testGetDevice() {
		assertNotNull(deviceComponent.getDevice(TestUtils.getDevice().getId(), CUSTOMER_ID));
		assertNull(deviceComponent.getDevice(null, null));
		assertNull(deviceComponent.getDevice("", null));
		assertNull(deviceComponent.getDevice(null, ""));
		assertNull(deviceComponent.getDevice("", ""));
		assertNull(deviceComponent.getDevice("blah", CUSTOMER_ID));
		assertNull(deviceComponent.getDevice(TestUtils.getDevice().getId(), "blah"));
	}

	@Test
	public void testGetDevicesBySite() {
		assertTrue(deviceComponent.getDevicesBySite(null, null).isEmpty());
		assertTrue(deviceComponent.getDevicesBySite("", null).isEmpty());
		assertTrue(deviceComponent.getDevicesBySite(null, "").isEmpty());
		assertTrue(deviceComponent.getDevicesBySite("", "").isEmpty());
		assertTrue(deviceComponent.getDevicesBySite(CUSTOMER_ID, "blah").isEmpty());
		assertEquals(6, deviceComponent.getDevicesBySite(CUSTOMER_ID, SITE).size());
	}

	@Test
	public void testGetDevicesVirtual() {
		assertFalse(deviceComponent.getDevices(false).stream()
				.filter(device -> CUSTOMER_ID.equals(device.getClientId()))
				.toList()
				.isEmpty());
		assertEquals(
				5,
				deviceComponent.getDevices(false).stream()
						.filter(device -> CUSTOMER_ID.equals(device.getClientId()))
						.toList()
						.size());
		assertEquals(
				1,
				deviceComponent.getDevices(true).stream()
						.filter(device -> CUSTOMER_ID.equals(device.getClientId()))
						.toList()
						.size());
	}

	@Test
	public void testSiteDelete() {
		Device testSite = deviceComponent.getDevicesBySite(TestConstants.CUSTOMER_ID, TestConstants.SITE).stream()
				.filter(Device::isVirtual)
				.findFirst()
				.orElse(null);
		assertNotNull(testSite);
		assertTrue(deviceComponent
				.getDevicesBySite(testSite.getClientId(), DeviceComponent.NO_SITE)
				.isEmpty());
		deviceComponent.deleteDevice(testSite.getId(), testSite.getClientId());
		assertTrue(deviceComponent
				.getDevicesBySite(testSite.getClientId(), testSite.getSite())
				.isEmpty());
		List<Device> updatedDevices = deviceComponent.getDevicesBySite(testSite.getClientId(), DeviceComponent.NO_SITE);
		assertFalse(updatedDevices.isEmpty());
		updatedDevices.forEach(device -> assertEquals(device.getSite(), DeviceComponent.NO_SITE));
	}

	@Test
	public void testSiteUpdate() {
		Device testSite = deviceComponent.getDevicesBySite(TestConstants.CUSTOMER_ID, TestConstants.SITE).stream()
				.filter(Device::isVirtual)
				.findFirst()
				.orElse(null);
		assertNotNull(testSite);
		List<Device> originalDevices = deviceComponent.getDevicesBySite(testSite.getClientId(), testSite.getSite());
		testSite.setName(TestConstants.SITE + 2);
		testSite.setSite(TestConstants.SITE + 2);
		deviceComponent.updateDevice(testSite);
		List<Device> updatedDevices = deviceComponent.getDevicesBySite(testSite.getClientId(), testSite.getSite());
		assertEquals(originalDevices.size(), updatedDevices.size());
		assertFalse(updatedDevices.isEmpty());
		updatedDevices.forEach(device -> assertEquals(device.getSite(), testSite.getSite()));
	}

	@Test
	public void testSubscriptionLimit() {
		Device testDevice = new Device();
		for (int ai = 0; ai < 9; ai++) {
			testDevice.setId("test-" + ai);
			testDevice.setName(deviceName + ai);
			deviceComponent.addDevice(testDevice);
		}
		if (deviceComponent.addDevice(testDevice)) {
			fail();
		}
		deviceComponent.deleteDevicesByCustomerId(TestConstants.CUSTOMER_ID);
		subscriptionComponent.updateSubscription(TestConstants.CUSTOMER_ID, 0);
		if (deviceComponent.addDevice(testDevice)) {
			fail();
		}
	}

	@Test
	public void testFindDeviceByDeviceName() {
		Optional<Device> device =
				deviceComponent.findDeviceByDeviceName(TestConstants.CUSTOMER_ID, TestConstants.deviceName + 0);
		assertTrue(device.isPresent());
		assertFalse(deviceComponent
				.findDeviceByDeviceName(TestConstants.CUSTOMER_ID, "pretty" + TestConstants.deviceName + 0)
				.isPresent());

		assertFalse(deviceComponent
				.findDeviceByDeviceName(TestConstants.CUSTOMER_ID, TestConstants.deviceName)
				.isPresent());
		assertFalse(deviceComponent
				.findDeviceByDeviceName(TestConstants.CUSTOMER_ID + 1, TestConstants.deviceName + 0)
				.isPresent());
		assertFalse(deviceComponent
				.findDeviceByDeviceName(TestConstants.CUSTOMER_ID, device.get().getId())
				.isPresent());
	}

	@Test
	public void findDeviceByName() {
		Optional<Device> device =
				deviceComponent.findDeviceByName(TestConstants.CUSTOMER_ID, "pretty" + TestConstants.deviceName + 0);
		assertTrue(device.isPresent());
		assertFalse(deviceComponent
				.findDeviceByName(TestConstants.CUSTOMER_ID, TestConstants.deviceName + 0)
				.isPresent());

		assertFalse(deviceComponent
				.findDeviceByName(TestConstants.CUSTOMER_ID, "pretty" + TestConstants.deviceName)
				.isPresent());
		assertFalse(deviceComponent
				.findDeviceByName(TestConstants.CUSTOMER_ID + 1, "pretty" + TestConstants.deviceName + 0)
				.isPresent());
		assertFalse(deviceComponent
				.findDeviceByName(TestConstants.CUSTOMER_ID, device.get().getId())
				.isPresent());
	}

	@Test
	public void findDeviceById() {
		assertTrue(deviceComponent.findDeviceById(TestUtils.getDevice().getId()).isPresent());
	}

	@Test
	public void deviceLocationUpdate() {
		Device device = TestUtils.getSite();
		assertEquals(-1, device.getLatitude());
		assertEquals(-1, device.getLongitude());
		device.setCity("Minneapolis");
		deviceComponent.updateDevice(device);
		assertEquals(-1, TestUtils.getSite().getLatitude());
		device.setState("MN");
		deviceComponent.updateDevice(device);
		assertEquals(-1, TestUtils.getSite().getLatitude());
		device.setCountry("USA");
		deviceComponent.updateDevice(device);
		assertEquals(44.97902, TestUtils.getSite().getLatitude());

		device = TestUtils.getDevice();
		device.setCity("Minneapolis");
		device.setState("MN");
		device.setCountry("USA");
		deviceComponent.updateDevice(device);
		assertEquals(-1, TestUtils.getDevice().getLatitude());
	}

	@BeforeEach
	protected void setupTestDevice() {
		TestUtils.setupSite();
	}
}
