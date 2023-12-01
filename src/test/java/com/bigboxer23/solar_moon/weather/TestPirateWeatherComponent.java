package com.bigboxer23.solar_moon.weather;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.TestConstants;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** */
public class TestPirateWeatherComponent implements IComponentRegistry, TestConstants {
	@Test
	public void getForcastData() {
		Optional<PirateWeatherDataResponse> weather = weatherComponent.getForcastData(testLatitude, testLongitude);
		assertTrue(weather.isPresent());
	}
}
