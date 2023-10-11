package com.bigboxer23.solar_moon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.open_search.OpenSearchComponent;
import com.bigboxer23.solar_moon.util.TokenGenerator;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/** */
public class TestUtils {
	public static String getDeviceXML(String deviceName, Date date) {
		return getDeviceXML(TestConstants.device2Xml, deviceName, date);
	}

	public static String getDeviceXML(String deviceXML, String deviceName, Date date) {
		if (deviceName != null && !deviceName.isBlank()) {
			deviceXML = deviceXML.replace(TestDeviceComponent.deviceName, deviceName);
		}
		if (date != null) {
			SimpleDateFormat sdf = new SimpleDateFormat(MeterConstants.DATE_PATTERN_UTC);
			sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
			deviceXML = deviceXML.replace(TestConstants.date, sdf.format(date));
		}
		return deviceXML;
	}

	public static void deleteAllCustomerDevices(DeviceComponent deviceComponent) {
		deviceComponent
				.getDevices(TestDeviceComponent.clientId)
				.forEach(device -> deviceComponent.getTable().deleteItem(device));
	}

	public static void setupSite(DeviceComponent deviceComponent, OpenSearchComponent OSComponent) {
		deleteAllCustomerDevices(deviceComponent);
		OSComponent.deleteByCustomerId(TestDeviceComponent.clientId);
		Device testDevice = new Device();
		testDevice.setClientId(TestDeviceComponent.clientId);
		testDevice.setSite(TestDeviceComponent.SITE);
		for (int ai = 0; ai < 5; ai++) {
			addDevice(deviceComponent, TestDeviceComponent.deviceName + ai, testDevice, false);
		}
		addDevice(deviceComponent, TestDeviceComponent.SITE, testDevice, true);
	}

	private static void addDevice(DeviceComponent deviceComponent, String name, Device testDevice, boolean isVirtual) {
		testDevice.setId(TokenGenerator.generateNewToken());
		testDevice.setName(name);
		testDevice.setDeviceName(name);
		testDevice.setVirtual(isVirtual);
		if (isVirtual) {
			testDevice.setDeviceName(null);
		}
		deviceComponent.getTable().putItem(testDevice);
	}

	public static void validateDateData(OpenSearchComponent component, String deviceName, Date date) {
		DeviceData data = component.getDeviceByTimePeriod(TestDeviceComponent.clientId, deviceName, date);
		assertNotNull(data);
		assertEquals(date, data.getDate());
	}

	public static void validateDateData(OpenSearchComponent component, Date date) {
		validateDateData(component, TestDeviceComponent.deviceName + 0, date);
	}
}
