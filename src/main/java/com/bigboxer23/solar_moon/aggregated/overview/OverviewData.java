package com.bigboxer23.solar_moon.aggregated.overview;

import com.bigboxer23.solar_moon.data.Alarm;
import com.bigboxer23.solar_moon.data.Device;
import java.util.List;
import java.util.Map;
import lombok.Data;

/** */
@Data
public class OverviewData {
	private List<Device> devices;

	private List<Alarm> alarms;

	private SiteOverviewData overall;

	private Map<String, SiteOverviewData> sitesOverviewData;
}
