package com.bigboxer23.solar_moon.aggregated.overview;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.search.OpenSearchConstants;
import com.bigboxer23.solar_moon.search.SearchJSON;
import com.bigboxer23.solar_moon.util.TimeConstants;
import com.bigboxer23.solar_moon.util.TimeUtils;
import java.util.Date;
import java.util.HashMap;
import software.amazon.awssdk.utils.StringUtils;

/** */
public class OverviewComponent implements IComponentRegistry {
	public OverviewData getOverviewData(SearchJSON search) {
		OverviewData data = new OverviewData();
		data.setAlarms(alarmComponent.getAlarms(search.getCustomerId()));
		data.setDevices(deviceComponent.getDevicesForCustomerId(search.getCustomerId()));
		search.setVirtual(true);
		data.setOverall(getData(null, search));
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
		data.getDevices().stream().filter(Device::isVirtual).forEach(site -> data.getSitesOverviewData()
				.put(site.getDisplayName(), getData(site.getDisplayName(), searchJson)));
	}

	private void fillInOverallInfo(OverviewData data, SearchJSON searchJson) {
		if (searchJson == null || StringUtils.isBlank(searchJson.getTimeZone())) {
			return;
		}
		Date start = TimeUtils.getStartOfDay(searchJson.getTimeZone());
		searchJson.setDeviceName(null);
		searchJson.setEndDate(start.getTime() + TimeConstants.DAY);
		searchJson.setStartDate(start.getTime());
		searchJson.setType(OpenSearchConstants.AVG_TOTAL_SEARCH_TYPE);
		// This is necessary because the period can shift to wk/mo/yr, and always need to get daily
		// for overview as well.
		data.getOverall().setDailyEnergyConsumedTotal(OSComponent.search(searchJson));
		data.getOverall().setDailyEnergyConsumedAverage(OSComponent.getAverageEnergyConsumedPerDay(searchJson));
	}

	private SiteOverviewData getData(String site, SearchJSON searchJson) {
		SiteOverviewData data = new SiteOverviewData();
		searchJson.setDeviceName(site);
		searchJson.setType(OpenSearchConstants.TOTAL_SEARCH_TYPE);
		data.setTotal(OSComponent.search(searchJson));
		searchJson.setType(OpenSearchConstants.TIME_SERIES_SEARCH_TYPE);
		data.setTimeSeries(OSComponent.search(searchJson));
		searchJson.setDaylight(true);
		searchJson.setType(OpenSearchConstants.AVG_SEARCH_TYPE);
		data.setAvg(OSComponent.search(searchJson));
		return data;
	}
}
