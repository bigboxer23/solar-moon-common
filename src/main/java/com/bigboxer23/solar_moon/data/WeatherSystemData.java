package com.bigboxer23.solar_moon.data;

import lombok.Data;

/** */
@Data
public class WeatherSystemData {
	private int sunrise;

	private int sunset;

	private boolean fromCache = false;
}
