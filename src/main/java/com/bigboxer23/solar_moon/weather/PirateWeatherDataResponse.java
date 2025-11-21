package com.bigboxer23.solar_moon.weather;

import lombok.Data;

/** */
@Data
public class PirateWeatherDataResponse {
	private double latitude;
	private double longitude;
	private String timezone;
	private double offset;
	private double elevation;
	private PirateWeatherData currently;
}
