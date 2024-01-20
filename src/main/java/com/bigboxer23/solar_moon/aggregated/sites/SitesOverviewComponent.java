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
import java.util.Map;
import java.util.Optional;
import org.opensearch.client.opensearch.core.SearchResponse;
import software.amazon.awssdk.utils.StringUtils;

/** */
public class SitesOverviewComponent implements IComponentRegistry {
	public SitesOverviewData getSitesOverviewData(SearchJSON search) {
		SitesOverviewData data = new SitesOverviewData();
		data.setDevices(deviceComponent.getDevicesForCustomerId(search.getCustomerId()));
		fillSiteInformation(data, search);
		return data;
	}

	public SitesSiteData getExtendedSiteOverviewData(String siteId, SearchJSON search) {
		return deviceComponent
				.findDeviceById(siteId)
				.filter(site -> site.getClientId().equalsIgnoreCase(search.getCustomerId()))
				.map(site -> getSiteOverviewData(site, search))
				.map(siteOverview -> fillExtendedSiteOverviewData(siteOverview, search))
				.orElse(null);
	}

	private SitesSiteData getSiteOverviewData(Device site, SearchJSON search) {
		SitesSiteData siteData = new SitesSiteData();
		siteData.setSite(site);
		fillWeatherInformation(siteData, site);
		fillAvgTotalInformation(siteData, site, search);
		fillMaxInformation(siteData, site);
		return siteData;
	}

	private void fillSiteInformation(SitesOverviewData data, SearchJSON search) {
		data.setSites(new HashMap<>());
		data.getDevices().stream().filter(Device::isVirtual).forEach(site -> {
			data.getSites().put(site.getId(), getSiteOverviewData(site, search));
		});
	}

	private void fillWeatherInformation(SitesSiteData siteData, Device site) {
		Optional.ofNullable(OSComponent.getLastDeviceEntry(
						site.getName(), OpenSearchQueries.getDeviceIdQuery(site.getId())))
				.filter(deviceData -> !StringUtils.isEmpty(deviceData.getWeatherSummary())) // No weather stamped for
				// some reason
				.ifPresent(deviceData -> siteData.setWeather(new SiteWeatherData(deviceData)));
	}

	private void fillAvgTotalInformation(SitesSiteData siteData, Device site, SearchJSON search) {
		search.setDeviceId(site.getId());
		search.setType(OpenSearchConstants.TOTAL_SEARCH_TYPE);
		siteData.setTotal(OSComponent.search(search));
		search.setType(OpenSearchConstants.AVG_SEARCH_TYPE);
		search.setDaylight(true);
		siteData.setAvg(OSComponent.search(search));
	}

	private void fillMaxInformation(SitesSiteData siteData, Device site) {
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

	private SitesSiteData fillExtendedSiteOverviewData(SitesSiteData siteOverview, SearchJSON search) {
		siteOverview.setDevices(deviceComponent.getDevicesBySite(
				search.getCustomerId(), siteOverview.getSite().getDisplayName()));
		siteOverview.setAlarms(
				alarmComponent
						.findAlarmsBySite(
								search.getCustomerId(), siteOverview.getSite().getId())
						.stream()
						.filter(alarm -> alarm.getStartDate() > search.getStartDate())
						.toList());
		fillSiteTimeSeries(siteOverview, search);
		fillDevicesTimeSeries(siteOverview, search);
		fillDevicesAverageTotal(siteOverview, search);
		return siteOverview;
	}

	private void fillSiteTimeSeries(SitesSiteData siteOverview, SearchJSON search) {
		search.setDaylight(false);
		search.setDeviceId(
				siteOverview.getSite().isSubtraction() ? siteOverview.getSite().getId() : null);
		search.setSite(
				siteOverview.getSite().isSubtraction()
						? null
						: siteOverview.getSite().getDisplayName());
		search.setType(
				siteOverview.getSite().isSubtraction()
						? OpenSearchConstants.TIME_SERIES_SEARCH_TYPE
						: OpenSearchConstants.STACKED_TIME_SERIES_SEARCH_TYPE);
		siteOverview.setTimeSeries(OSComponent.search(search));
	}

	private void fillDevicesAverageTotal(SitesSiteData siteOverview, SearchJSON search) {
		siteOverview.setDeviceAvg(new HashMap<>());
		fillDeviceMap(siteOverview, search, true, OpenSearchConstants.AVG_SEARCH_TYPE, siteOverview.getDeviceAvg());
		siteOverview.setDeviceTotals(new HashMap<>());
		fillDeviceMap(
				siteOverview, search, false, OpenSearchConstants.TOTAL_SEARCH_TYPE, siteOverview.getDeviceTotals());
	}

	private void fillDevicesTimeSeries(SitesSiteData siteOverview, SearchJSON search) {
		siteOverview.setDeviceTimeSeries(new HashMap<>());
		fillDeviceMap(
				siteOverview,
				search,
				false,
				OpenSearchConstants.TIME_SERIES_SEARCH_TYPE,
				siteOverview.getDeviceTimeSeries());
	}

	private void fillDeviceMap(
			SitesSiteData siteOverview,
			SearchJSON search,
			boolean daylight,
			String type,
			Map<String, SearchResponse> map) {
		search.setSite(null);
		search.setDaylight(daylight);
		search.setType(type);
		siteOverview.getDevices().stream().filter(d -> !d.isVirtual()).forEach(d -> {
			search.setDeviceId(d.getId());
			map.put(d.getId(), OSComponent.search(search));
		});
	}
}
