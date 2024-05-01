package com.bigboxer23.solar_moon.aggregated.sites;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.alarm.IAlarmConstants;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.search.OpenSearchConstants;
import com.bigboxer23.solar_moon.search.OpenSearchQueries;
import com.bigboxer23.solar_moon.search.SearchJSON;
import com.bigboxer23.solar_moon.util.TimeConstants;
import com.bigboxer23.solar_moon.util.TimeUtils;
import com.bigboxer23.solar_moon.web.TransactionUtil;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.opensearch.client.opensearch.core.SearchResponse;
import software.amazon.awssdk.utils.StringUtils;

/** */
public class SitesOverviewComponent implements IComponentRegistry {
	public SitesSiteData getExtendedSiteOverviewData(String siteId, SearchJSON search) {
		TransactionUtil.addDeviceId(siteId, siteId);
		logger.info("Fetching site data");
		SitesSiteData data = deviceComponent
				.findDeviceById(siteId, search.getCustomerId())
				.map(site -> getSiteOverviewData(site, search))
				.map(siteOverview -> fillExtendedSiteOverviewData(siteOverview, search))
				.orElse(null);
		subscriptionComponent.addTrialDate(data, search.getCustomerId());
		return data;
	}

	private SitesSiteData getSiteOverviewData(Device site, SearchJSON search) {
		SitesSiteData siteData = new SitesSiteData();
		siteData.setSite(site);
		fillLocalTimeInformation(siteData, site);
		siteData.setWeather(getWeatherInformation(site));
		fillAvgTotalInformation(siteData, site, search);
		siteData.setWeeklyMaxPower(getMaxInformation(site.getId(), site.getClientId()));
		return siteData;
	}

	private void fillSiteInformation(SitesOverviewData data, SearchJSON search) {
		data.setSites(new HashMap<>());
		data.getDevices().stream().filter(Device::isDeviceSite).forEach(site -> data.getSites()
				.put(site.getId(), getSiteOverviewData(site, search)));
	}

	public SiteWeatherData getWeatherInformation(Device site) {
		return Optional.ofNullable(
						OSComponent.getLastDeviceEntry(site.getId(), OpenSearchQueries.getDeviceIdQuery(site.getId())))
				.filter(deviceData -> !StringUtils.isEmpty(deviceData.getWeatherSummary())) // No weather stamped for
				// some reason
				.map(SiteWeatherData::new)
				.orElse(null);
	}

	public void fillLocalTimeInformation(SitesSiteData siteData, Device site) {
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

	public SearchResponse getMaxInformation(String deviceId, String customerId) {
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
		siteOverview.setDevices(deviceComponent.getDevicesBySiteId(
				search.getCustomerId(), siteOverview.getSite().getSiteId()));
		siteOverview.setAlarms(
				alarmComponent
						.findAlarmsBySite(
								search.getCustomerId(), siteOverview.getSite().getId())
						.stream()
						.filter(alarm -> (alarm.getStartDate() > search.getStartDate()
										&& search.getEndDate() > alarm.getStartDate())
								|| alarm.getState() == IAlarmConstants.ACTIVE)
						.toList());
		fillSiteTimeSeries(siteOverview, search);
		fillDevicesTimeSeries(siteOverview, search);
		fillDevicesAverageTotal(siteOverview, search);
		return siteOverview;
	}

	protected void fillSiteTimeSeries(SitesSiteData siteOverview, SearchJSON search) {
		SearchJSON searchJson = new SearchJSON(search);
		searchJson.setDeviceId(
				siteOverview.getSite().isSubtraction() ? siteOverview.getSite().getId() : null);
		searchJson.setSiteId(
				siteOverview.getSite().isSubtraction()
						? null
						: siteOverview.getSite().getId());
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
				search.isDaylight(),
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
		searchJson.setSiteId(null);
		searchJson.setDaylight(daylight);
		searchJson.setType(type);
		siteOverview.getDevices().stream().filter(d -> !d.isDeviceSite()).forEach(d -> {
			searchJson.setDeviceId(d.getId());
			map.put(d.getId(), OSComponent.search(searchJson));
		});
	}
}
