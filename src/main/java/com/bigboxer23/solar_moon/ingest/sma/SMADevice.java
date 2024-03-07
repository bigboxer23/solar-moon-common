package com.bigboxer23.solar_moon.ingest.sma;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.device.DeviceComponent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import lombok.Data;

/** */
@Data
public class SMADevice implements ISMAIngestConstants {
	private String deviceName;

	private String customerId;

	Device device;

	private Date timestamp;

	private List<SMARecord> records = new ArrayList<>();

	public SMADevice(String customerId) {
		setCustomerId(customerId);
	}

	public void addRecord(SMARecord record) {
		if (TOTAL_YIELD.equalsIgnoreCase(record.getAttributeName())) {
			setDeviceName(record.getDevice());
			SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
			setDevice(IComponentRegistry.generationComponent.findDeviceFromDeviceName(customerId, deviceName));
			if (getDevice() == null) {
				IComponentRegistry.logger.error("cannot add record, no device created (licensing?)");
				return;
			}
			if (!DeviceComponent.NO_SITE.equals(getDevice().getSiteId())) {
				Device siteDevice =
						IComponentRegistry.deviceComponent.getDevice(getDevice().getSiteId(), customerId);
				IComponentRegistry.locationComponent
						.getLocalTimeZone(siteDevice.getLatitude(), siteDevice.getLongitude())
						.ifPresent(timeZone -> sdf.setTimeZone(TimeZone.getTimeZone(timeZone)));
			}
			try {
				setTimestamp(sdf.parse(record.getTimestamp()));
			} catch (ParseException e) {
				IComponentRegistry.logger.warn("cannot parse date string: " + record.getTimestamp(), e);
			}
		}
		records.add(record);
	}
}
