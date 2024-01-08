package com.bigboxer23.solar_moon.aggregated.sites;

import lombok.Data;
import org.opensearch.client.opensearch.core.SearchResponse;

/** */
@Data
public class SiteOverviewData {
	private String id;

	private SearchResponse weeklyMaxPower;

	private SearchResponse avgTotal;

	private SiteWeatherData weather;

	private String localTime;
}
