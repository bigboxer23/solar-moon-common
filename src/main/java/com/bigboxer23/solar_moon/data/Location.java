package com.bigboxer23.solar_moon.data;

import lombok.Data;

/** */
@Data
public class Location {
	private double lat;

	private double lon;

	private String country;

	private String state;

	private boolean fromCache = false;
}
