package com.bigboxer23.solar_moon.weather;

import lombok.Data;

/** */
@Data
public class PirateWeatherData {
	private long time;
	private String summary;
	private long nearestStormDistance;
	private long nearestStormBearing;
	private double precipIntensity;
	private double precipProbability;
	private double precipIntensityError;
	private String precipType;
	private double temperature;
	private double apparentTemperature;
	private double dewPoint;
	private double humidity;
	private double pressure;
	private double windSpeed;
	private double windGust;
	private double windBearing;
	private double cloudCover;
	private double uvIndex;
	private double visibility;
	private double ozone;
}
