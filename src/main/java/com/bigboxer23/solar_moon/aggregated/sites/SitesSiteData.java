package com.bigboxer23.solar_moon.aggregated.sites;

import com.bigboxer23.solar_moon.data.Alarm;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.data.Subscription;
import com.bigboxer23.solar_moon.subscription.IHasSubscription;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.opensearch.client.opensearch.core.SearchResponse;

/** */
@Data
public class SitesSiteData implements IHasSubscription {
	private Device site;

	private SearchResponse<DeviceData> weeklyMaxPower;

	private SearchResponse<DeviceData> avg;

	private SearchResponse<DeviceData> total;

	private SiteWeatherData weather;

	private String localTime;

	private List<Device> devices;

	private List<Alarm> alarms;

	private SearchResponse<DeviceData> timeSeries;

	private Map<String, SearchResponse<DeviceData>> deviceAvg;

	private Map<String, SearchResponse<DeviceData>> deviceTotals;

	private Map<String, SearchResponse<DeviceData>> deviceTimeSeries;

	private Map<String, SearchResponse<DeviceData>> deviceWeeklyMaxPower;

	private Subscription subscription;
}
