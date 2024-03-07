package com.bigboxer23.solar_moon.ingest.sma;

import lombok.Data;

/** */
@Data
public class SMARecord {
	private String device;

	private String attributeName;

	private String timestamp;

	private String value;
}
