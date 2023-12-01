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
				(String) openSearchMap.get(SITE),
				(String) openSearchMap.get(DEVICE_NAME),
				(String) openSearchMap.get(CUSTOMER_ID),
				(String) openSearchMap.get(DEVICE_ID));
		setTotalRealPower(doubleToFloat(openSearchMap.get(TOTAL_REAL_POWER)));
		setEnergyConsumed(doubleToFloat(openSearchMap.get(ENG_CONS)));
		setPowerFactor(doubleToFloat(openSearchMap.get(TOTAL_PF)));
		setAverageVoltage(doubleToFloat(openSearchMap.get(AVG_VOLT)));
		setAverageCurrent(doubleToFloat(openSearchMap.get(AVG_CURRENT)));
		setTotalEnergyConsumed(doubleToFloat(openSearchMap.get(TOTAL_ENG_CONS)));
		setDate(new Date((Long) openSearchMap.get(OpenSearchConstants.TIMESTAMP)));
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

	public DeviceData(String site, String name, String customerId, String deviceId) {
		attributes = new HashMap<>();
		attributes.put(SITE, new DeviceAttribute(SITE, "", site));
		attributes.put(DEVICE_NAME, new DeviceAttribute(DEVICE_NAME, "", name));
		attributes.put(CUSTOMER_ID, new DeviceAttribute(CUSTOMER_ID, "", customerId));
		attributes.put(DEVICE_ID, new DeviceAttribute(DEVICE_ID, "", deviceId));
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

	public String getTotalEnergyConsumedUnit() {
		return Optional.ofNullable(attributes.get(TOTAL_ENG_CONS))
				.map(DeviceAttribute::getUnit)
				.orElse(null);
	}

	public String getName() {
		return (String) attributes.get(DEVICE_NAME).getValue();
	}

	public String getSite() {
		return (String) attributes.get(SITE).getValue();
	}

	public String getCustomerId() {
		return (String) attributes.get(CUSTOMER_ID).getValue();
	}

	public String getDeviceId() {
		return (String) attributes.get(DEVICE_ID).getValue();
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
