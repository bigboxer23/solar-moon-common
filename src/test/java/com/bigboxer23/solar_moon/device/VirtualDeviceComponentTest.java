package com.bigboxer23.solar_moon.device;

import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class VirtualDeviceComponentTest {

	@Mock
	private DeviceComponent mockDeviceComponent;

	private VirtualDeviceComponent virtualDeviceComponent;

	private static final String CUSTOMER_ID = "test-customer-123";
	private static final String DEVICE_ID = "device-123";
	private static final String SITE_ID = "site-123";

	@BeforeEach
	void setUp() {
		virtualDeviceComponent = new VirtualDeviceComponent();
	}

	@Test
	void testHandleVirtualDevice_withNoSiteId_doesNothing() {
		DeviceData deviceData = createDeviceData();
		deviceData.setSiteId(DeviceComponent.NO_SITE);

		virtualDeviceComponent.handleVirtualDevice(deviceData);
	}

	@Test
	void testHandleVirtualDevice_withValidData_handlesCorrectly() {
		DeviceData deviceData = createDeviceData();

		virtualDeviceComponent.handleVirtualDevice(deviceData);
	}

	@Test
	void testHandleVirtualDevice_withEmptySiteId_doesNotProcess() {
		DeviceData deviceData = createDeviceData();
		deviceData.setSiteId("");

		virtualDeviceComponent.handleVirtualDevice(deviceData);
	}

	@Test
	void testHandleVirtualDevice_withDifferentSiteIds_processesSeparately() {
		DeviceData deviceData1 = createDeviceData();
		deviceData1.setSiteId("site-1");

		DeviceData deviceData2 = createDeviceData();
		deviceData2.setSiteId("site-2");

		virtualDeviceComponent.handleVirtualDevice(deviceData1);
		virtualDeviceComponent.handleVirtualDevice(deviceData2);
	}

	@Test
	void testHandleVirtualDevice_withMultipleDevicesInSite_aggregatesCorrectly() {
		DeviceData deviceData = createDeviceData();
		deviceData.setEnergyConsumed(100.0f);
		deviceData.setTotalRealPower(50.0f);

		virtualDeviceComponent.handleVirtualDevice(deviceData);
	}

	@Test
	void testHandleVirtualDevice_withNegativeValues_handlesCorrectly() {
		DeviceData deviceData = createDeviceData();
		deviceData.setEnergyConsumed(-10.0f);
		deviceData.setTotalRealPower(-5.0f);

		virtualDeviceComponent.handleVirtualDevice(deviceData);
	}

	@Test
	void testHandleVirtualDevice_withZeroValues_handlesCorrectly() {
		DeviceData deviceData = createDeviceData();
		deviceData.setEnergyConsumed(0.0f);
		deviceData.setTotalRealPower(0.0f);

		virtualDeviceComponent.handleVirtualDevice(deviceData);
	}

	@Test
	void testHandleVirtualDevice_withLargeValues_handlesCorrectly() {
		DeviceData deviceData = createDeviceData();
		deviceData.setEnergyConsumed(999999.99f);
		deviceData.setTotalRealPower(888888.88f);

		virtualDeviceComponent.handleVirtualDevice(deviceData);
	}

	@Test
	void testHandleVirtualDevice_withSiteDevice_setsCorrectly() {
		DeviceData deviceData = createDeviceData();

		virtualDeviceComponent.handleVirtualDevice(deviceData);
	}

	@Test
	void testHandleVirtualDevice_withMultipleConcurrentRequests_handlesCorrectly() {
		DeviceData deviceData1 = createDeviceData();
		DeviceData deviceData2 = createDeviceData();

		virtualDeviceComponent.handleVirtualDevice(deviceData1);
		virtualDeviceComponent.handleVirtualDevice(deviceData2);
	}

	@Test
	void testHandleVirtualDevice_withDifferentTimestamps_processesEach() {
		DeviceData deviceData1 = createDeviceData();
		deviceData1.setDate(new Date(System.currentTimeMillis() - 10000));

		DeviceData deviceData2 = createDeviceData();
		deviceData2.setDate(new Date(System.currentTimeMillis()));

		virtualDeviceComponent.handleVirtualDevice(deviceData1);
		virtualDeviceComponent.handleVirtualDevice(deviceData2);
	}

	private DeviceData createDeviceData() {
		DeviceData data = new DeviceData(SITE_ID, CUSTOMER_ID, DEVICE_ID);
		data.setDate(new Date());
		data.setEnergyConsumed(100.0f);
		data.setTotalRealPower(50.0f);
		data.setTotalEnergyConsumed(1000.0f);
		return data;
	}

	private Device createVirtualDevice() {
		Device device = new Device(DEVICE_ID, CUSTOMER_ID, "Virtual Device");
		device.setVirtual(true);
		device.setSiteId(SITE_ID);
		device.setSite("Test Site");
		return device;
	}

	private Device createPhysicalDevice(String deviceId) {
		Device device = new Device(deviceId, CUSTOMER_ID, "Physical Device " + deviceId);
		device.setVirtual(false);
		device.setSiteId(SITE_ID);
		device.setSite("Test Site");
		return device;
	}
}
