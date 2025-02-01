package com.bigboxer23.solar_moon.weather;

import com.bigboxer23.solar_moon.data.Location;
import com.bigboxer23.solar_moon.data.WeatherData;
import com.bigboxer23.solar_moon.data.WeatherSystemData;
import com.bigboxer23.utils.http.OkHttpUtil;
import com.bigboxer23.utils.properties.PropertyUtils;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;

/** */
// @Component
@Slf4j
public class OpenWeatherComponent {
	private static final String kOpenWeatherMapUrl =
			"https://api.openweathermap.org/data/2.5/weather?lat={0}&lon={1}&APPID={2}";

	public static final String kOpenWeatherMapCityToLatLong =
			"http://api.openweathermap.org/geo/1.0/direct?q={0}&limit=2&appid={1}";

	private String openWeatherMapAPIKey;

	private final Moshi moshi = new Moshi.Builder().build();

	public OpenWeatherComponent() {
		openWeatherMapAPIKey = PropertyUtils.getProperty("openweathermap.api");
	}

	private Map<String, WeatherSystemData> weatherCache =
			new HashMap<>(); // use map instead of cache... lambda doesn't have cache really
	//			CacheBuilder.newBuilder().expireAfterAccess(3, TimeUnit.DAYS).build();

	private Map<String, Location> locationCache = new HashMap<>();

	//			CacheBuilder.newBuilder().expireAfterAccess(30, TimeUnit.DAYS).build();

	protected Location getLatLongFromCity(String city, String state, int countryCode) {
		return Optional.ofNullable(locationCache.get(city + state + countryCode))
				.map(loc -> {
					log.debug("retrieving lat/long (cached) from " + city + " " + state);
					loc.setFromCache(true);
					return loc;
				})
				.orElseGet(() -> {
					log.info("retrieving lat/long from " + city + " " + state);
					try (Response response = OkHttpUtil.getSynchronous(
							MessageFormat.format(
									kOpenWeatherMapCityToLatLong,
									city + "," + state + "," + countryCode,
									openWeatherMapAPIKey),
							null)) {
						String body = response.body().string();
						log.debug("lat/long body " + body);
						JsonAdapter<List<Location>> jsonAdapter =
								moshi.adapter(Types.newParameterizedType(List.class, Location.class));
						Location location = Optional.ofNullable(jsonAdapter.fromJson(body))
								.map(loc -> {
									if (loc.isEmpty()) {
										return null;
									}
									return loc.stream()
											.filter(location1 -> location1.getState() != null)
											.findAny()
											.orElse(null);
								})
								.orElse(null);
						if (location != null) {
							locationCache.put(city + state + countryCode, location);
						}
						return location;
					} catch (IOException e) {
						log.error("getLatLongFromCity", e);
					}
					return null;
				});
	}

	protected WeatherSystemData getSunriseSunset(double latitude, double longitude) {
		return Optional.ofNullable(weatherCache.get(latitude + ":" + longitude))
				.map(loc -> {
					log.debug("retrieving weather (cached) from " + latitude + ":" + longitude);
					loc.setFromCache(true);
					return loc;
				})
				.orElseGet(() -> {
					log.debug("Fetching OpenWeatherMap data");
					try (Response response = OkHttpUtil.getSynchronous(
							MessageFormat.format(kOpenWeatherMapUrl, latitude, longitude, openWeatherMapAPIKey),
							null)) {
						String body = response.body().string();
						log.debug("weather body " + body);
						WeatherSystemData data = Optional.ofNullable(
										moshi.adapter(WeatherData.class).fromJson(body))
								.map(WeatherData::getSys)
								.orElse(null);
						if (data != null) {
							weatherCache.put(latitude + ":" + longitude, data);
						}
						return data;
					} catch (IOException e) {
						log.error("getSunriseSunset", e);
					}
					return null;
				});
	}

	public WeatherSystemData getSunriseSunsetFromCityStateCountry(String city, String state, int countryCode) {
		return Optional.ofNullable(getLatLongFromCity(city, state, countryCode))
				.map(location -> getSunriseSunset(location.getLat(), location.getLon()))
				.orElse(null);
	}
}
