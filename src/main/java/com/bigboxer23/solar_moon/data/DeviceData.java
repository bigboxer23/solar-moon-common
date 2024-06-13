package com.bigboxer23.solar_moon.data;

import static com.bigboxer23.solar_moon.ingest.MeterConstants.*;
import static com.bigboxer23.solar_moon.search.OpenSearchConstants.TIMESTAMP;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import java.util.Optional;
import lombok.Data;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.utils.StringUtils;

/** */
@Data
public class DeviceData {
	@JsonProperty(TIMESTAMP)
	private Date date;

	@JsonProperty(SITE_ID)
	private String siteId;

	@JsonProperty(CUSTOMER_ID_ATTRIBUTE)
	private String customerId;

	@JsonProperty(DEVICE_ID)
	private String deviceId;

	@JsonProperty(TOTAL_REAL_POWER)
	private float totalRealPower = -1;

	@JsonProperty(TOTAL_ENG_CONS)
	private float totalEnergyConsumed = -1;

	@JsonProperty(ENG_CONS)
	private float energyConsumed = -1;

	@JsonProperty(VIRTUAL)
	private boolean isVirtual = false;

	@JsonProperty(IS_SITE)
	private boolean isSite = false;

	@JsonProperty(DAYLIGHT)
	private boolean daylight = false;

	@JsonProperty(TEMPERATURE)
	private float temperature = -1;

	@JsonProperty(UV_INDEX)
	private float uVIndex = -1;

	@JsonProperty(PRECIPITATION_INTENSITY)
	private float precipitationIntensity = 0;

	@JsonProperty(WEATHER_SUMMARY)
	private String weatherSummary = "";

	@JsonProperty(AVG_VOLT)
	private float averageVoltage = -1;

	@JsonProperty(AVG_CURRENT)
	private float averageCurrent = -1;

	@JsonProperty(TOTAL_PF)
	private float powerFactor = -1;

	@JsonProperty(VISIBILITY)
	private float visibility = 0;

	@JsonProperty(CLOUD_COVER)
	private float cloudCover = 0;

	public DeviceData() {}

	public DeviceData(String siteId, String customerId, String deviceId) {
		setSiteId(siteId);
		setCustomerId(customerId);
		setDeviceId(deviceId);
	}

	public DeviceData(DeviceData deviceData) {
		this(deviceData.getSiteId(), deviceData.getCustomerId(), deviceData.getDeviceId());
		setTotalRealPower(deviceData.getTotalRealPower());
		setEnergyConsumed(deviceData.getEnergyConsumed());
		setPowerFactor(deviceData.getPowerFactor());
		setAverageVoltage(deviceData.getAverageVoltage());
		setAverageCurrent(deviceData.getAverageCurrent());
		setTotalEnergyConsumed(deviceData.getTotalEnergyConsumed());
		setDate(deviceData.getDate());
		setVirtual(deviceData.isVirtual());
		setSite(deviceData.isSite());
		setDaylight(deviceData.isDaylight());
		setWeatherSummary(deviceData.getWeatherSummary());
		setTemperature(deviceData.getTemperature());
		setVisibility(deviceData.getVisibility());
		setCloudCover(deviceData.getCloudCover());
		setUVIndex(deviceData.getUVIndex());
		setPrecipitationIntensity(deviceData.getPrecipitationIntensity());
	}

	public static DeviceData createEmpty(String siteId, String customerId, String deviceId, Date timestamp) {
		DeviceData data = new DeviceData(siteId, customerId, deviceId);
		data.setDate(timestamp);
		data.setPowerFactor(1);
		data.setAverageCurrent(0);
		data.setTotalEnergyConsumed(0);
		data.setTotalRealPower(0);
		data.setAverageVoltage(0);
		return data;
	}

	public void addAttribute(String key, Object value) {
		switch (key) {
			case TOTAL_REAL_POWER:
				setTotalRealPower(doubleToFloat(value));
				break;
			case TOTAL_ENG_CONS:
				setTotalEnergyConsumed(doubleToFloat(value));
				break;
			case ENG_CONS:
				setEnergyConsumed(doubleToFloat(value));
				break;
			case VIRTUAL:
				setVirtual((Boolean) value);
				break;
			case IS_SITE:
				setSite((Boolean) value);
				break;
			case DAYLIGHT:
				setDaylight((Boolean) value);
				break;
			case TEMPERATURE:
				setTemperature(doubleToFloat(value));
				break;
			case UV_INDEX:
				setUVIndex(doubleToFloat(value));
				break;
			case PRECIPITATION_INTENSITY:
				setPrecipitationIntensity(doubleToFloat(value));
				break;
			case WEATHER_SUMMARY:
				setWeatherSummary((String) value);
				break;
			case SITE_ID:
				setSiteId((String) value);
				break;
			case CUSTOMER_ID_ATTRIBUTE:
				setCustomerId((String) value);
				break;
			case DEVICE_ID:
				setDeviceId((String) value);
				break;
			case AVG_VOLT:
				setAverageVoltage(doubleToFloat(value));
				break;
			case AVG_CURRENT:
				setAverageCurrent(doubleToFloat(value));
				break;
			case TOTAL_PF:
				setPowerFactor(doubleToFloat(value));
				break;
			case VISIBILITY:
				setVisibility(doubleToFloat(value));
				break;
			case CLOUD_COVER:
				setCloudCover(doubleToFloat(value));
				break;
			default:
				LoggerFactory.getLogger(DeviceData.class).error("unknown field: " + key);
				throw new RuntimeException("unknown field: " + key);
		}
	}

	@JsonIgnore
	public boolean isValid() {
		if (StringUtils.isEmpty(siteId)
				|| StringUtils.isEmpty(customerId)
				|| StringUtils.isEmpty(deviceId)
				|| getDate() == null) {
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
		return switch (value) {
			case null -> -1;
			case Integer aI -> aI;
			case Float aV -> aV;
			default -> Optional.of(value)
					.map(val -> (Double) val)
					.map(Double::floatValue)
					.orElse(null);
		};
	}
}
