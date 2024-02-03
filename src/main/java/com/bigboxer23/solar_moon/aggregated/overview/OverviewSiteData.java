package com.bigboxer23.solar_moon.aggregated.overview;

import com.bigboxer23.solar_moon.aggregated.sites.SiteWeatherData;
import lombok.Data;
import org.opensearch.client.opensearch.core.SearchResponse;

/** */
@Data
public class OverviewSiteData {
	private SearchResponse totalAvg;

	private SearchResponse total;

	private SearchResponse avg;

	private SearchResponse timeSeries;

	private SearchResponse timeSeriesMax;

	private SearchResponse dailyEnergyConsumedTotal;

	private double dailyEnergyConsumedAverage;

	private SearchResponse weeklyMaxPower;

	private SiteWeatherData weather;
}
