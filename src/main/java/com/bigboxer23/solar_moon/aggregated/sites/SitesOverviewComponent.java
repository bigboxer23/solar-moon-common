package com.bigboxer23.solar_moon.aggregated.sites;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.search.OpenSearchConstants;
import com.bigboxer23.solar_moon.search.OpenSearchQueries;
import com.bigboxer23.solar_moon.search.SearchJSON;
import com.bigboxer23.solar_moon.util.TimeConstants;
import com.bigboxer23.solar_moon.util.TimeUtils;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;
import software.amazon.awssdk.utils.StringUtils;

/** */
public class SitesOverviewComponent implements IComponentRegistry {
	public SitesOverviewData getSitesOverviewData(SearchJSON search) {
		SitesOverviewData data = new SitesOverviewData();
		data.setDevices(deviceComponent.getDevicesForCustomerId(search.getCustomerId()));
		fillSiteInformation(data, search);
		return data;
	}

	private void fillSiteInformation(SitesOverviewData data, SearchJSON search) {
		data.setSites(new HashMap<>());
		data.getDevices().stream().filter(Device::isVirtual).forEach(site -> {
			SiteOverviewData siteData = new SiteOverviewData();
			siteData.setId(site.getId());
			data.getSites().put(siteData.getId(), siteData);
			fillWeatherInformation(siteData, site);
			fillAvgTotalInformation(siteData, site, search);
			fillMaxInformation(siteData, site);
		});
	}

	private void fillWeatherInformation(SiteOverviewData siteData, Device site) {
		Optional.ofNullable(OSComponent.getLastDeviceEntry(
						site.getName(), OpenSearchQueries.getDeviceIdQuery(site.getId())))
				.filter(deviceData -> !StringUtils.isEmpty(deviceData.getWeatherSummary())) // No weather stamped for
				// some reason
				.ifPresent(deviceData -> siteData.setWeather(new SiteWeatherData(deviceData)));
	}

	private void fillAvgTotalInformation(SiteOverviewData siteData, Device site, SearchJSON search) {
		search.setDeviceId(site.getId());
		siteData.setAvgTotal(OSComponent.search(search));
	}

	private void fillMaxInformation(SiteOverviewData siteData, Device site) {
		SearchJSON search = new SearchJSON();
		search.setCustomerId(site.getClientId());
		search.setType(OpenSearchConstants.MAX_CURRENT_SEARCH_TYPE);
		search.setBucketSize("3h");
		Date end = TimeUtils.get15mRoundedDate();
		search.setDeviceName(site.getSite());
		search.setEndDate(end.getTime());
		search.setStartDate(end.getTime() - TimeConstants.WEEK);
		siteData.setWeeklyMaxPower(OSComponent.search(search));
	}
}
