package com.bigboxer23.solar_moon.aggregated.overview;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.TestConstants;
import com.bigboxer23.solar_moon.search.OpenSearchConstants;
import com.bigboxer23.solar_moon.search.SearchJSON;
import com.bigboxer23.solar_moon.util.TimeConstants;
import com.bigboxer23.solar_moon.util.TimeUtils;
import java.util.Date;
import software.amazon.awssdk.utils.StringUtils;

/** */
public class OverviewComponentIntegrationTest implements IComponentRegistry, TestConstants {
	public void fillInOverallInfo() {
		SearchJSON searchJson = new SearchJSON();
		searchJson.setTimeZone("America/Chicago");
		searchJson.setCustomerId(CUSTOMER_ID);
		if (StringUtils.isBlank(searchJson.getTimeZone())) {
			return;
		}
		Date start = TimeUtils.getStartOfDay(searchJson.getTimeZone());
		searchJson.setDeviceName(null);
		searchJson.setEndDate(start.getTime() + TimeConstants.DAY);
		searchJson.setStartDate(start.getTime());
		searchJson.setType(OpenSearchConstants.AVG_TOTAL_SEARCH_TYPE);
		System.out.println("Avg: " + OSComponent.getAverageEnergyConsumedPerDay(searchJson));
		// This is necessary because the period can shift to wk/mo/yr, and always need to get daily
		// for overview as well.
		// data.getOverall().setDailyEnergyConsumedTotal(OSComponent.search(searchJson));
		// data.getOverall().setDailyEnergyConsumedAverage(OSComponent.getAverageEnergyConsumedPerDay(searchJson));
	}
}
