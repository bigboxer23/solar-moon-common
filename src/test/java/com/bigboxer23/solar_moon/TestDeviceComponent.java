package com.bigboxer23.solar_moon;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.data.Device;
import java.util.List;

import com.bigboxer23.solar_moon.open_search.OpenSearchComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

/** */
// @ActiveProfiles("test")
public class TestDeviceComponent {

	protected static final String deviceKey = "2459786f-74c6-42e0-bc37-a501cb87297a";
	protected static final String deviceName = "testDevice";

	protected static final String clientId = "0badd0c2-450b-4204-80d5-c7c77fc13500";

	protected static final String deviceId = "edc76a9b-e451-4592-996e-0e54410bae5e";

	protected static final String SITE = "testSite";

	private DeviceComponent component = new DeviceComponent();

	private OpenSearchComponent OSComponent = new OpenSearchComponent();

	private Device testDevice = new Device();

	@Test
	public void testFindDeviceByDeviceKey() {
		assertNull(component.findDeviceByDeviceKey(null));
		assertNull(component.findDeviceByDeviceKey(""));
		assertNull(component.findDeviceByDeviceKey("1234"));
		assertNotNull(component.findDeviceByDeviceKey(deviceKey));
	}

	@Test
	public void testGetDevices() {
		assertEquals(0, component.getDevices(null).size());
		assertEquals(0, component.getDevices("").size());
		assertEquals(1, component.getDevices(clientId).size());
		assertEquals(0, component.getDevices("tacoClient").size());
	}

	@Test
	public void testGetDevice() {
		assertNotNull(component.getDevice(deviceId, clientId));
		assertNull(component.getDevice(null, null));
		assertNull(component.getDevice("", null));
		assertNull(component.getDevice(null, ""));
		assertNull(component.getDevice("", ""));
		assertNull(component.getDevice("blah", clientId));
		assertNull(component.getDevice(deviceId, "blah"));
	}

	@Test
	public void testGetDevicesBySite() {
		assertTrue(component.getDevicesBySite(null, null).isEmpty());
		assertTrue(component.getDevicesBySite("", null).isEmpty());
		assertTrue(component.getDevicesBySite(null, "").isEmpty());
		assertTrue(component.getDevicesBySite("", "").isEmpty());
		assertTrue(component.getDevicesBySite(clientId, "blah").isEmpty());
		assertEquals(1, component.getDevicesBySite(clientId, SITE).size());
	}

	@Test
	public void testGetDevicesVirtual() {
		assertFalse(component.getDevices(false).stream()
				.filter(device -> clientId.equals(device.getClientId()))
				.toList()
				.isEmpty());
		assertEquals(
				1,
				component.getDevices(false).stream()
						.filter(device -> clientId.equals(device.getClientId()))
						.toList()
						.size());
		assertTrue(component.getDevices(true).stream()
				.filter(device -> clientId.equals(device.getClientId()))
				.toList()
				.isEmpty());
		testDevice = new Device();
		setupTestDevice(true);
		assertTrue(component.getDevices(false).stream()
				.filter(device -> clientId.equals(device.getClientId()))
				.toList()
				.isEmpty());
		assertFalse(component.getDevices(true).stream()
				.filter(device -> clientId.equals(device.getClientId()))
				.toList()
				.isEmpty());
		assertEquals(
				1,
				component.getDevices(true).stream()
						.filter(device -> clientId.equals(device.getClientId()))
						.toList()
						.size());
		testDevice = new Device();
	}

	@Test
	public void testSiteDelete() {
		TestUtils.setupSite(component, OSComponent);
		Device testSite = component.getDevicesBySite(TestDeviceComponent.clientId, TestDeviceComponent.SITE).stream()
				.filter(Device::isVirtual)
				.findFirst()
				.orElse(null);
		assertNotNull(testSite);
		assertTrue(component
				.getDevicesBySite(testSite.getClientId(), DeviceComponent.NO_SITE)
				.isEmpty());
		component.deleteDevice(testSite.getId(), testSite.getClientId());
		assertTrue(component
				.getDevicesBySite(testSite.getClientId(), testSite.getSite())
				.isEmpty());
		List<Device> updatedDevices = component.getDevicesBySite(testSite.getClientId(), DeviceComponent.NO_SITE);
		assertFalse(updatedDevices.isEmpty());
		updatedDevices.forEach(device -> assertEquals(device.getSite(), DeviceComponent.NO_SITE));
	}

	@Test
	public void testSiteUpdate() {
		TestUtils.setupSite(component, OSComponent);
		Device testSite = component.getDevicesBySite(TestDeviceComponent.clientId, TestDeviceComponent.SITE).stream()
				.filter(Device::isVirtual)
				.findFirst()
				.orElse(null);
		assertNotNull(testSite);
		List<Device> originalDevices = component.getDevicesBySite(testSite.getClientId(), testSite.getSite());
		testSite.setName(TestDeviceComponent.SITE + 2);
		testSite.setSite(TestDeviceComponent.SITE + 2);
		component.updateDevice(testSite);
		List<Device> updatedDevices = component.getDevicesBySite(testSite.getClientId(), testSite.getSite());
		assertEquals(originalDevices.size(), updatedDevices.size());
		assertFalse(updatedDevices.isEmpty());
		updatedDevices.forEach(device -> assertEquals(device.getSite(), testSite.getSite()));
	}

	@BeforeEach
	protected void setupTestDevice() {
		TestUtils.deleteAllCustomerDevices(component);
		setupTestDevice(false);
	}

	protected void setupTestDevice(boolean isVirtual) {
		try {
			component.getTable().putItem(testDevice);
		} catch (DynamoDbException e) {
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
			Device dbDevice = component.getTable().getItem(testDevice);
			if (dbDevice != null) {
				component.getTable().deleteItem(dbDevice);
			}
			component.getTable().putItem(testDevice);
			return;
		}
		fail();
	}
}
