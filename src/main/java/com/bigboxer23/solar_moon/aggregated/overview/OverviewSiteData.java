package com.bigboxer23.solar_moon.aggregated.overview;

import com.bigboxer23.solar_moon.aggregated.sites.SiteWeatherData;
import com.bigboxer23.solar_moon.data.DeviceData;
import lombok.Data;
import org.opensearch.client.opensearch.core.SearchResponse;

/** */
@Data
public class OverviewSiteData {
	private SearchResponse<DeviceData> totalAvg;

	private SearchResponse<DeviceData> total;

	private SearchResponse<DeviceData> avg;

	private SearchResponse<DeviceData> timeSeries;

	private SearchResponse<DeviceData> timeSeriesMax;

	private SearchResponse<DeviceData> dailyEnergyConsumedTotal;

	private double dailyEnergyConsumedAverage;

	private SearchResponse<DeviceData> weeklyMaxPower;

	private SiteWeatherData weather;
}
