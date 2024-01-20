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
		search.setVirtual(true);
		OverviewData data = new OverviewData(
				deviceComponent.getDevicesForCustomerId(search.getCustomerId()),
				alarmComponent.getAlarms(search.getCustomerId()));
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
		SearchJSON search = new SearchJSON(searchJson);
		data.setOverall(getData(null, search));
		Date start = TimeUtils.getStartOfDay(search.getTimeZone());
		search.setEndDate(start.getTime() + TimeConstants.DAY);
		search.setStartDate(start.getTime());
		search.setType(OpenSearchConstants.AVG_TOTAL_SEARCH_TYPE);
		// This is necessary because the period can shift to wk/mo/yr, and always need to get daily
		// for overview as well.
		data.getOverall().setDailyEnergyConsumedTotal(OSComponent.search(search));
		data.getOverall().setDailyEnergyConsumedAverage(OSComponent.getAverageEnergyConsumedPerDay(search));
	}

	private OverviewSiteData getData(String site, SearchJSON searchJson) {
		OverviewSiteData data = new OverviewSiteData();
		SearchJSON search = new SearchJSON(searchJson);
		search.setDeviceName(site);
		search.setType(OpenSearchConstants.TOTAL_SEARCH_TYPE);
		data.setTotal(OSComponent.search(search));
		search.setType(OpenSearchConstants.TIME_SERIES_SEARCH_TYPE);
		data.setTimeSeries(OSComponent.search(search));
		search.setDaylight(true);
		search.setType(OpenSearchConstants.AVG_SEARCH_TYPE);
		data.setAvg(OSComponent.search(search));
		return data;
	}
}
