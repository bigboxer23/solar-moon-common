package com.bigboxer23.solar_moon;

import static com.bigboxer23.solar_moon.ingest.MeterConstants.*;
import static com.bigboxer23.solar_moon.search.OpenSearchConstants.DATA_SEARCH_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceAttribute;
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
import org.opensearch.client.ResponseException;
import org.opensearch.client.opensearch.core.SearchResponse;
import software.amazon.awssdk.utils.StringUtils;

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
		if (CUSTOMER_ID.equals(customerId)) {
			subscriptionComponent.updateSubscription(customerId, 1);
		}
		Device testDevice = new Device();
		testDevice.setClientId(customerId);
		testDevice.setSite(TestConstants.SITE);
		addDevice(TestConstants.SITE, testDevice, true, null);
		for (int ai = 0; ai < 5; ai++) {
			addDevice(TestConstants.deviceName + ai, testDevice, false, testDevice.getSiteId());
		}
	}

	public static void setupSite() {
		setupSite(CUSTOMER_ID);
	}

	public static Device getDevice() {
		return deviceComponent
				.findDeviceByDeviceName(CUSTOMER_ID, TestConstants.deviceName + 0)
				.orElse(null);
	}

	public static Device getSite() {
		return deviceComponent.getDevicesForCustomerId(CUSTOMER_ID).stream()
				.filter(Device::isDeviceSite)
				.findAny()
				.get();
	}

	public static void seedOpenSearchData(String customerId) throws XPathExpressionException, ResponseException {
		LocalDateTime ldt = LocalDateTime.ofInstant(
						TimeUtils.get15mRoundedDate().toInstant(), ZoneId.systemDefault())
				.minusDays(2);
		for (int aj = 0; aj < 5; aj++) {
			System.out.println(TestConstants.deviceName + aj);
			for (int ai = 0; ai < 5; ai++) {
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

	public static void seedOpenSearchData() throws XPathExpressionException, ResponseException {
		seedOpenSearchData(CUSTOMER_ID);
	}

	private static void addDevice(String name, Device testDevice, boolean isVirtual, String siteId) {
		testDevice.setId(TokenGenerator.generateNewToken());
		testDevice.setName("pretty" + name);
		testDevice.setDeviceName(name);
		testDevice.setVirtual(isVirtual);
		testDevice.setIsSite(null);
		if (isVirtual) {
			testDevice.setDeviceName(null);
			testDevice.setIsSite("1");
			siteId = testDevice.getId();
		}
		testDevice.setSiteId(siteId);
		deviceComponent.addDevice(testDevice);
	}

	public static void validateDateData(String deviceName, Date date) {
		DeviceData data = OSComponent.getDeviceByTimePeriod(CUSTOMER_ID, deviceName, date);
		assertNotNull(data);
		assertEquals(date, data.getDate());
	}

	public static void validateDateData(Date date) {
		validateDateData(TestConstants.deviceName + 0, date);
	}

	private static void updateDeviceForClone(Device device, String customerId, String deviceName, String sitePrefix) {
		device.setClientId(customerId);
		device.setId(TokenGenerator.generateNewToken());
		device.setAddress(null);
		device.setDeviceKey(null);
		device.setNotificationsDisabled(true);
		device.setSite(sitePrefix + " " + TestConstants.SITE);
		device.setDeviceName(deviceName);
		device.setName(deviceName);
	}

	public static void cloneUser(String customerId, String srcCustomerId, String deviceFilter, int numberOfWeeks)
			throws ResponseException {
		Map<String, Device> devices = new HashMap<>();
		List<Device> srcDevices = deviceComponent.getDevicesForCustomerId(srcCustomerId);
		Device site = srcDevices.stream()
				.filter(d -> d.getName().startsWith(deviceFilter))
				.filter(Device::isVirtual)
				.findAny()
				.orElse(null);
		if (site == null) {
			logger.warn("can't find site");
			return;
		}
		devices.put(site.getId(), site);
		updateDeviceForClone(site, customerId, deviceFilter + " " + TestConstants.SITE, deviceFilter);
		deviceComponent.addDevice(site);
		srcDevices.stream()
				.filter(d -> d.getName().startsWith(deviceFilter))
				.filter(d -> !d.isVirtual())
				.forEach(d -> {
					devices.put(d.getId(), d);
					d.setMock(d.getName());
					updateDeviceForClone(
							d, customerId, d.getName().replace(deviceFilter, TestConstants.deviceName), deviceFilter);
					deviceComponent.addDevice(d);
				});
		SearchJSON search = new SearchJSON();
		search.setCustomerId(srcCustomerId);
		search.setType(DATA_SEARCH_TYPE);
		search.setSize(7500);
		search.setOffset(0);
		for (int week = 1; week < numberOfWeeks; week++) {
			search.setStartDate(new Date().getTime() - TimeConstants.DAY * (7L * week));
			search.setEndDate(new Date().getTime() - TimeConstants.DAY * (7L * (week - 1)));
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
							data.getAttributes()
									.put(
											MeterConstants.SITE,
											new DeviceAttribute(MeterConstants.SITE, "", site.getName()));
							data.setCustomerId(customerId);
							Optional<Device> srcDevice = Optional.ofNullable(devices.get(data.getDeviceId()));
							data.setDeviceId(srcDevice.map(Device::getId).orElse(null));
							data.setName(srcDevice.map(Device::getName).orElse(null));
						}
						return data;
					})
					.filter(d -> !StringUtils.isEmpty(d.getDeviceId()))
					.toList();
			if (!datas.isEmpty()) {
				OSComponent.logData(null, datas);
			}
			System.out.println(deviceFilter + " " + week + ":" + datas.size());
		}
	}
}
