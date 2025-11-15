package com.bigboxer23.solar_moon.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class TimezoneMapperTest {

	@Test
	public void testLatLngToTimezoneString_newYork() {
		String timezone = TimezoneMapper.latLngToTimezoneString(40.7128, -74.0060);

		assertEquals("America/New_York", timezone);
	}

	@Test
	public void testLatLngToTimezoneString_london() {
		String timezone = TimezoneMapper.latLngToTimezoneString(51.5074, -0.1278);

		assertEquals("Europe/London", timezone);
	}

	@Test
	public void testLatLngToTimezoneString_tokyo() {
		String timezone = TimezoneMapper.latLngToTimezoneString(35.6762, 139.6503);

		assertEquals("Asia/Tokyo", timezone);
	}

	@Test
	public void testLatLngToTimezoneString_sydney() {
		String timezone = TimezoneMapper.latLngToTimezoneString(-33.8688, 151.2093);

		assertEquals("Australia/Sydney", timezone);
	}

	@Test
	public void testLatLngToTimezoneString_losAngeles() {
		String timezone = TimezoneMapper.latLngToTimezoneString(34.0522, -118.2437);

		assertEquals("America/Los_Angeles", timezone);
	}

	@Test
	public void testLatLngToTimezoneString_paris() {
		String timezone = TimezoneMapper.latLngToTimezoneString(48.8566, 2.3522);

		assertEquals("Europe/Paris", timezone);
	}

	@Test
	public void testLatLngToTimezoneString_moscow() {
		String timezone = TimezoneMapper.latLngToTimezoneString(55.7558, 37.6173);

		assertEquals("Europe/Moscow", timezone);
	}

	@Test
	public void testLatLngToTimezoneString_beijing() {
		String timezone = TimezoneMapper.latLngToTimezoneString(39.9042, 116.4074);

		assertEquals("Asia/Shanghai", timezone);
	}

	@Test
	public void testLatLngToTimezoneString_capeTown() {
		String timezone = TimezoneMapper.latLngToTimezoneString(-33.9249, 18.4241);

		assertEquals("Africa/Johannesburg", timezone);
	}

	@Test
	public void testLatLngToTimezoneString_sãoPaulo() {
		String timezone = TimezoneMapper.latLngToTimezoneString(-23.5505, -46.6333);

		assertEquals("America/Sao_Paulo", timezone);
	}

	@Test
	public void testLatLngToTimezoneString_dubai() {
		String timezone = TimezoneMapper.latLngToTimezoneString(25.2048, 55.2708);

		assertEquals("Asia/Dubai", timezone);
	}

	@Test
	public void testLatLngToTimezoneString_chicago() {
		String timezone = TimezoneMapper.latLngToTimezoneString(41.8781, -87.6298);

		assertEquals("America/Chicago", timezone);
	}

	@Test
	public void testLatLngToTimezoneString_denver() {
		String timezone = TimezoneMapper.latLngToTimezoneString(39.7392, -104.9903);

		assertEquals("America/Denver", timezone);
	}

	@Test
	public void testLatLngToTimezoneString_anchorage() {
		String timezone = TimezoneMapper.latLngToTimezoneString(61.2181, -149.9003);

		assertEquals("America/Anchorage", timezone);
	}

	@Test
	public void testLatLngToTimezoneString_honolulu() {
		String timezone = TimezoneMapper.latLngToTimezoneString(21.3099, -157.8581);

		assertEquals("Pacific/Honolulu", timezone);
	}

	@Test
	public void testLatLngToTimezoneString_berlin() {
		String timezone = TimezoneMapper.latLngToTimezoneString(52.5200, 13.4050);

		assertEquals("Europe/Berlin", timezone);
	}

	@Test
	public void testLatLngToTimezoneString_singapore() {
		String timezone = TimezoneMapper.latLngToTimezoneString(1.3521, 103.8198);

		assertEquals("Asia/Singapore", timezone);
	}

	@Test
	public void testLatLngToTimezoneString_auckland() {
		String timezone = TimezoneMapper.latLngToTimezoneString(-36.8485, 174.7633);

		assertEquals("Pacific/Auckland", timezone);
	}

	@Test
	public void testLatLngToTimezoneString_mumbai() {
		String timezone = TimezoneMapper.latLngToTimezoneString(19.0760, 72.8777);

		assertEquals("Asia/Kolkata", timezone);
	}

	@Test
	public void testLatLngToTimezoneString_mexicoCity() {
		String timezone = TimezoneMapper.latLngToTimezoneString(19.4326, -99.1332);

		assertEquals("America/Mexico_City", timezone);
	}

	@Test
	public void testLatLngToTimezoneString_vancouver() {
		String timezone = TimezoneMapper.latLngToTimezoneString(49.2827, -123.1207);

		assertEquals("America/Vancouver", timezone);
	}

	@Test
	public void testLatLngToTimezoneString_toronto() {
		String timezone = TimezoneMapper.latLngToTimezoneString(43.6532, -79.3832);

		assertEquals("America/Toronto", timezone);
	}

	@Test
	public void testLatLngToTimezoneString_buenosAires() {
		String timezone = TimezoneMapper.latLngToTimezoneString(-34.6037, -58.3816);

		assertEquals("America/Argentina/Buenos_Aires", timezone);
	}

	@Test
	public void testLatLngToTimezoneString_northPole() {
		String timezone = TimezoneMapper.latLngToTimezoneString(90.0, 0.0);

		assertNotNull(timezone);
	}

	@Test
	public void testLatLngToTimezoneString_southPole() {
		String timezone = TimezoneMapper.latLngToTimezoneString(-90.0, 0.0);

		assertNotNull(timezone);
	}

	@Test
	public void testLatLngToTimezoneString_equatorPrimeMeridian() {
		String timezone = TimezoneMapper.latLngToTimezoneString(0.0, 0.0);

		assertNotNull(timezone);
	}

	@Test
	public void testLatLngToTimezoneString_internationalDateLine() {
		String timezone = TimezoneMapper.latLngToTimezoneString(0.0, 180.0);

		assertNotNull(timezone);
	}
}
