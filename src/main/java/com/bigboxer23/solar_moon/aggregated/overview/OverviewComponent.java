package com.bigboxer23.solar_moon.aggregated.overview;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.aggregated.sites.SitesOverviewComponent;
import com.bigboxer23.solar_moon.alarm.AlarmComponent;
import com.bigboxer23.solar_moon.alarm.IAlarmConstants;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.device.DeviceComponent;
import com.bigboxer23.solar_moon.search.OpenSearchComponent;
import com.bigboxer23.solar_moon.search.OpenSearchConstants;
import com.bigboxer23.solar_moon.search.SearchJSON;
import com.bigboxer23.solar_moon.subscription.SubscriptionComponent;
import com.bigboxer23.solar_moon.util.TimeConstants;
import com.bigboxer23.solar_moon.util.TimeUtils;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.utils.StringUtils;

/** */
@Slf4j
public class OverviewComponent implements IComponentRegistry {
	public OverviewData getOverviewData(SearchJSON search) {
		log.info("Fetching overview data");
		search.setIsSite(true);
		OverviewData data = new OverviewData(
				getDeviceComponent().getDevicesForCustomerId(search.getCustomerId()),
				getAlarmComponent().getAlarms(search.getCustomerId()).stream()
						.filter(alarm -> (alarm.getStartDate() > search.getStartDate()
										&& search.getEndDate() > alarm.getStartDate())
								|| alarm.getState() == IAlarmConstants.ACTIVE)
						.toList());
		getSubscriptionComponent().addSubscriptionInformation(data, search.getCustomerId());
		fillSiteInfo(data, search);
		fillInOverallInfo(data, search);
		return data;
	}

	private void fillSiteInfo(OverviewData data, SearchJSON searchJson) {
		if (data == null || data.getDevices() == null) {
			log.warn("data or devices null, cannot fill data");
			return;
		}
		data.setSitesOverviewData(new HashMap<>());
		data.getDevices().stream().filter(Device::isDeviceSite).forEach(site -> data.getSitesOverviewData()
				.put(
						site.getDisplayName(),
						getData(site, searchJson, OpenSearchConstants.TIME_SERIES_WITH_ERRORS_SEARCH_TYPE)));
	}

	private void fillInOverallInfo(OverviewData data, SearchJSON searchJson) {
		if (searchJson == null || StringUtils.isBlank(searchJson.getTimeZone())) {
			return;
		}
		data.setOverall(getData(null, searchJson, OpenSearchConstants.STACKED_TIME_SERIES_SEARCH_TYPE));

		SearchJSON search = new SearchJSON(searchJson);
		Date start = TimeUtils.getStartOfDay(search.getTimeZone());
		search.setEndDate(start.getTime() + TimeConstants.DAY);
		search.setStartDate(start.getTime());
		search.setType(OpenSearchConstants.AVG_TOTAL_SEARCH_TYPE);
		search.setDaylight(true);
		// This is necessary because the period can shift to wk/mo/yr, and always need to get daily
		// for overview as well.
		data.getOverall().setDailyEnergyConsumedTotal(getOSComponent().search(search));
		data.getOverall().setDailyEnergyConsumedAverage(getOSComponent().getAverageEnergyConsumedPerDay(search));
	}

	private OverviewSiteData getData(Device device, SearchJSON searchJson, String timeSeriesType) {
		OverviewSiteData data = new OverviewSiteData();
		SearchJSON search = new SearchJSON(searchJson);
		search.setDeviceId(Optional.ofNullable(device).map(Device::getId).orElse(null));
		search.setType(OpenSearchConstants.TOTAL_SEARCH_TYPE);
		data.setTotal(getOSComponent().search(search));
		search.setType(timeSeriesType);
		data.setTimeSeries(getOSComponent().search(search));
		search.setType(OpenSearchConstants.AVG_SEARCH_TYPE);
		data.setAvg(getOSComponent().search(search));
		if (device != null) {
			data.setWeeklyMaxPower(
					getSitesOverviewComponent().getMaxInformation(device.getId(), searchJson.getCustomerId()));
			data.setWeather(getSitesOverviewComponent().getWeatherInformation(device));
		}
		return data;
	}

	protected DeviceComponent getDeviceComponent() {
		return deviceComponent;
	}

	protected AlarmComponent getAlarmComponent() {
		return alarmComponent;
	}

	protected SubscriptionComponent getSubscriptionComponent() {
		return subscriptionComponent;
	}

	protected OpenSearchComponent getOSComponent() {
		return OSComponent;
	}

	protected SitesOverviewComponent getSitesOverviewComponent() {
		return sitesOverviewComponent;
	}
}
