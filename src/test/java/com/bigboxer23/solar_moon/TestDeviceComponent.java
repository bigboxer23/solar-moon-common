package com.bigboxer23.solar_moon;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.data.Device;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** */
// @ActiveProfiles("test")
public class TestDeviceComponent implements IComponentRegistry {

	protected static final String deviceKey = "2459786f-74c6-42e0-bc37-a501cb87297a";
	protected static final String deviceName = "testDevice";

	protected static final String clientId = "0badd0c2-450b-4204-80d5-c7c77fc13500";

	protected static final String deviceId = "edc76a9b-e451-4592-996e-0e54410bae5e";

	protected static final String SITE = "testSite";

	private Device testDevice = new Device();

	@Test
	public void testFindDeviceByDeviceKey() {
		assertNull(deviceComponent.findDeviceByDeviceKey(null));
		assertNull(deviceComponent.findDeviceByDeviceKey(""));
		assertNull(deviceComponent.findDeviceByDeviceKey("1234"));
		assertNotNull(deviceComponent.findDeviceByDeviceKey(deviceKey));
	}

	@Test
	public void testGetDevices() {
		assertEquals(0, deviceComponent.getDevices(null).size());
		assertEquals(0, deviceComponent.getDevices("").size());
		assertEquals(1, deviceComponent.getDevices(clientId).size());
		assertEquals(0, deviceComponent.getDevices("tacoClient").size());
	}

	@Test
	public void testGetDevice() {
		assertNotNull(deviceComponent.getDevice(deviceId, clientId));
		assertNull(deviceComponent.getDevice(null, null));
		assertNull(deviceComponent.getDevice("", null));
		assertNull(deviceComponent.getDevice(null, ""));
		assertNull(deviceComponent.getDevice("", ""));
		assertNull(deviceComponent.getDevice("blah", clientId));
		assertNull(deviceComponent.getDevice(deviceId, "blah"));
	}

	@Test
	public void testGetDevicesBySite() {
		assertTrue(deviceComponent.getDevicesBySite(null, null).isEmpty());
		assertTrue(deviceComponent.getDevicesBySite("", null).isEmpty());
		assertTrue(deviceComponent.getDevicesBySite(null, "").isEmpty());
		assertTrue(deviceComponent.getDevicesBySite("", "").isEmpty());
		assertTrue(deviceComponent.getDevicesBySite(clientId, "blah").isEmpty());
		assertEquals(1, deviceComponent.getDevicesBySite(clientId, SITE).size());
	}

	@Test
	public void testGetDevicesVirtual() {
		assertFalse(deviceComponent.getDevices(false).stream()
				.filter(device -> clientId.equals(device.getClientId()))
				.toList()
				.isEmpty());
		assertEquals(
				1,
				deviceComponent.getDevices(false).stream()
						.filter(device -> clientId.equals(device.getClientId()))
						.toList()
						.size());
		assertTrue(deviceComponent.getDevices(true).stream()
				.filter(device -> clientId.equals(device.getClientId()))
				.toList()
				.isEmpty());
		testDevice = new Device();
		setupTestDevice(true);
		assertTrue(deviceComponent.getDevices(false).stream()
				.filter(device -> clientId.equals(device.getClientId()))
				.toList()
				.isEmpty());
		assertFalse(deviceComponent.getDevices(true).stream()
				.filter(device -> clientId.equals(device.getClientId()))
				.toList()
				.isEmpty());
		assertEquals(
				1,
				deviceComponent.getDevices(true).stream()
						.filter(device -> clientId.equals(device.getClientId()))
						.toList()
						.size());
		testDevice = new Device();
	}

	@Test
	public void testSiteDelete() {
		TestUtils.setupSite();
		Device testSite =
				deviceComponent.getDevicesBySite(TestDeviceComponent.clientId, TestDeviceComponent.SITE).stream()
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
		TestUtils.setupSite();
		Device testSite =
				deviceComponent.getDevicesBySite(TestDeviceComponent.clientId, TestDeviceComponent.SITE).stream()
						.filter(Device::isVirtual)
						.findFirst()
						.orElse(null);
		assertNotNull(testSite);
		List<Device> originalDevices = deviceComponent.getDevicesBySite(testSite.getClientId(), testSite.getSite());
		testSite.setName(TestDeviceComponent.SITE + 2);
		testSite.setSite(TestDeviceComponent.SITE + 2);
		deviceComponent.updateDevice(testSite);
		List<Device> updatedDevices = deviceComponent.getDevicesBySite(testSite.getClientId(), testSite.getSite());
		assertEquals(originalDevices.size(), updatedDevices.size());
		assertFalse(updatedDevices.isEmpty());
		updatedDevices.forEach(device -> assertEquals(device.getSite(), testSite.getSite()));
	}

	@Test
	public void testSubscriptionLimit() {
		for (int ai = 0; ai < 9; ai++) {
			testDevice.setId(deviceId + ai);
			testDevice.setName(deviceName + ai);
			deviceComponent.addDevice(testDevice);
		}
		if (deviceComponent.addDevice(testDevice)) {
			fail();
		}
		deviceComponent.deleteDevicesByCustomerId(TestDeviceComponent.clientId);
		subscriptionComponent.updateSubscription(TestDeviceComponent.clientId, 0);
		if (deviceComponent.addDevice(testDevice)) {
			fail();
		}
	}

	@Test
	public void testFindDeviceByName() {
		TestUtils.setupSite();
		Optional<Device> device =
				deviceComponent.findDeviceByName(TestDeviceComponent.clientId, TestDeviceComponent.deviceName + 0);
		assertTrue(device.isPresent());
		assertFalse(deviceComponent
				.findDeviceByName(TestDeviceComponent.clientId, TestDeviceComponent.deviceName)
				.isPresent());
		assertFalse(deviceComponent
				.findDeviceByName(TestDeviceComponent.clientId + 1, TestDeviceComponent.deviceName + 0)
				.isPresent());
		assertFalse(deviceComponent
				.findDeviceByName(TestDeviceComponent.clientId, device.get().getId())
				.isPresent());
	}

	@BeforeEach
	protected void setupTestDevice() {
		deviceComponent.deleteDevicesByCustomerId(TestDeviceComponent.clientId);
		setupTestDevice(false);
	}

	protected void setupTestDevice(boolean isVirtual) {
		subscriptionComponent.updateSubscription(TestDeviceComponent.clientId, 1);
		testDevice.setId(deviceId);
		testDevice.setName(deviceName);
		testDevice.setDeviceName(deviceName);
		testDevice.setClientId(clientId);
		testDevice.setDeviceKey(deviceKey);
		testDevice.setSite(SITE);
		if (isVirtual) {
			testDevice.setVirtual(isVirtual); // only set if true so we can test the initial state is properly
			// set
		}
		Device dbDevice = deviceComponent.getDevice(testDevice.getId(), testDevice.getClientId());
		if (dbDevice != null) {
			deviceComponent.deleteDevice(testDevice.getId(), testDevice.getClientId());
		}
		deviceComponent.addDevice(testDevice);
	}
}
