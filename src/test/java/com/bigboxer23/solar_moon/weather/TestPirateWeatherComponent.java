package com.bigboxer23.solar_moon.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.TestConstants;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** */
public class TestPirateWeatherComponent implements IComponentRegistry, TestConstants {
	@Test
	public void getForcastData() {
		Optional<PirateWeatherDataResponse> weather = weatherComponent.fetchForecastData(testLatitude, testLongitude);
		assertTrue(weather.isPresent());
	}

	@Test
	public void testCRU() {
		Optional<PirateWeatherDataResponse> weather = weatherComponent.fetchForecastData(testLatitude, testLongitude);
		weather.ifPresent(w -> {
			weatherComponent.updateWeather(testLatitude, testLongitude, w.getCurrently());
			Optional<PirateWeatherData> data = weatherComponent.getWeather(testLatitude, testLongitude);
			assertTrue(data.isPresent());
			assertEquals(w.getCurrently().getTime(), data.get().getTime());
		});
	}

	/*@Test
	public void fetchNewWeather() {
		weatherComponent.fetchNewWeather();
	}*/
}
