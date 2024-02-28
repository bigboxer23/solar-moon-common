package com.bigboxer23.solar_moon.device;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.TestConstants;
import com.bigboxer23.solar_moon.TestUtils;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.util.TokenGenerator;
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
	public void testGetDevicesBySiteId() {
		assertTrue(deviceComponent.getDevicesBySiteId(null, null).isEmpty());
		assertTrue(deviceComponent.getDevicesBySiteId("", null).isEmpty());
		assertTrue(deviceComponent.getDevicesBySiteId(null, "").isEmpty());
		assertTrue(deviceComponent.getDevicesBySiteId("", "").isEmpty());
		assertTrue(deviceComponent.getDevicesBySiteId(CUSTOMER_ID, "blah").isEmpty());
		assertTrue(deviceComponent.getDevicesBySiteId(CUSTOMER_ID, "").isEmpty());
		assertTrue(deviceComponent.getDevicesBySiteId(CUSTOMER_ID, null).isEmpty());
		assertEquals(
				6,
				deviceComponent
						.getDevicesBySiteId(CUSTOMER_ID, TestUtils.getDevice().getSiteId())
						.size());
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
	public void getSites() {
		assertEquals(
				1,
				deviceComponent.getSites().stream()
						.filter(device -> CUSTOMER_ID.equals(device.getClientId()))
						.toList()
						.size());
	}

	@Test
	public void addDevice() {
		Device device = TestUtils.getDevice();
		assertNull(deviceComponent.addDevice(device));
		assertNull(deviceComponent.addDevice(
				new Device(TokenGenerator.generateNewToken(), device.getClientId(), device.getDeviceName())));

		// Test new site gets siteId automatically stamped
		Device siteDevice = new Device(
				TokenGenerator.generateNewToken(), device.getClientId(), device.getDeviceName() + "siteTest");
		siteDevice.setSite("temporary");
		assertNotNull(deviceComponent.addDevice(siteDevice).getSiteId());
	}

	@Test
	public void updateDevice() {
		Device device = TestUtils.getDevice();
		Device device2 =
				new Device(TokenGenerator.generateNewToken(), device.getClientId(), device.getDeviceName() + 22);
		assertNotNull(deviceComponent.addDevice(device2));
		device.setDeviceName("temp");
		device.setName("temp");
		assertTrue(deviceComponent.updateDevice(device).isPresent());

		device2.setDeviceName(device.getDeviceName());
		device2.setDeviceName(device.getName());
		assertFalse(deviceComponent.updateDevice(device2).isPresent());
	}

	@Test
	public void testSiteDelete() {
		Device testSite =
				deviceComponent
						.getDevicesBySiteId(
								TestConstants.CUSTOMER_ID, TestUtils.getSite().getSiteId())
						.stream()
						.filter(Device::isDeviceSite)
						.findFirst()
						.orElse(null);
		assertNotNull(testSite);
		assertTrue(deviceComponent
				.getDevicesBySiteId(testSite.getClientId(), DeviceComponent.NO_SITE)
				.isEmpty());
		deviceComponent.deleteDevice(testSite.getId(), testSite.getClientId());
		assertTrue(deviceComponent
				.getDevicesBySiteId(testSite.getClientId(), testSite.getSiteId())
				.isEmpty());
		List<Device> updatedDevices =
				deviceComponent.getDevicesBySiteId(testSite.getClientId(), DeviceComponent.NO_SITE);
		assertFalse(updatedDevices.isEmpty());
		updatedDevices.forEach(device -> {
			assertEquals(device.getSite(), DeviceComponent.NO_SITE);
			assertEquals(device.getSiteId(), DeviceComponent.NO_SITE);
		});
	}

	@Test
	public void testSiteUpdate() {
		Device testSite =
				deviceComponent
						.getDevicesBySiteId(
								TestConstants.CUSTOMER_ID, TestUtils.getSite().getSiteId())
						.stream()
						.filter(Device::isDeviceSite)
						.findFirst()
						.orElse(null);
		assertNotNull(testSite);
		List<Device> originalDevices = deviceComponent.getDevicesBySiteId(testSite.getClientId(), testSite.getSiteId());
		testSite.setName(TestConstants.SITE + 2);
		testSite.setSite(TestConstants.SITE + 2);
		deviceComponent.updateDevice(testSite);
		List<Device> updatedDevices = deviceComponent.getDevicesBySiteId(testSite.getClientId(), testSite.getSiteId());
		assertEquals(originalDevices.size(), updatedDevices.size());
		assertFalse(updatedDevices.isEmpty());
		updatedDevices.forEach(device -> assertEquals(device.getSite(), testSite.getSite()));
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
		Device site = TestUtils.getSite();
		assertEquals(-1, site.getLatitude());
		assertEquals(-1, site.getLongitude());
		site.setCity("Minneapolis");
		deviceComponent.updateDevice(site).ifPresent(s -> assertEquals(-1, s.getLatitude()));
		site.setState("MN");
		deviceComponent.updateDevice(site).ifPresent(s -> assertEquals(-1, s.getLatitude()));
		site.setCountry("USA");
		deviceComponent.updateDevice(site).ifPresent(s -> assertEquals(44.97902, s.getLatitude()));

		site = TestUtils.getDevice();
		site.setCity("Minneapolis");
		site.setState("MN");
		site.setCountry("USA");
		deviceComponent.updateDevice(site).ifPresent(d -> assertEquals(-1, d.getLatitude()));
	}

	@BeforeEach
	protected void setupTestDevice() {
		TestUtils.setupSite();
	}
}
