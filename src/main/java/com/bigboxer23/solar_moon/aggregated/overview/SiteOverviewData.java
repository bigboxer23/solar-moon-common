package com.bigboxer23.solar_moon.aggregated.overview;

import lombok.Data;
import org.opensearch.client.opensearch.core.SearchResponse;

/** */
@Data
public class SiteOverviewData {
	private SearchResponse totalAvg;

	private SearchResponse total;

	private SearchResponse avg;

	private SearchResponse timeSeries;

	private SearchResponse dailyEnergyConsumedTotal;

	private double dailyEnergyConsumedAverage;
}
