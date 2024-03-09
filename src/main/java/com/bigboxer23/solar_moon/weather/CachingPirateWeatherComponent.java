package com.bigboxer23.solar_moon.weather;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/** */
public class CachingPirateWeatherComponent extends PirateWeatherComponent {
	private final LoadingCache<String, Optional<PirateWeatherData>> weatherCache = CacheBuilder.newBuilder()
			.maximumSize(10)
			.expireAfterWrite(10, TimeUnit.SECONDS)
			.build(new CacheLoader<>() {
				public Optional<PirateWeatherData> load(String key) {
					return CachingPirateWeatherComponent.super.getWeather(
							Double.parseDouble(key.substring(0, key.indexOf(":"))),
							Double.parseDouble(key.substring(key.indexOf(":") + 1)));
				}
			});

	private final LoadingCache<String, Long> lastUpdateCache = CacheBuilder.newBuilder()
			.maximumSize(10)
			.expireAfterWrite(10, TimeUnit.SECONDS)
			.build(new CacheLoader<>() {
				public Long load(String key) {
					return CachingPirateWeatherComponent.super.getLastUpdate(
							Double.parseDouble(key.substring(0, key.indexOf(":"))),
							Double.parseDouble(key.substring(key.indexOf(":") + 1)));
				}
			});

	@Override
	public long getLastUpdate(double latitude, double longitude) {
		return lastUpdateCache.getUnchecked(latitude + ":" + longitude);
	}

	@Override
	public Optional<PirateWeatherData> getWeather(double latitude, double longitude) {
		return weatherCache.getUnchecked(latitude + ":" + longitude);
	}
}
