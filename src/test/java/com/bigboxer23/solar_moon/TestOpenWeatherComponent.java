package com.bigboxer23.solar_moon;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.data.Location;
import com.bigboxer23.solar_moon.data.WeatherSystemData;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/** */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestOpenWeatherComponent implements IComponentRegistry {

	@Test
	@Order(1)
	public void testGetLatLongFromCity() {
		Location location = weatherComponent.getLatLongFromCity("golden valley", "mn", 840);
		assertNotNull(location);
		assertEquals(44.9861176, location.getLat());
		assertEquals(-93.3784618, location.getLon());
		assertFalse(location.isFromCache());
		location = weatherComponent.getLatLongFromCity("golden valley", "mn", 840);
		assertTrue(location.isFromCache());
	}

	@Test
	public void testGetSunriseSunsetFromCityStateCountry() {
		WeatherSystemData data = weatherComponent.getSunriseSunsetFromCityStateCountry("golden valley", "mn", 581);
		assertNotNull(data);
		assertTrue(data.getSunrise() > -1);
		assertTrue(data.getSunset() > -1);
		assertFalse(data.isFromCache());
		data = weatherComponent.getSunriseSunsetFromCityStateCountry("golden valley", "mn", 581);
		assertTrue(data.isFromCache());
		// invalid case
		data = weatherComponent.getSunriseSunsetFromCityStateCountry("golden valley2", "nv", 582);
		assertNull(data);
	}
}
