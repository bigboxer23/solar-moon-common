package com.bigboxer23.solar_moon.aggregated.sites;

import com.bigboxer23.solar_moon.data.Alarm;
import com.bigboxer23.solar_moon.data.Device;
import java.util.List;
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
}
