package com.bigboxer23.solar_moon.aggregated.sites;

import com.bigboxer23.solar_moon.data.Alarm;
import com.bigboxer23.solar_moon.data.Device;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.opensearch.client.opensearch.core.SearchResponse;

/** */
@Data
public class SiteOverviewData {
	private Device site;

	private SearchResponse weeklyMaxPower;

	private SearchResponse avgTotal;

	private SiteWeatherData weather;

	private String localTime;

	private List<Device> devices;

	private List<Alarm> alarms;

	private SearchResponse timeSeries;

	private Map<String, SearchResponse> deviceAvgTotals;

	private Map<String, SearchResponse> deviceTimeSeries;
}
