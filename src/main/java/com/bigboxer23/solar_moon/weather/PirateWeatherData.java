package com.bigboxer23.solar_moon.weather;

import lombok.Data;

/** */
@Data
public class PirateWeatherData {
	private long time;
	private String summary;
	private String icon;
	private double nearestStormDistance;
	private double nearestStormBearing;
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

	/**
	 * Pirate weather occasionally returns some whack data. Normalize it, so we don't write
	 * incorrect data.
	 *
	 * @return
	 */
	public double getUvIndex() {
		return Math.min(20, Math.max(0, uvIndex));
	}
}
