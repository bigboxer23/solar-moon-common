package com.bigboxer23.solar_moon.aggregated.overview;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.TestConstants;
import com.bigboxer23.solar_moon.search.OpenSearchConstants;
import com.bigboxer23.solar_moon.search.SearchJSON;
import com.bigboxer23.solar_moon.util.TimeConstants;
import com.bigboxer23.solar_moon.util.TimeUtils;
import java.util.Date;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.utils.StringUtils;

/** */
public class TestOverviewComponent implements IComponentRegistry, TestConstants {
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

	@Test
	public void tmp() {
		{
			SearchJSON searchJson = new SearchJSON();
			searchJson.setTimeZone("America/Chicago");
			searchJson.setCustomerId("98719340-c001-70f5-6f96-64d758660b24");
			searchJson.setEndDate(1726499441607L);
			searchJson.setStartDate(1726462800000L);
			searchJson.setBucketSize("30m");
			searchJson.setDaylight(true);
			OverviewData data = IComponentRegistry.overviewComponent.getOverviewData(searchJson);
			System.out.println(data);
			/*"deviceId": null,
			"endDate": 1726499441607,
			"startDate": 1726462800000,
			"timeZone": "America/Chicago",
			"bucketSize": "30m",
			"type": "avgTotal",
			"siteId": null,
			"daylight": true*/

		}
	}
}
