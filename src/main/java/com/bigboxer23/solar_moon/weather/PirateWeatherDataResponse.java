package com.bigboxer23.solar_moon.weather;

import lombok.Data;

/** */
@Data
public class PirateWeatherDataResponse {
	private double latitude;
	private double longitude;
	private String timezone;
	private int offset;
	private int elevation;
	private PirateWeatherData currently;
}
