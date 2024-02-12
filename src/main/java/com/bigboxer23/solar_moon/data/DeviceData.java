package com.bigboxer23.solar_moon.data;

import static com.bigboxer23.solar_moon.ingest.MeterConstants.*;

import com.bigboxer23.solar_moon.search.OpenSearchConstants;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.Data;

/** */
@Data
public class DeviceData {

	private Map<String, DeviceAttribute> attributes;

	private Date date;

	public DeviceData(Map<String, Object> openSearchMap) {
		this(
				(String) openSearchMap.get(SITE_ID),
				(String) openSearchMap.get(SITE),//TODO: remove eventually
				(String) openSearchMap.get(DEVICE_NAME),
				(String) openSearchMap.get(CUSTOMER_ID_ATTRIBUTE),
				(String) openSearchMap.get(DEVICE_ID));
		setTotalRealPower(doubleToFloat(openSearchMap.get(TOTAL_REAL_POWER)));
		setEnergyConsumed(doubleToFloat(openSearchMap.get(ENG_CONS)));
		setPowerFactor(doubleToFloat(openSearchMap.get(TOTAL_PF)));
		setAverageVoltage(doubleToFloat(openSearchMap.get(AVG_VOLT)));
		setAverageCurrent(doubleToFloat(openSearchMap.get(AVG_CURRENT)));
		setTotalEnergyConsumed(doubleToFloat(openSearchMap.get(TOTAL_ENG_CONS)));
		setDate(new Date((Long) openSearchMap.get(OpenSearchConstants.TIMESTAMP)));
		if (openSearchMap.get(VIRTUAL) != null && (Boolean) openSearchMap.get(VIRTUAL)) {
			setIsVirtual();
		}
		if (openSearchMap.get(IS_SITE) != null && (Boolean) openSearchMap.get(IS_SITE)) {
			setIsSite();
		}
		if (openSearchMap.containsKey(DAYLIGHT)) {
			setDaylight((Boolean) openSearchMap.get(DAYLIGHT));
		}
		setWeather(openSearchMap.get(WEATHER_SUMMARY));
		setTemperature(openSearchMap.get(TEMPERATURE));
		addIfExists("cloudCover", openSearchMap);
		addIfExists("visibility", openSearchMap);
		setUVIndex(openSearchMap.get(UV_INDEX));
		addIfExists("precipIntensity", openSearchMap);
	}

	private void addIfExists(String attributeName, Map<String, Object> openSearchMap) {
		if (openSearchMap.containsKey(attributeName)) {
			addAttribute(new DeviceAttribute(attributeName, "", openSearchMap.get(attributeName)));
		}
	}

	public boolean isValid() {
		if (getAttributes().size() <= 4) {
			return false;
		}
		if (isVirtual()) {
			return true;
		}
		return getAverageVoltage() > -1
				&& getAverageCurrent() > -1
				&& getPowerFactor() != -1 // Check only for -1, b/c could be negative
				&& getTotalRealPower() > -1
				&& getTotalEnergyConsumed() > -1;
	}

	private float doubleToFloat(Object value) {
		if (value == null) {
			return -1;
		}
		return Optional.of(value)
				.map(val -> (Double) val)
				.map(Double::floatValue)
				.orElse(null);
	}

	public DeviceData(String siteId, String site, String name, String customerId, String deviceId) {
		attributes = new HashMap<>();
		attributes.put(SITE_ID, new DeviceAttribute(SITE_ID, "", siteId));
		attributes.put(SITE, new DeviceAttribute(SITE, "", site));
		attributes.put(DEVICE_NAME, new DeviceAttribute(DEVICE_NAME, "", name));
		setCustomerId(customerId);
		setDeviceId(deviceId);
	}

	public void addAttribute(DeviceAttribute attr) {
		attributes.put(attr.getName(), attr);
	}

	public float getTotalRealPower() {
		return (Float) Optional.ofNullable(attributes.get(TOTAL_REAL_POWER))
				.map(DeviceAttribute::getValue)
				.orElse(-1f);
	}

	public void setTotalRealPower(float totalRealPower) {
		addAttribute(new DeviceAttribute(TOTAL_REAL_POWER, getTotalEnergyConsumedUnit(), totalRealPower));
	}

	public float getTotalEnergyConsumed() {
		return (Float) Optional.ofNullable(attributes.get(TOTAL_ENG_CONS))
				.map(DeviceAttribute::getValue)
				.orElse(-1f);
	}

	public void setTotalEnergyConsumed(float totalEnergyConsumed) {
		addAttribute(new DeviceAttribute(TOTAL_ENG_CONS, getTotalEnergyConsumedUnit(), totalEnergyConsumed));
	}

	public float getEnergyConsumed() {
		return (Float) Optional.ofNullable(attributes.get(ENG_CONS))
				.map(DeviceAttribute::getValue)
				.orElse(-1f);
	}

	public void setEnergyConsumed(float energyConsumed) {
		addAttribute(new DeviceAttribute(ENG_CONS, getTotalEnergyConsumedUnit(), energyConsumed));
	}

	public void setIsVirtual() {
		addAttribute(new DeviceAttribute(VIRTUAL, "", true));
	}

	public boolean isVirtual() {
		return (Boolean) Optional.ofNullable(attributes.get(VIRTUAL))
				.map(DeviceAttribute::getValue)
				.orElse(false);
	}

	public void setIsSite() {
		addAttribute(new DeviceAttribute(IS_SITE, "", true));
	}

	public boolean isSite() {
		return (Boolean) Optional.ofNullable(attributes.get(IS_SITE))
				.map(DeviceAttribute::getValue)
				.orElse(false);
	}

	public void setDaylight(boolean isDaylight) {
		addAttribute(new DeviceAttribute(DAYLIGHT, "boolean", isDaylight));
	}

	public boolean isDayLight() {
		return (Boolean) Optional.ofNullable(attributes.get(DAYLIGHT))
				.map(DeviceAttribute::getValue)
				.orElse(false);
	}

	public void setTemperature(Object temperature) {
		Optional.ofNullable(temperature)
				.map(this::doubleToFloat)
				.ifPresent(t -> addAttribute(new DeviceAttribute(TEMPERATURE, "", t)));
	}

	public float getTemperature() {
		return (Float) Optional.ofNullable(attributes.get(TEMPERATURE))
				.map(DeviceAttribute::getValue)
				.orElse(-1f);
	}

	public void setUVIndex(Object uvIndex) {
		Optional.ofNullable(uvIndex)
				.map(this::doubleToFloat)
				.ifPresent(t -> addAttribute(new DeviceAttribute(UV_INDEX, "", t)));
	}

	public float getUVIndex() {
		return (Float) Optional.ofNullable(attributes.get(UV_INDEX))
				.map(DeviceAttribute::getValue)
				.orElse(-1f);
	}

	public void setWeather(Object weatherSummary) {
		Optional.ofNullable(weatherSummary)
				.map(s -> (String) s)
				.ifPresent(summary -> addAttribute(new DeviceAttribute(WEATHER_SUMMARY, "", summary)));
	}

	public String getWeatherSummary() {
		return (String) Optional.ofNullable(attributes.get(WEATHER_SUMMARY))
				.map(DeviceAttribute::getValue)
				.orElse("");
	}

	public String getTotalEnergyConsumedUnit() {
		return Optional.ofNullable(attributes.get(TOTAL_ENG_CONS))
				.map(DeviceAttribute::getUnit)
				.orElse(null);
	}

	public String getName() {
		return (String) attributes.get(DEVICE_NAME).getValue();
	}

	public void setName(String name) {
		attributes.put(DEVICE_NAME, new DeviceAttribute(DEVICE_NAME, "", name));
	}

	public String getSite() {
		return (String) attributes.get(SITE).getValue();
	}

	public String getSiteId() {
		return (String) attributes.get(SITE_ID).getValue();
	}

	public String getCustomerId() {
		return (String) attributes.get(CUSTOMER_ID_ATTRIBUTE).getValue();
	}

	public void setCustomerId(String customerId) {
		attributes.put(CUSTOMER_ID_ATTRIBUTE, new DeviceAttribute(CUSTOMER_ID_ATTRIBUTE, "", customerId));
	}

	public String getDeviceId() {
		return (String) attributes.get(DEVICE_ID).getValue();
	}

	public void setDeviceId(String deviceId) {
		attributes.put(DEVICE_ID, new DeviceAttribute(DEVICE_ID, "", deviceId));
	}

	public float getAverageVoltage() {
		return (float) Optional.ofNullable(getAttributes().get(AVG_VOLT))
				.map(DeviceAttribute::getValue)
				.orElse(-1f);
	}

	public void setAverageVoltage(float voltage) {
		addAttribute(new DeviceAttribute(AVG_VOLT, "", voltage));
	}

	public float getAverageCurrent() {
		return (float) Optional.ofNullable(getAttributes().get(AVG_CURRENT))
				.map(DeviceAttribute::getValue)
				.orElse(-1f);
	}

	public void setAverageCurrent(float current) {
		addAttribute(new DeviceAttribute(AVG_CURRENT, "", current));
	}

	public float getPowerFactor() {
		return (float) Optional.ofNullable(getAttributes().get(TOTAL_PF))
				.map(DeviceAttribute::getValue)
				.orElse(-1f);
	}

	public void setPowerFactor(float powerFactor) {
		addAttribute(new DeviceAttribute(TOTAL_PF, "", powerFactor));
	}
}
