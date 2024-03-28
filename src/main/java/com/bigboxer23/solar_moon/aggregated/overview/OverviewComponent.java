package com.bigboxer23.solar_moon.aggregated.overview;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.search.OpenSearchConstants;
import com.bigboxer23.solar_moon.search.SearchJSON;
import com.bigboxer23.solar_moon.util.TimeConstants;
import com.bigboxer23.solar_moon.util.TimeUtils;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;
import software.amazon.awssdk.utils.StringUtils;

/** */
public class OverviewComponent implements IComponentRegistry {
	public OverviewData getOverviewData(SearchJSON search) {
		logger.info("Fetching overview data");
		search.setIsSite(true);
		OverviewData data = new OverviewData(
				deviceComponent.getDevicesForCustomerId(search.getCustomerId()),
				alarmComponent.getAlarms(search.getCustomerId()).stream()
						.filter(alarm -> (alarm.getStartDate() > search.getStartDate()
										&& search.getEndDate() > alarm.getStartDate())
								|| alarm.getEndDate() == 0)
						.toList());

		fillSiteInfo(data, search);
		fillInOverallInfo(data, search);
		return data;
	}

	private void fillSiteInfo(OverviewData data, SearchJSON searchJson) {
		if (data == null || data.getDevices() == null) {
			logger.warn("data or devices null, cannot fill data");
			return;
		}
		data.setSitesOverviewData(new HashMap<>());
		data.getDevices().stream().filter(Device::isDeviceSite).forEach(site -> data.getSitesOverviewData()
				.put(site.getDisplayName(), getData(site, searchJson, OpenSearchConstants.TIME_SERIES_SEARCH_TYPE)));
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
		// This is necessary because the period can shift to wk/mo/yr, and always need to get daily
		// for overview as well.
		data.getOverall().setDailyEnergyConsumedTotal(OSComponent.search(search));
		data.getOverall().setDailyEnergyConsumedAverage(OSComponent.getAverageEnergyConsumedPerDay(search));
	}

	private OverviewSiteData getData(Device device, SearchJSON searchJson, String timeSeriesType) {
		OverviewSiteData data = new OverviewSiteData();
		SearchJSON search = new SearchJSON(searchJson);
		search.setDeviceId(Optional.ofNullable(device).map(Device::getId).orElse(null));
		search.setType(OpenSearchConstants.TOTAL_SEARCH_TYPE);
		data.setTotal(OSComponent.search(search));
		search.setType(timeSeriesType);
		data.setTimeSeries(OSComponent.search(search));
		search.setType(OpenSearchConstants.AVG_SEARCH_TYPE);
		data.setAvg(OSComponent.search(search));
		if (device != null) {
			data.setWeeklyMaxPower(
					sitesOverviewComponent.getMaxInformation(device.getId(), searchJson.getCustomerId()));
			data.setWeather(sitesOverviewComponent.getWeatherInformation(device));
		}
		return data;
	}
}
