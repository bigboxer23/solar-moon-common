package com.bigboxer23.solar_moon.ingest.sma;

import lombok.Data;

/** */
@Data
public class SMARecord {
	private String device;

	private String attributeName;

	private String timestamp;

	private String value;

	public SMARecord() {}

	public SMARecord(SMARecord donor) {
		setDevice(donor.getDevice());
		setAttributeName(donor.getAttributeName());
		setTimestamp(donor.getTimestamp());
		setValue(donor.getValue());
	}
}
