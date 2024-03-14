package com.bigboxer23.solar_moon.ingest.sma;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.Device;
import java.text.ParseException;
import java.util.*;
import lombok.Data;
import software.amazon.awssdk.utils.StringUtils;

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
		if (getDevice() == null) {
			setDeviceName(record.getDevice());
			setDevice(IComponentRegistry.generationComponent.findDeviceFromDeviceName(customerId, deviceName));
			if (getDevice() == null) {
				IComponentRegistry.logger.error("cannot add record, no device created (licensing?)");
				return;
			}
			if (!StringUtils.isEmpty(record.getTimestamp())) {
				try {
					setTimestamp(
							SMAIngestComponent.getDateFormatter(getDevice()).parse(record.getTimestamp()));
				} catch (ParseException e) {
					IComponentRegistry.logger.warn("cannot parse date string: " + record.getTimestamp(), e);
				}
			}
		}
		records.add(record);
	}
}
