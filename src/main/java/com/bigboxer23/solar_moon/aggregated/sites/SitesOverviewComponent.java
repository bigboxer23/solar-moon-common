package com.bigboxer23.solar_moon.aggregated.sites;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.search.OpenSearchConstants;
import com.bigboxer23.solar_moon.search.OpenSearchQueries;
import com.bigboxer23.solar_moon.search.SearchJSON;
import com.bigboxer23.solar_moon.util.TimeConstants;
import com.bigboxer23.solar_moon.util.TimeUtils;
import java.time.format.DateTimeFormatter;
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
		fillTimeInformation(siteData, site);
		fillWeatherInformation(siteData, site);
		fillAvgTotalInformation(siteData, site, search);
		siteData.setWeeklyMaxPower(getMaxInformation(site.getId(), site.getClientId()));
		return siteData;
	}

	private void fillSiteInformation(SitesOverviewData data, SearchJSON search) {
		data.setSites(new HashMap<>());
		data.getDevices().stream().filter(Device::isDeviceSite).forEach(site -> data.getSites()
				.put(site.getId(), getSiteOverviewData(site, search)));
	}

	private void fillWeatherInformation(SitesSiteData siteData, Device site) {
		Optional.ofNullable(OSComponent.getLastDeviceEntry(
						site.getName(), OpenSearchQueries.getDeviceIdQuery(site.getId())))
				.filter(deviceData -> !StringUtils.isEmpty(deviceData.getWeatherSummary())) // No weather stamped for
				// some reason
				.ifPresent(deviceData -> siteData.setWeather(new SiteWeatherData(deviceData)));
	}

	public void fillTimeInformation(SitesSiteData siteData, Device site) {
		locationComponent
				.getLocalTimeString(site.getLatitude(), site.getLongitude())
				.map(time -> time.format(DateTimeFormatter.ofPattern("h:mm a")))
				.ifPresent(siteData::setLocalTime);
	}

	private void fillAvgTotalInformation(SitesSiteData siteData, Device site, SearchJSON search) {
		SearchJSON searchJSON = new SearchJSON(search);
		searchJSON.setDeviceId(site.getId());
		searchJSON.setType(OpenSearchConstants.TOTAL_SEARCH_TYPE);
		siteData.setTotal(OSComponent.search(searchJSON));

		searchJSON.setType(OpenSearchConstants.AVG_SEARCH_TYPE);
		searchJSON.setDaylight(true);
		siteData.setAvg(OSComponent.search(searchJSON));
	}

	private SearchResponse getMaxInformation(String deviceId, String customerId) {
		SearchJSON search = new SearchJSON();
		search.setCustomerId(customerId);
		search.setType(OpenSearchConstants.MAX_CURRENT_SEARCH_TYPE);
		search.setBucketSize("3h");
		Date end = TimeUtils.get15mRoundedDate();
		search.setDeviceId(deviceId);
		search.setEndDate(end.getTime());
		search.setStartDate(end.getTime() - TimeConstants.WEEK);
		return OSComponent.search(search);
	}

	private SitesSiteData fillExtendedSiteOverviewData(SitesSiteData siteOverview, SearchJSON search) {
		siteOverview.setDevices(deviceComponent.getDevicesBySite(
				search.getCustomerId(), siteOverview.getSite().getDisplayName()));
		siteOverview.setAlarms(
				alarmComponent
						.findAlarmsBySite(
								search.getCustomerId(), siteOverview.getSite().getId())
						.stream()
						.filter(alarm -> alarm.getStartDate() > search.getStartDate()
								&& search.getEndDate() > alarm.getStartDate())
						.toList());
		fillSiteTimeSeries(siteOverview, search);
		fillDevicesTimeSeries(siteOverview, search);
		fillDevicesAverageTotal(siteOverview, search);
		return siteOverview;
	}

	private void fillSiteTimeSeries(SitesSiteData siteOverview, SearchJSON search) {
		SearchJSON searchJson = new SearchJSON(search);
		searchJson.setDaylight(false);
		searchJson.setDeviceId(
				siteOverview.getSite().isSubtraction() ? siteOverview.getSite().getId() : null);
		searchJson.setSite(
				siteOverview.getSite().isSubtraction()
						? null
						: siteOverview.getSite().getDisplayName());
		searchJson.setType(
				siteOverview.getSite().isSubtraction()
						? OpenSearchConstants.TIME_SERIES_SEARCH_TYPE
						: OpenSearchConstants.STACKED_TIME_SERIES_SEARCH_TYPE);
		siteOverview.setTimeSeries(OSComponent.search(searchJson));
	}

	private void fillDevicesAverageTotal(SitesSiteData siteOverview, SearchJSON search) {
		siteOverview.setDeviceAvg(new HashMap<>());
		fillDeviceMap(siteOverview, search, true, OpenSearchConstants.AVG_SEARCH_TYPE, siteOverview.getDeviceAvg());
		siteOverview.setDeviceTotals(new HashMap<>());
		fillDeviceMap(
				siteOverview, search, false, OpenSearchConstants.TOTAL_SEARCH_TYPE, siteOverview.getDeviceTotals());
		siteOverview.setDeviceWeeklyMaxPower(new HashMap<>());
		siteOverview.getDevices().stream().filter(d -> !d.isDeviceSite()).forEach(d -> siteOverview
				.getDeviceWeeklyMaxPower()
				.put(d.getId(), getMaxInformation(d.getId(), d.getClientId())));
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
		SearchJSON searchJson = new SearchJSON(search);
		searchJson.setSite(null);
		searchJson.setDaylight(daylight);
		searchJson.setType(type);
		siteOverview.getDevices().stream().filter(d -> !d.isDeviceSite()).forEach(d -> {
			searchJson.setDeviceId(d.getId());
			map.put(d.getId(), OSComponent.search(searchJson));
		});
	}
}
