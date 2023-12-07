package com.bigboxer23.solar_moon;

import static com.bigboxer23.solar_moon.ingest.MeterConstants.*;
import static com.bigboxer23.solar_moon.ingest.MeterConstants.TOTAL_PF;
import static com.bigboxer23.solar_moon.search.OpenSearchConstants.DATA_SEARCH_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.ingest.MeterConstants;
import com.bigboxer23.solar_moon.search.OpenSearchUtils;
import com.bigboxer23.solar_moon.search.SearchJSON;
import com.bigboxer23.solar_moon.util.TimeConstants;
import com.bigboxer23.solar_moon.util.TimeUtils;
import com.bigboxer23.solar_moon.util.TokenGenerator;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import javax.xml.xpath.XPathExpressionException;
import org.opensearch.client.opensearch.core.SearchResponse;

/** */
public class TestUtils implements IComponentRegistry, TestConstants {

	public static String getDeviceXML(String deviceName, Date date, float avgCurrent) {
		return getDeviceXML(device2Xml, deviceName, date, avgCurrent, -1, -1, -1, -1);
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
				device2Xml, deviceName, date, avgCurrent, avgVoltage, powerFactor, totalEnergyConsumed, totalRealPower);
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
			deviceXML = deviceXML.replace(TestConstants.deviceName, deviceName);
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

	public static void nukeCustomerId(String customerId) {
		deviceComponent.deleteDevicesByCustomerId(customerId);
		subscriptionComponent.updateSubscription(customerId, 0);
		OSComponent.deleteByCustomerId(customerId);
		alarmComponent.deleteAlarmsByCustomerId(customerId);
	}

	public static void setupSite(String customerId) {
		nukeCustomerId(customerId);
		Device testDevice = new Device();
		testDevice.setClientId(customerId);
		testDevice.setSite(TestConstants.SITE);
		for (int ai = 0; ai < 5; ai++) {
			addDevice(TestConstants.deviceName + ai, testDevice, false);
		}
		addDevice(TestConstants.SITE, testDevice, true);
	}

	public static void setupSite() {
		setupSite(TestConstants.CUSTOMER_ID);
	}

	public static Device getDevice() {
		return deviceComponent
				.findDeviceByDeviceName(TestConstants.CUSTOMER_ID, TestConstants.deviceName + 0)
				.orElse(null);
	}

	public static Device getSite() {
		return deviceComponent.getDevicesForCustomerId(TestConstants.CUSTOMER_ID).stream()
				.filter(Device::isVirtual)
				.findAny()
				.get();
	}

	public static void seedOpenSearchData(String customerId) throws XPathExpressionException {
		LocalDateTime ldt = LocalDateTime.ofInstant(
						TimeUtils.get15mRoundedDate().toInstant(), ZoneId.systemDefault())
				.minusDays(2);
		generationComponent.handleDeviceBody(
				TestUtils.getDeviceXML(
						TestConstants.SITE,
						Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()),
						5),
				customerId);
		for (int aj = 0; aj < 1; aj++) {
			for (int ai = 0; ai < 10; ai++) {
				generationComponent.handleDeviceBody(
						TestUtils.getDeviceXML(
								TestConstants.deviceName + aj,
								Date.from(ldt.minusMinutes(15 * ai)
										.atZone(ZoneId.systemDefault())
										.toInstant()),
								(5 * ai),
								230,
								1,
								(15 * ai),
								(10 * ai)),
						customerId);
			}
		}
		OpenSearchUtils.waitForIndexing();
	}

	public static void seedOpenSearchData() throws XPathExpressionException {
		seedOpenSearchData(TestConstants.CUSTOMER_ID);
	}

	private static void addDevice(String name, Device testDevice, boolean isVirtual) {
		testDevice.setId(TokenGenerator.generateNewToken());
		testDevice.setName("pretty" + name);
		testDevice.setDeviceName(name);
		testDevice.setVirtual(isVirtual);
		if (isVirtual) {
			testDevice.setDeviceName(null);
		}
		deviceComponent.addDevice(testDevice);
	}

	public static void validateDateData(String deviceName, Date date) {
		DeviceData data = OSComponent.getDeviceByTimePeriod(TestConstants.CUSTOMER_ID, deviceName, date);
		assertNotNull(data);
		assertEquals(date, data.getDate());
	}

	public static void validateDateData(Date date) {
		validateDateData(TestConstants.deviceName + 0, date);
	}

	public static void cloneUser(String customerId, String srcCustomerId) {
		Map<String, String> deviceIds = new HashMap<>();
		deviceComponent.getDevicesForCustomerId(srcCustomerId).forEach(d -> {
			deviceIds.put(d.getId(), TokenGenerator.generateNewToken());
			d.setClientId(customerId);
			d.setId(deviceIds.get(d.getId()));
			deviceComponent.addDevice(d);
		});
		SearchJSON search = new SearchJSON();
		search.setCustomerId(srcCustomerId);
		search.setType(DATA_SEARCH_TYPE);
		search.setSize(5000);
		search.setOffset(0);
		// half year
		for (int week = 1; week < 26; week++) {
			search.setStartDate(new Date().getTime() - TimeConstants.DAY * (7 * week));
			search.setEndDate(new Date().getTime() - TimeConstants.DAY * (7 * (week - 1)));
			SearchResponse<Map> response = OSComponent.search(search);
			List<DeviceData> datas = response.hits().hits().stream()
					.map(h -> {
						DeviceData data = OpenSearchUtils.getDeviceDataFromFields("", h.source());
						if (data != null) {
							if (data.getTotalEnergyConsumed() == -1) {
								data.getAttributes().remove(TOTAL_ENG_CONS);
							}
							if (data.getAverageVoltage() == -1) {
								data.getAttributes().remove(AVG_VOLT);
							}
							if (data.getAverageCurrent() == -1) {
								data.getAttributes().remove(AVG_CURRENT);
							}
							if (data.getPowerFactor() == -1) {
								data.getAttributes().remove(TOTAL_PF);
							}
							data.setCustomerId(customerId);
							data.setDeviceId(deviceIds.get(data.getDeviceId()));
						}
						return data;
					})
					.toList();
			if (!datas.isEmpty()) {
				OSComponent.logData(null, datas);
			}
			System.out.println(response.hits().total().value());
		}
	}
}
