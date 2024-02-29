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
	private String siteId;
	private int offset;
	private int size;
	private boolean virtual;
	private boolean isSite;
	private boolean noVirtual;
	private boolean noIsSite;
	private boolean daylight;
	private boolean includeSource = false;

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
		setIsSite(search.getIsSite());
		setNoIsSite(search.isNoIsSite());
		setSize(search.getSize());
		setOffset(search.getOffset());
		setSiteId(search.getSiteId());
		setIncludeSource(search.isIncludeSource());
	}

	public SearchJSON(String customerId, String deviceId, long endDate, long startDate) {
		setCustomerId(customerId);
		setDeviceId(deviceId);
		setEndDate(endDate);
		setStartDate(startDate);
	}

	public void setIsSite(boolean isSite) {
		this.isSite = isSite;
	}

	public boolean getIsSite() {
		return isSite;
	}

	public Date getJavaStartDate() {
		return new Date(startDate);
	}

	public Date getJavaEndDate() {
		return new Date(endDate);
	}
}
