package com.bigboxer23.solar_moon.aggregated.sites;

import com.bigboxer23.solar_moon.data.Alarm;
import com.bigboxer23.solar_moon.data.Device;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.opensearch.client.opensearch.core.SearchResponse;

/** */
@Data
public class SitesSiteData {
	private Device site;

	private SearchResponse weeklyMaxPower;

	private SearchResponse avg;

	private SearchResponse total;

	private SiteWeatherData weather;

	private String localTime; // TODO:

	private List<Device> devices;

	private List<Alarm> alarms;

	private SearchResponse timeSeries;

	private Map<String, SearchResponse> deviceAvg;

	private Map<String, SearchResponse> deviceTotals;

	private Map<String, SearchResponse> deviceTimeSeries;
}
