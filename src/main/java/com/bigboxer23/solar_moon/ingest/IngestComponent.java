package com.bigboxer23.solar_moon.ingest;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceAttribute;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.util.TokenGenerator;
import com.bigboxer23.solar_moon.web.TransactionUtil;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.xml.xpath.*;
import org.opensearch.client.ResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import software.amazon.awssdk.utils.StringUtils;

/** Class to read data from the generation meter web interface */
// @Component
public class IngestComponent implements MeterConstants {

	private static final Map<String, String> fields = new HashMap<>();

	static {
		fields.put(TOTAL_ENG_CONS, TOTAL_ENG_CONS);
		fields.put(TOTAL_REAL_POWER, TOTAL_REAL_POWER);
		fields.put(AVG_CURRENT, AVG_CURRENT);
		fields.put(AVG_VOLT, AVG_VOLT);
		fields.put(TOTAL_PF, TOTAL_PF);
		fields.put("Energy Consumption", TOTAL_ENG_CONS);
		fields.put("Real Power", TOTAL_REAL_POWER);
		fields.put("Current", AVG_CURRENT);
		fields.put("Voltage, Line to Neutral", AVG_VOLT);
		fields.put("Power Factor", TOTAL_PF);
		fields.put("kWh del+rec", TOTAL_ENG_CONS);
		fields.put("I a", AVG_CURRENT);
		fields.put("Vll ab", AVG_VOLT);
		fields.put("PF sign tot", TOTAL_PF);
		fields.put("Total System Power Factor", TOTAL_PF);
	}

	private static final Logger logger = LoggerFactory.getLogger(IngestComponent.class);

	public DeviceData handleDeviceBody(String body, String customerId)
			throws XPathExpressionException, ResponseException {
		if (customerId == null || customerId.isBlank()) {
			logger.error("no customer id, not doing anything.");
			return null;
		}
		if (body == null || body.isBlank()) {
			logger.error("no body, not doing anything.");
			return null;
		}
		logger.debug("parsing device body: " + body);
		Device device = Optional.ofNullable(findDeviceName(body))
				.map(deviceName -> findDeviceFromDeviceName(customerId, deviceName))
				.orElse(null);
		if (device == null) {
			String deviceName = findDeviceName(body);
			logger.warn("New device found, " + deviceName);
			device = new Device(TokenGenerator.generateNewToken(), customerId, deviceName);
			IComponentRegistry.deviceComponent.addDevice(device);
		}
		TransactionUtil.addDeviceId(device.getId());
		DeviceData deviceData = Optional.of(device)
				.map(server -> parseDeviceInformation(
						body, server.getSite(), server.getDisplayName(), customerId, server.getId()))
				.filter(DeviceData::isValid)
				.orElse(null);
		if (deviceData == null) {
			logger.info("device was not valid, not handling.");
			return null;
		}
		IComponentRegistry.alarmComponent.resolveActiveAlarms(deviceData);
		Device site = IComponentRegistry.deviceComponent
				.findDeviceByName(device.getClientId(), device.getSite())
				.orElse(null);
		if (device.isDeviceSite()) {
			deviceData.setIsSite();
		}
		IComponentRegistry.locationComponent.addLocationData(deviceData, site);
		IComponentRegistry.weatherComponent.addWeatherData(deviceData, site);
		IComponentRegistry.OSComponent.logData(
				deviceData.getDate() != null ? deviceData.getDate() : new Date(),
				Collections.singletonList(deviceData));

		IComponentRegistry.virtualDeviceComponent.handleVirtualDevice(deviceData);
		return deviceData;
	}

	public boolean isUpdateEvent(String body) throws XPathExpressionException {
		return getNodeListForPath(body, MODE_PATH)
				.map(nodes ->
						nodes.getLength() > 0 && FILE_DATA.equals(nodes.item(0).getTextContent()))
				.orElse(false);
	}

	public String findDeviceName(String body) throws XPathExpressionException {
		return getNodeListForPath(body, DEVICE_NAME_PATH)
				.map(nodes -> nodes.getLength() > 0 ? nodes.item(0).getTextContent() : null)
				.orElse(null);
	}

	public boolean isOK(String body) {
		return Optional.ofNullable(findError(body))
				.map("Ok errorCode:0"::equalsIgnoreCase)
				.orElse(false);
	}

	private Optional<NodeList> getNodeListForPath(String body, String path) throws XPathExpressionException {
		if (StringUtils.isBlank(body)) {
			logger.error("no body, not doing anything.");
			return Optional.empty();
		}
		return Optional.ofNullable((NodeList) XPathFactory.newInstance()
				.newXPath()
				.compile(path)
				.evaluate(new InputSource(new StringReader(body)), XPathConstants.NODESET));
	}

	public String findError(String body) {
		try {
			return getNodeListForPath(body, ERROR_PATH)
					.map(nodes -> {
						if (nodes.getLength() < 1) {
							return null;
						}
						Node errorNode = nodes.item(0);
						return errorNode.getAttributes().getNamedItem("text").getTextContent()
								+ " errorCode:"
								+ errorNode.getTextContent();
					})
					.orElse(null);
		} catch (XPathExpressionException e) {
			logger.warn("findError", e);
		}
		return null;
	}

	private Device findDeviceFromDeviceName(String customerId, String deviceName) {
		if (customerId == null || customerId.isBlank() || deviceName == null || deviceName.isBlank()) {
			logger.warn("customer id or device name is null, can't find");
			return null;
		}
		logger.debug("finding device from device name/customer id " + deviceName + " " + customerId);
		return IComponentRegistry.deviceComponent.getDevicesForCustomerId(customerId).stream()
				.filter(server -> deviceName.equals(server.getDeviceName()))
				.findAny()
				.orElseGet(() -> {
					logger.warn("could not find device name for " + deviceName);
					return null;
				});
	}

	protected DeviceData parseDeviceInformation(
			String body, String site, String name, String customerId, String deviceId) {
		try {
			logger.debug("parsing device info " + site + ":" + name + "\n" + body);
			DeviceData deviceData = new DeviceData(site, name, customerId, deviceId);
			if (!isOK(body)) {
				IComponentRegistry.alarmComponent.faultDetected(
						customerId, deviceData.getDeviceId(), deviceData.getSite(), findError(body));
				return null;
			}
			getNodeListForPath(body, POINT_PATH).ifPresent(nodes -> {
				Map<String, String> mappingFields = new HashMap<>(fields);
				IComponentRegistry.mappingComponent
						.getMappings(customerId)
						.forEach(a -> mappingFields.put(a.getMappingName(), a.getAttribute()));
				for (int i = 0; i < nodes.getLength(); i++) {
					String attributeName =
							nodes.item(i).getAttributes().getNamedItem("name").getNodeValue();
					if (mappingFields.containsKey(attributeName)) {
						try {
							float value = Float.parseFloat(nodes.item(i)
									.getAttributes()
									.getNamedItem("value")
									.getNodeValue());
							deviceData.addAttribute(new DeviceAttribute(
									mappingFields.get(attributeName),
									nodes.item(i)
											.getAttributes()
											.getNamedItem("units")
											.getNodeValue(),
									value));
						} catch (NumberFormatException nfe) {
							logger.warn("bad value retrieved from xml " + attributeName + "\n" + body, nfe);
							String value = nodes.item(i)
									.getAttributes()
									.getNamedItem("value")
									.getNodeValue();
							if (StringUtils.isEmpty(value) || "NULL".equalsIgnoreCase(value)) {
								IComponentRegistry.alarmComponent.faultDetected(
										customerId, deviceData.getDeviceId(), deviceData.getSite(), findError(body));
							}
						}
					}
				}
			});
			calculateTotalRealPower(deviceData);
			calculateTotalEnergyConsumed(deviceData);
			calculateTime(deviceData, body);
			return deviceData;
		} catch (XPathExpressionException e) {
			logger.error("parseDeviceInformation", e);
		}
		return null;
	}

	private void calculateTime(DeviceData deviceData, String body) throws XPathExpressionException {
		getNodeListForPath(body, DATE_PATH).ifPresent(nodes -> {
			if (nodes.getLength() > 0) {
				Node timeNode = nodes.item(0);
				if (timeNode.getTextContent() == null
						|| "NULL".equals(timeNode.getTextContent())
						|| timeNode.getTextContent().isEmpty()
						|| timeNode.getAttributes().getNamedItem(ZONE) == null) {
					return;
				}
				SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);
				try {
					deviceData.setDate(sdf.parse(timeNode.getTextContent()
							+ " "
							+ timeNode.getAttributes().getNamedItem(ZONE).getNodeValue()));
				} catch (ParseException e) {
					logger.warn("cannot parse date string: " + body, e);
				}
			}
		});
	}

	private void calculateTotalRealPower(DeviceData deviceData) {
		if (deviceData.getTotalRealPower() != -1) {
			logger.debug("Value already exists, not calculating");
			return;
		}
		if (deviceData.getAverageVoltage() == -1
				|| deviceData.getAverageCurrent() == -1
				|| deviceData.getPowerFactor() == -1) {
			logger.info("missing required values to calculate real power "
					+ deviceData.getName()
					+ " "
					+ deviceData.getAverageVoltage()
					+ ","
					+ deviceData.getAverageCurrent()
					+ ","
					+ deviceData.getPowerFactor());
			return;
		}
		double rp = (deviceData.getAverageVoltage()
						* deviceData.getAverageCurrent()
						* Math.abs(deviceData.getPowerFactor() / 100)
						* Math.sqrt(3))
				/ 1000f;
		deviceData.setTotalRealPower(
				new BigDecimal(rp).setScale(1, RoundingMode.HALF_UP).floatValue());
	}

	/**
	 * Calculate the difference of power consumed since the last run. Add a new field with the
	 * difference
	 */
	private void calculateTotalEnergyConsumed(DeviceData deviceData) {
		if (deviceData.getName() == null) {
			logger.info("Can't calc total energy w/o device name");
			return;
		}
		logger.debug("calculating total energy consumed. " + deviceData.getName());
		float totalEnergyConsumption = deviceData.getTotalEnergyConsumed();
		if (totalEnergyConsumption < 0) {
			return;
		}
		Float previousTotalEnergyConsumed = IComponentRegistry.OSComponent.getTotalEnergyConsumed(deviceData.getName());
		if (previousTotalEnergyConsumed != null) {
			deviceData.setEnergyConsumed(totalEnergyConsumption - previousTotalEnergyConsumed);
		}
	}
}
