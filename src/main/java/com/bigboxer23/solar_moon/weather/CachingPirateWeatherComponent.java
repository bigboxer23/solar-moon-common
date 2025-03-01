package com.bigboxer23.solar_moon.weather;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.time.Duration;
import java.util.Optional;

/** */
public class CachingPirateWeatherComponent extends PirateWeatherComponent {
	private final LoadingCache<String, Optional<PirateWeatherData>> weatherCache = Caffeine.newBuilder()
			.maximumSize(10)
			.expireAfterWrite(Duration.ofSeconds(10))
			.build(key -> CachingPirateWeatherComponent.super.getWeather(
					Double.parseDouble(key.substring(0, key.indexOf(":"))),
					Double.parseDouble(key.substring(key.indexOf(":") + 1))));

	private final LoadingCache<String, Long> lastUpdateCache = Caffeine.newBuilder()
			.maximumSize(10)
			.expireAfterWrite(Duration.ofSeconds(10))
			.build(key -> CachingPirateWeatherComponent.super.getLastUpdate(
					Double.parseDouble(key.substring(0, key.indexOf(":"))),
					Double.parseDouble(key.substring(key.indexOf(":") + 1))));

	@Override
	public long getLastUpdate(double latitude, double longitude) {
		Long timestamp = lastUpdateCache.getIfPresent(latitude + ":" + longitude);
		return timestamp != null ? timestamp : 0L;
	}

	@Override
	public Optional<PirateWeatherData> getWeather(double latitude, double longitude) {
		return weatherCache.get(latitude + ":" + longitude);
	}
}
