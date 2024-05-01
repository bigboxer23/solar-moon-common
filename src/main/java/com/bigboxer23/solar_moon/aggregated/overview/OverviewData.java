package com.bigboxer23.solar_moon.aggregated.overview;

import com.bigboxer23.solar_moon.data.Alarm;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.subscription.IHasSubscriptionDate;
import java.util.List;
import java.util.Map;
import lombok.Data;

/** */
@Data
public class OverviewData implements IHasSubscriptionDate {
	public OverviewData(List<Device> devices, List<Alarm> alarms) {
		setDevices(devices);
		setAlarms(alarms);
	}

	private List<Device> devices;

	private List<Alarm> alarms;

	private long trialDate = -1;

	private OverviewSiteData overall;

	private Map<String, OverviewSiteData> sitesOverviewData;
}
