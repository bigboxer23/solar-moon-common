package com.bigboxer23.solar_moon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.open_search.OpenSearchUtils;
import com.bigboxer23.solar_moon.util.TokenGenerator;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;
import javax.xml.xpath.XPathExpressionException;

/** */
public class TestUtils implements IComponentRegistry {

	public static String getDeviceXML(String deviceName, Date date, float avgCurrent) {
		return getDeviceXML(TestConstants.device2Xml, deviceName, date, avgCurrent, -1, -1, -1, -1);
	}

	public static String getDeviceXML(
			String deviceName,
			Date date,
			float avgCurrent,
			float avgVoltage,
			float powerFactor,
			float totalEnergyConsumed,
			float totalRealPower) {
		return getDeviceXML(
				TestConstants.device2Xml,
				deviceName,
				date,
				avgCurrent,
				avgVoltage,
				powerFactor,
				totalEnergyConsumed,
				totalRealPower);
	}

	public static String getDeviceXML(String deviceXML, String deviceName, Date date, float avgCurrent) {
		return getDeviceXML(deviceXML, deviceName, date, avgCurrent, -1, -1, -1, -1);
	}

	public static String getDeviceXML(
			String deviceXML,
			String deviceName,
			Date date,
			float avgCurrent,
			float avgVoltage,
			float powerFactor,
			float totalEnergyConsumed,
			float totalRealPower) {
		if (deviceName != null && !deviceName.isBlank()) {
			deviceXML = deviceXML.replace(TestDeviceComponent.deviceName, deviceName);
		}
		if (date != null) {
			SimpleDateFormat sdf = new SimpleDateFormat(MeterConstants.DATE_PATTERN_UTC);
			sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
			deviceXML = deviceXML.replace(TestConstants.date, sdf.format(date));
		}
		if (totalRealPower >= 0) {
			deviceXML = deviceXML.replace("2.134055", "" + totalRealPower);
		}
		if (avgCurrent >= 0) {
			deviceXML = deviceXML.replace("57.2345", "" + avgCurrent);
		}
		if (avgVoltage >= 0) {
			deviceXML = deviceXML.replace("4285.5", "" + avgVoltage);
		}
		if (totalEnergyConsumed >= 0) {
			deviceXML = deviceXML.replace("9703343", "" + totalEnergyConsumed);
		}
		if (powerFactor >= 0) {
			deviceXML = deviceXML.replace("89.123", "" + powerFactor);
		}
		return deviceXML;
	}

	public static void setupSite(String customerId) {
		deviceComponent.deleteDevicesByCustomerId(customerId);
		subscriptionComponent.updateSubscription(customerId, 1);
		OSComponent.deleteByCustomerId(customerId);
		Device testDevice = new Device();
		testDevice.setClientId(customerId);
		testDevice.setSite(TestDeviceComponent.SITE);
		for (int ai = 0; ai < 5; ai++) {
			addDevice(TestDeviceComponent.deviceName + ai, testDevice, false);
		}
		addDevice(TestDeviceComponent.SITE, testDevice, true);
	}

	public static void setupSite() {
		setupSite(TestDeviceComponent.clientId);
	}

	public static void seedOpenSearchData(String customerId) throws XPathExpressionException {
		LocalDateTime ldt = LocalDateTime.ofInstant(
						TimeUtils.get15mRoundedDate().toInstant(), ZoneId.systemDefault())
				.minusDays(2);
		generationComponent.handleDeviceBody(
				TestUtils.getDeviceXML(
						TestDeviceComponent.SITE,
						Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()),
						5),
				customerId);
		for (int aj = 0; aj < 1; aj++) {
			for (int ai = 0; ai < 10; ai++) {
				generationComponent.handleDeviceBody(
						TestUtils.getDeviceXML(
								TestDeviceComponent.deviceName + aj,
								Date.from(ldt.minusMinutes(15 * ai)
										.atZone(ZoneId.systemDefault())
										.toInstant()),
								(5 * ai)),
						customerId);
			}
		}
		OpenSearchUtils.waitForIndexing();
	}

	public static void seedOpenSearchData() throws XPathExpressionException {
		seedOpenSearchData(TestDeviceComponent.clientId);
	}

	private static void addDevice(String name, Device testDevice, boolean isVirtual) {
		testDevice.setId(TokenGenerator.generateNewToken());
		testDevice.setName(name);
		testDevice.setDeviceName(name);
		testDevice.setVirtual(isVirtual);
		if (isVirtual) {
			testDevice.setDeviceName(null);
		}
		deviceComponent.addDevice(testDevice);
	}

	public static void validateDateData(String deviceName, Date date) {
		DeviceData data = OSComponent.getDeviceByTimePeriod(TestDeviceComponent.clientId, deviceName, date);
		assertNotNull(data);
		assertEquals(date, data.getDate());
	}

	public static void validateDateData(Date date) {
		validateDateData(TestDeviceComponent.deviceName + 0, date);
	}
}
