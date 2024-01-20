package com.bigboxer23.solar_moon.aggregated.sites;

import com.bigboxer23.solar_moon.data.Device;
import java.util.List;
import java.util.Map;
import lombok.Data;

/** */
@Data
public class SitesOverviewData {
	private List<Device> devices;

	private Map<String, SitesSiteData> sites;
}
