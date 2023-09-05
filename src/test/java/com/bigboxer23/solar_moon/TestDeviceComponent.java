package com.bigboxer23.solar_moon;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.data.Device;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

/** */
@ActiveProfiles("test")
public class TestDeviceComponent {

	protected String deviceKey = "2459786f-74c6-42e0-bc37-a501cb87297a";
	protected String deviceName = "testDevice";

	protected String clientId = "0badd0c2-450b-4204-80d5-c7c77fc13500";

	protected String deviceId = "edc76a9b-e451-4592-996e-0e54410bae5e";

	private DeviceComponent component = new DeviceComponent();

	private Device testDevice = new Device();

	@Test
	public void testFindDeviceByDeviceKey() {
		setupTestDevice();
		assertNull(component.findDeviceByDeviceKey("1234"));
		assertNotNull(component.findDeviceByDeviceKey(deviceKey));
	}

	@Test
	public void testDevices() {
		setupTestDevice();
		component
				.getDevices(clientId)
				.forEach(page -> assertEquals(1, page.items().size()));
		component
				.getDevices("tacoClient")
				.forEach(page -> assertEquals(0, page.items().size()));
	}

	protected void setupTestDevice() {
		try {
			component.getDeviceTable().putItem(testDevice);
		} catch (DynamoDbException e) {
			testDevice.setId(deviceId);
			testDevice.setName(deviceName);
			testDevice.setClientId(clientId);
			testDevice.setDeviceKey(deviceKey);
			Device dbDevice = component.getDeviceTable().getItem(testDevice);
			;
			if (dbDevice != null) {
				component.getDeviceTable().deleteItem(dbDevice);
			}
			component.getDeviceTable().putItem(testDevice);
			return;
		}
		fail();
	}
}
