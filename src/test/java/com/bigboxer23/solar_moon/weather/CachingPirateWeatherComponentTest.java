package com.bigboxer23.solar_moon.weather;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.IComponentRegistry;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CachingPirateWeatherComponentTest {

	@Mock
	private WeatherRepository mockRepository;

	private CachingPirateWeatherComponent weatherComponent;

	private static final double LATITUDE = 37.7749;
	private static final double LONGITUDE = -122.4194;

	private class TestableCachingPirateWeatherComponent extends CachingPirateWeatherComponent {
		@Override
		protected WeatherRepository getRepository() {
			return mockRepository;
		}
	}

	@BeforeEach
	void setUp() {
		weatherComponent = new TestableCachingPirateWeatherComponent();
	}

	@Test
	void testGetLastUpdate_repeatedLookupsHitRepositoryOnce() {
		when(mockRepository.findByLatitudeLongitude(LATITUDE, LONGITUDE)).thenReturn(Optional.of(storedWeather(1000L)));

		assertEquals(1000L, weatherComponent.getLastUpdate(LATITUDE, LONGITUDE));
		assertEquals(1000L, weatherComponent.getLastUpdate(LATITUDE, LONGITUDE));

		verify(mockRepository, times(1)).findByLatitudeLongitude(LATITUDE, LONGITUDE);
	}

	@Test
	void testGetLastUpdate_withNoStoredData_returnsNegativeOne() {
		when(mockRepository.findByLatitudeLongitude(LATITUDE, LONGITUDE)).thenReturn(Optional.empty());

		assertEquals(-1L, weatherComponent.getLastUpdate(LATITUDE, LONGITUDE));
	}

	@Test
	void testGetLastUpdate_cachesPerCoordinatePair() {
		when(mockRepository.findByLatitudeLongitude(LATITUDE, LONGITUDE)).thenReturn(Optional.of(storedWeather(1000L)));
		when(mockRepository.findByLatitudeLongitude(10.0, 20.0)).thenReturn(Optional.of(storedWeather(2000L)));

		assertEquals(1000L, weatherComponent.getLastUpdate(LATITUDE, LONGITUDE));
		assertEquals(2000L, weatherComponent.getLastUpdate(10.0, 20.0));

		verify(mockRepository).findByLatitudeLongitude(LATITUDE, LONGITUDE);
		verify(mockRepository).findByLatitudeLongitude(10.0, 20.0);
	}

	@Test
	void testGetWeather_repeatedLookupsHitRepositoryOnce() {
		when(mockRepository.findByLatitudeLongitude(LATITUDE, LONGITUDE)).thenReturn(Optional.of(storedWeather(1000L)));

		Optional<PirateWeatherData> first = weatherComponent.getWeather(LATITUDE, LONGITUDE);
		Optional<PirateWeatherData> second = weatherComponent.getWeather(LATITUDE, LONGITUDE);

		assertTrue(first.isPresent());
		assertSame(first.get(), second.get());
		verify(mockRepository, times(1)).findByLatitudeLongitude(LATITUDE, LONGITUDE);
	}

	@Test
	void testGetWeather_withNoStoredData_returnsEmptyAndCachesMiss() {
		when(mockRepository.findByLatitudeLongitude(LATITUDE, LONGITUDE)).thenReturn(Optional.empty());

		assertTrue(weatherComponent.getWeather(LATITUDE, LONGITUDE).isEmpty());
		assertTrue(weatherComponent.getWeather(LATITUDE, LONGITUDE).isEmpty());

		verify(mockRepository, times(1)).findByLatitudeLongitude(LATITUDE, LONGITUDE);
	}

	@Test
	void testGetWeather_withUnparseableJson_returnsEmpty() {
		StoredWeatherData stored = new StoredWeatherData(LATITUDE, LONGITUDE, "not json", 1000L);
		when(mockRepository.findByLatitudeLongitude(LATITUDE, LONGITUDE)).thenReturn(Optional.of(stored));

		assertTrue(weatherComponent.getWeather(LATITUDE, LONGITUDE).isEmpty());
	}

	@Test
	void testGetWeather_deserializesStoredPayload() {
		when(mockRepository.findByLatitudeLongitude(LATITUDE, LONGITUDE)).thenReturn(Optional.of(storedWeather(1000L)));

		PirateWeatherData data =
				weatherComponent.getWeather(LATITUDE, LONGITUDE).orElseThrow();

		assertEquals("Partly Cloudy", data.getSummary());
		assertEquals(72.5, data.getTemperature());
	}

	private StoredWeatherData storedWeather(long time) {
		PirateWeatherData data = new PirateWeatherData();
		data.setSummary("Partly Cloudy");
		data.setIcon("partly-cloudy-day");
		data.setTemperature(72.5);
		return new StoredWeatherData(
				LATITUDE,
				LONGITUDE,
				IComponentRegistry.moshi.adapter(PirateWeatherData.class).toJson(data),
				time);
	}
}
