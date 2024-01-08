package com.bigboxer23.solar_moon.aggregated.sites;

import com.bigboxer23.solar_moon.data.DeviceData;
import lombok.Data;

/** */
@Data
public class SiteWeatherData {
	private String weatherSummary;

	private float temperature;

	private float uvIndex;

	public SiteWeatherData(DeviceData data) {
		setWeatherSummary(data.getWeatherSummary());
		setUvIndex(data.getUVIndex());
		setTemperature(data.getTemperature());
	}
}
