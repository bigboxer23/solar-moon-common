package com.bigboxer23.solar_moon.search;

import java.util.Date;
import lombok.Data;

/** */
@Data
public class SearchJSON {
	private String customerId;
	private String deviceName;
	private String deviceId;
	private long endDate;
	private long startDate;
	private String timeZone; // America/Chicago, etc
	private String bucketSize = "30m";
	private String type;
	private String site;
	private int offset;
	private int size;
	private boolean virtual;
	private boolean noVirtual;
	private boolean daylight;

	public SearchJSON() {}

	public SearchJSON(SearchJSON search) {
		setCustomerId(search.getCustomerId());
		setDeviceName(search.getDeviceName());
		setEndDate(search.getEndDate());
		setStartDate(search.getStartDate());
		setDeviceId(search.getDeviceId());
		setTimeZone(search.getTimeZone());
		setBucketSize(search.getBucketSize());
		setVirtual(search.isVirtual());
		setNoVirtual(search.isNoVirtual());
		setDaylight(search.isDaylight());
		setSite(search.getSite());
		setType(search.getType());
	}

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
