package com.bigboxer23.solar_moon.weather;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PirateWeatherComponentTest {

	@Mock
	private WeatherRepository mockRepository;

	private TestablePirateWeatherComponent weatherComponent;

	private static final double LATITUDE = 37.7749;
	private static final double LONGITUDE = -122.4194;
	private static final String WEATHER_SUMMARY = "Partly Cloudy";
	private static final String ICON = "partly-cloudy-day";
	private static final double TEMPERATURE = 72.5;
	private static final double CLOUD_COVER = 0.4;
	private static final double VISIBILITY = 10.0;
	private static final double UV_INDEX = 5.0;
	private static final double PRECIP_INTENSITY = 0.0;

	private static class TestablePirateWeatherComponent extends PirateWeatherComponent {
		private final WeatherRepository repository;

		public TestablePirateWeatherComponent(WeatherRepository repository) {
			this.repository = repository;
		}

		@Override
		protected WeatherRepository getRepository() {
			return repository;
		}
	}

	@BeforeEach
	void setUp() {
		weatherComponent = new TestablePirateWeatherComponent(mockRepository);
	}

	@Test
	void testGetLastUpdate_withExistingWeatherData_returnsTimestamp() {
		long expectedTime = System.currentTimeMillis();
		StoredWeatherData weatherData = createTestStoredWeatherData(expectedTime);
		when(mockRepository.findByLatitudeLongitude(LATITUDE, LONGITUDE)).thenReturn(Optional.of(weatherData));

		long result = weatherComponent.getLastUpdate(LATITUDE, LONGITUDE);

		assertEquals(expectedTime, result);
		verify(mockRepository).findByLatitudeLongitude(LATITUDE, LONGITUDE);
	}

	@Test
	void testGetLastUpdate_withNoWeatherData_returnsMinusOne() {
		when(mockRepository.findByLatitudeLongitude(LATITUDE, LONGITUDE)).thenReturn(Optional.empty());

		long result = weatherComponent.getLastUpdate(LATITUDE, LONGITUDE);

		assertEquals(-1L, result);
		verify(mockRepository).findByLatitudeLongitude(LATITUDE, LONGITUDE);
	}

	@Test
	void testGetWeather_withValidStoredData_returnsPirateWeatherData() {
		StoredWeatherData weatherData = createTestStoredWeatherData(System.currentTimeMillis());
		when(mockRepository.findByLatitudeLongitude(LATITUDE, LONGITUDE)).thenReturn(Optional.of(weatherData));

		Optional<PirateWeatherData> result = weatherComponent.getWeather(LATITUDE, LONGITUDE);

		assertTrue(result.isPresent());
		assertEquals(WEATHER_SUMMARY, result.get().getSummary());
		assertEquals(ICON, result.get().getIcon());
		verify(mockRepository).findByLatitudeLongitude(LATITUDE, LONGITUDE);
	}

	@Test
	void testGetWeather_withNoStoredData_returnsEmpty() {
		when(mockRepository.findByLatitudeLongitude(LATITUDE, LONGITUDE)).thenReturn(Optional.empty());

		Optional<PirateWeatherData> result = weatherComponent.getWeather(LATITUDE, LONGITUDE);

		assertFalse(result.isPresent());
		verify(mockRepository).findByLatitudeLongitude(LATITUDE, LONGITUDE);
	}

	@Test
	void testUpdateWeather_withValidData_updatesRepository() {
		PirateWeatherData pirateWeatherData = createTestPirateWeatherData();
		when(mockRepository.update(any(StoredWeatherData.class))).thenReturn(Optional.of(new StoredWeatherData()));

		weatherComponent.updateWeather(LATITUDE, LONGITUDE, pirateWeatherData);

		verify(mockRepository).update(any(StoredWeatherData.class));
	}

	@Test
	void testUpdateWeather_withNullData_doesNotUpdate() {
		weatherComponent.updateWeather(LATITUDE, LONGITUDE, null);

		verify(mockRepository, never()).update(any(StoredWeatherData.class));
	}

	@Test
	void testAddWeatherData_withNullSite_doesNotAddWeatherData() {
		DeviceData deviceData = new DeviceData();

		weatherComponent.addWeatherData(deviceData, null);

		assertEquals("", deviceData.getWeatherSummary());
		verify(mockRepository, never()).findByLatitudeLongitude(anyDouble(), anyDouble());
	}

	@Test
	void testAddWeatherData_withNullDeviceData_doesNotAddWeatherData() {
		Device site = createTestSite();

		weatherComponent.addWeatherData(null, site);

		verify(mockRepository, never()).findByLatitudeLongitude(anyDouble(), anyDouble());
	}

	@Test
	void testAddWeatherData_withInvalidLatLong_doesNotAddWeatherData() {
		Device site = createTestSite();
		site.setLatitude(-1);
		site.setLongitude(-1);
		DeviceData deviceData = new DeviceData();

		weatherComponent.addWeatherData(deviceData, site);

		assertEquals("", deviceData.getWeatherSummary());
		verify(mockRepository, never()).findByLatitudeLongitude(anyDouble(), anyDouble());
	}

	private StoredWeatherData createTestStoredWeatherData(long time) {
		PirateWeatherData pirateWeatherData = createTestPirateWeatherData();
		String weatherJson = String.format(
				"{\"summary\":\"%s\",\"icon\":\"%s\",\"temperature\":%.1f,\"cloudCover\":%.1f,\"visibility\":%.1f,\"uvIndex\":%.1f,\"precipIntensity\":%.1f}",
				WEATHER_SUMMARY, ICON, TEMPERATURE, CLOUD_COVER, VISIBILITY, UV_INDEX, PRECIP_INTENSITY);
		return new StoredWeatherData(LATITUDE, LONGITUDE, weatherJson, time);
	}

	private PirateWeatherData createTestPirateWeatherData() {
		PirateWeatherData data = new PirateWeatherData();
		data.setSummary(WEATHER_SUMMARY);
		data.setIcon(ICON);
		data.setTemperature(TEMPERATURE);
		data.setCloudCover(CLOUD_COVER);
		data.setVisibility(VISIBILITY);
		data.setUvIndex(UV_INDEX);
		data.setPrecipIntensity(PRECIP_INTENSITY);
		return data;
	}

	private Device createTestSite() {
		Device site = new Device();
		site.setLatitude(LATITUDE);
		site.setLongitude(LONGITUDE);
		return site;
	}
}
