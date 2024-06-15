package com.bigboxer23.solar_moon.aggregated.sites;

import com.bigboxer23.solar_moon.data.DeviceData;
import lombok.Data;

/** */
@Data
public class SiteWeatherData {
	private String weatherSummary;

	private float temperature;

	private float uvIndex;

	private float precipitationIntensity;

	public SiteWeatherData(DeviceData data) {
		setWeatherSummary(data.getWeatherSummary());
		setUvIndex(data.getUVIndex());
		setTemperature(data.getTemperature());
		setPrecipitationIntensity(data.getPrecipitationIntensity());
	}
}
