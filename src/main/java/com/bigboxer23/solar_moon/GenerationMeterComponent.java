package com.bigboxer23.solar_moon;

import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceAttribute;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.open_search.OpenSearchComponent;
import com.bigboxer23.solar_moon.util.TokenGenerator;
import com.bigboxer23.solar_moon.web.TransactionUtil;
import com.bigboxer23.utils.http.RequestBuilderCallback;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.xml.xpath.*;
import okhttp3.Credentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import software.amazon.awssdk.utils.StringUtils;

/** Class to read data from the generation meter web interface */
// @Component
public class GenerationMeterComponent implements MeterConstants {

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
	}

	private static final Logger logger = LoggerFactory.getLogger(GenerationMeterComponent.class);

	private final OpenSearchComponent openSearch;

	private final AlarmComponent alarmComponent;

	private final DeviceComponent deviceComponent;

	private final SiteComponent siteComponent;

	public GenerationMeterComponent(
			OpenSearchComponent openSearch,
			AlarmComponent alarmComponent,
			DeviceComponent deviceComponent,
			SiteComponent siteComponent) {
		this.openSearch = openSearch;
		this.alarmComponent = alarmComponent;
		this.deviceComponent = deviceComponent;
		this.siteComponent = siteComponent;
	}

	public DeviceData handleDeviceBody(String body, String customerId) throws XPathExpressionException {
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
			deviceComponent.addDevice(device);
		}
		TransactionUtil.addDeviceId(device.getId());
		DeviceData deviceData = Optional.of(device)
				.map(server -> parseDeviceInformation(
						body,
						server.getSite(),
						StringUtils.isBlank(server.getName()) ? server.getDeviceName() : server.getName(),
						customerId,
						server.getId()))
				.filter(DeviceData::isValid)
				.orElse(null);
		if (deviceData == null) {
			logger.info("device was not valid, not handling.");
			return null;
		}
		alarmComponent.resolveActiveAlarms(customerId, deviceData);
		openSearch.logData(
				deviceData.getDate() != null ? deviceData.getDate() : new Date(),
				Collections.singletonList(deviceData));
		siteComponent.handleSite(deviceData);
		return deviceData;
	}

	public boolean isUpdateEvent(String body) throws XPathExpressionException {
		if (body == null || body.isBlank()) {
			logger.error("no body, not doing anything.");
			return false;
		}
		NodeList nodes = (NodeList) XPathFactory.newInstance()
				.newXPath()
				.compile(MODE_PATH)
				.evaluate(new InputSource(new StringReader(body)), XPathConstants.NODESET);
		boolean isUpdate =
				nodes.getLength() > 0 && FILE_DATA.equals(nodes.item(0).getTextContent());
		if (!isUpdate) {
			logger.debug("event is not " + FILE_DATA + ", doing nothing.");
		}
		return isUpdate;
	}

	public String findDeviceName(String body) throws XPathExpressionException {
		NodeList nodes = (NodeList) XPathFactory.newInstance()
				.newXPath()
				.compile(DEVICE_NAME_PATH)
				.evaluate(new InputSource(new StringReader(body)), XPathConstants.NODESET);
		return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : null;
	}

	private Device findDeviceFromDeviceName(String customerId, String deviceName) {
		if (customerId == null || customerId.isBlank() || deviceName == null || deviceName.isBlank()) {
			logger.warn("customer id or device name is null, can't find");
			return null;
		}
		logger.debug("finding device from device name/customer id " + deviceName + " " + customerId);
		return deviceComponent.getDevices(customerId).stream()
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
			InputSource xml = new InputSource(new StringReader(body));
			NodeList nodes = (NodeList)
					XPathFactory.newInstance().newXPath().compile(POINT_PATH).evaluate(xml, XPathConstants.NODESET);
			DeviceData deviceData = new DeviceData(site, name, customerId, deviceId);
			for (int i = 0; i < nodes.getLength(); i++) {
				String attributeName =
						nodes.item(i).getAttributes().getNamedItem("name").getNodeValue();
				if (fields.containsKey(attributeName)) {
					try {
						float value = Float.parseFloat(nodes.item(i)
								.getAttributes()
								.getNamedItem("value")
								.getNodeValue());
						deviceData.addAttribute(new DeviceAttribute(
								fields.get(attributeName),
								nodes.item(i)
										.getAttributes()
										.getNamedItem("units")
										.getNodeValue(),
								value));
					} catch (NumberFormatException nfe) {
						logger.warn("bad value retrieved from xml " + attributeName + "\n" + body, nfe);
						if (nodes.item(i).getAttributes().getNamedItem("value").getNodeValue() == null) {
							alarmComponent.alarmConditionDetected(
									customerId, deviceData, "bad value retrieved from device " + attributeName);
						}
					}
				}
			}
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
		InputSource xml = new InputSource(new StringReader(body));
		NodeList nodes = (NodeList)
				XPathFactory.newInstance().newXPath().compile(DATE_PATH).evaluate(xml, XPathConstants.NODESET);
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
	 *
	 * @param serverName
	 * @param attr
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
		Float previousTotalEnergyConsumed = openSearch.getTotalEnergyConsumed(deviceData.getName());
		if (previousTotalEnergyConsumed != null) {
			deviceData.setEnergyConsumed(totalEnergyConsumption - previousTotalEnergyConsumed);
		}
	}

	private RequestBuilderCallback getAuthCallback(String user, String pass) {
		return builder -> builder.addHeader("Authorization", Credentials.basic(user, pass));
	}
}
