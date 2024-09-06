package com.bigboxer23.solar_moon.ingest;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.alarm.SolectriaErrorOracle;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.data.LinkedDevice;
import com.bigboxer23.solar_moon.util.TimeConstants;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.opensearch.client.ResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import software.amazon.awssdk.utils.StringUtils;

/** */
public class ObviusIngestComponent implements MeterConstants {
	private static final Map<String, String> fields = new HashMap<>();

	static {
		fields.put(TOTAL_ENG_CONS, TOTAL_ENG_CONS);
		fields.put(TOTAL_REAL_POWER, TOTAL_REAL_POWER);
		fields.put(AVG_CURRENT, AVG_CURRENT);
		fields.put(AVG_VOLT, AVG_VOLT);
		fields.put(TOTAL_PF, TOTAL_PF);

		fields.put(ENERGY_CONSUMED_LABEL, TOTAL_ENG_CONS);
		fields.put(REAL_POWER_LABEL, TOTAL_REAL_POWER);
		fields.put(CURRENT_LABEL, AVG_CURRENT);
		fields.put(VOLTAGE_LABEL, AVG_VOLT);
		fields.put(POWER_FACTOR_LABEL, TOTAL_PF);

		fields.put("Voltage, Line to Neutral", AVG_VOLT);
		fields.put("Power Factor", TOTAL_PF);
		fields.put("kWh del+rec", TOTAL_ENG_CONS);
		fields.put("I a", AVG_CURRENT);
		fields.put("Vll ab", AVG_VOLT);
		fields.put("PF sign tot", TOTAL_PF);
		fields.put("Total System Power Factor", TOTAL_PF);
	}

	private static final Logger logger = LoggerFactory.getLogger(ObviusIngestComponent.class);

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
		if (isLinkedDevice(body)) {
			handleLinkedBody(body, customerId);
			return null;
		}
		Device device =
				IComponentRegistry.generationComponent.findDeviceFromDeviceName(customerId, findDeviceName(body));
		if (device == null) {
			logger.error("error getting device for device name " + findDeviceName(body));
			return null;
		}
		handleSerialNumber(device, body);
		logger.debug("parsing device body: " + body);
		return IComponentRegistry.generationComponent.handleDevice(
				device,
				Optional.of(device)
						.map(server -> parseDeviceInformation(
								body, server.getSiteId(), server.getDisplayName(), customerId, server.getId()))
						.filter(DeviceData::isValid)
						.orElse(null));
	}

	public void handleSerialNumber(Device device, String body) throws XPathExpressionException {
		if (device == null || StringUtils.isBlank(body) || StringUtils.isNotBlank(device.getSerialNumber())) {
			logger.debug("not handling serial number, bad body, device or already exists on device");
			return;
		}
		findSerialNumber(body).ifPresent(serialNumber -> {
			logger.info("adding serial number " + device.getSerialNumber());
			device.setSerialNumber(serialNumber);
			IComponentRegistry.deviceComponent.updateDevice(device);
		});
	}

	protected Optional<String> findSerialNumber(String body) throws XPathExpressionException {
		return getNodeListForPath(body, SERIAL_PATH).map(nodes -> {
			if (nodes.getLength() > 0 && StringUtils.isNotBlank(nodes.item(0).getTextContent())) {
				return nodes.item(0).getTextContent();
			}
			return null;
		});
	}

	public boolean isLinkedDevice(String body) throws XPathExpressionException {
		Boolean[] isLinkedDevice = {false};
		getNodeListForPath(body, POINT_PATH).ifPresent(nodes -> {
			for (int i = 0; !isLinkedDevice[0] && i < nodes.getLength(); i++) {
				String attributeName =
						nodes.item(i).getAttributes().getNamedItem("name").getNodeValue();
				if (CRITICAL_ALARMS.equals(attributeName) || INFORMATIVE_ALARMS.equals(attributeName)) {
					isLinkedDevice[0] = true;
				}
			}
		});
		return isLinkedDevice[0];
	}

	public void handleLinkedBody(String body, String customerId) throws XPathExpressionException {
		if (StringUtils.isBlank(body) || StringUtils.isBlank(customerId)) {
			logger.error("Customer id or body is invalid, cannot handle linked body");
			return;
		}
		Optional<String> serial = findSerialNumber(body);
		if (serial.isEmpty()) {
			logger.error("Can't find serial number, cannot handle linked body");
			return;
		}
		logger.info("handling linked device " + serial.get());
		LinkedDevice linkedDevice = new LinkedDevice(serial.get(), customerId);
		getTimestampFromBody(body).map(Date::getTime).ifPresent(linkedDevice::setDate);
		if (linkedDevice.getDate() <= 0) {
			logger.error(linkedDevice.getId() + " Can't find date, cannot handle linked body");
			return;
		}
		getNodeListForPath(body, POINT_PATH).ifPresent(nodes -> {
			for (int i = 0;
					(linkedDevice.getCriticalAlarm() == -1 || linkedDevice.getInformativeAlarm() == -1)
							&& i < nodes.getLength();
					i++) {
				String attributeName =
						nodes.item(i).getAttributes().getNamedItem("name").getNodeValue();
				if (CRITICAL_ALARMS.equals(attributeName)) {
					linkedDevice.setCriticalAlarm(SolectriaErrorOracle.rawErrorToCode(
							nodes.item(i).getAttributes().getNamedItem("value").getNodeValue()));
				}
				if (INFORMATIVE_ALARMS.equals(attributeName)) {
					linkedDevice.setInformativeAlarm(SolectriaErrorOracle.rawErrorToCode(
							nodes.item(i).getAttributes().getNamedItem("value").getNodeValue()));
				}
			}
		});
		IComponentRegistry.linkedDeviceComponent.update(linkedDevice);
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

	protected DeviceData parseDeviceInformation(
			String body, String siteId, String name, String customerId, String deviceId) {
		try {
			logger.debug("parsing device info " + siteId + ":" + name + "\n" + body);
			if (!isOK(body)) {
				IComponentRegistry.alarmComponent.faultDetected(customerId, deviceId, siteId, findError(body));
				return getTimestampFromBody(body)
						.map(date -> {
							DeviceData deviceData = DeviceData.createEmpty(siteId, customerId, deviceId, date);
							deviceData.setTotalEnergyConsumed(IComponentRegistry.OSComponent.getMaxTotalEnergyConsumed(
									customerId, deviceId, 7 * TimeConstants.DAY));
							return deviceData;
						})
						.orElse(null);
			}
			DeviceData deviceData = new DeviceData(siteId, customerId, deviceId);
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
							deviceData.addAttribute(mappingFields.get(attributeName), value);
						} catch (NumberFormatException nfe) {
							logger.warn("bad value retrieved from xml " + attributeName + "\n" + body, nfe);
							String value = nodes.item(i)
									.getAttributes()
									.getNamedItem("value")
									.getNodeValue();
							if (StringUtils.isEmpty(value) || "NULL".equalsIgnoreCase(value)) {
								IComponentRegistry.alarmComponent.faultDetected(
										customerId, deviceData.getDeviceId(), deviceData.getSiteId(), findError(body));
							}
						}
					}
				}
			});
			calculateTotalRealPower(deviceData);
			getTimestampFromBody(body).ifPresent(deviceData::setDate);
			return deviceData;
		} catch (XPathExpressionException e) {
			logger.error("parseDeviceInformation", e);
		}
		return null;
	}

	protected Optional<Date> getTimestampFromBody(String body) throws XPathExpressionException {
		return getNodeListForPath(body, DATE_PATH).map(nodes -> {
			if (nodes.getLength() > 0) {
				Node timeNode = nodes.item(0);
				if (timeNode.getTextContent() == null
						|| "NULL".equals(timeNode.getTextContent())
						|| timeNode.getTextContent().isEmpty()
						|| timeNode.getAttributes().getNamedItem(ZONE) == null) {
					return null;
				}
				SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);
				try {
					return sdf.parse(timeNode.getTextContent()
							+ " "
							+ timeNode.getAttributes().getNamedItem(ZONE).getNodeValue());
				} catch (ParseException e) {
					logger.warn("cannot parse date string: " + body, e);
				}
			}
			return null;
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
					+ deviceData.getDeviceId()
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
}
