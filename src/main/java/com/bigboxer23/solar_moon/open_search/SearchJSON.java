package com.bigboxer23.solar_moon.open_search;

import java.util.Date;
import lombok.Data;

/** */
@Data
public class SearchJSON {
	private String customerId;
	private String deviceName;
	private long endDate;
	private long startDate;
	private String timeZone; // America/Chicago, etc
	private String bucketSize = "30m";
	private String type;

	public SearchJSON() {}

	public SearchJSON(String customerId, String deviceName, long endDate, long startDate) {
		setCustomerId(customerId);
		setDeviceName(deviceName);
		setEndDate(endDate);
		setStartDate(startDate);
	}

	public Date getJavaStartDate() {
		return new Date(startDate);
	}

	public Date getJavaEndDate() {
		return new Date(endDate);
	}
}
