package com.bigboxer23.solar_moon.aggregated.overview;

import com.bigboxer23.solar_moon.data.Alarm;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.Subscription;
import com.bigboxer23.solar_moon.subscription.IHasSubscription;
import java.util.List;
import java.util.Map;
import lombok.Data;

/** */
@Data
public class OverviewData implements IHasSubscription {
	public OverviewData(List<Device> devices, List<Alarm> alarms) {
		setDevices(devices);
		setAlarms(alarms);
	}

	private List<Device> devices;

	private List<Alarm> alarms;

	private Subscription subscription;

	private OverviewSiteData overall;

	private Map<String, OverviewSiteData> sitesOverviewData;
}
