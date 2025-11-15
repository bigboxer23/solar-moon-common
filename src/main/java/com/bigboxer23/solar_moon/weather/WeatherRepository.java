package com.bigboxer23.solar_moon.weather;

import java.util.Optional;

public interface WeatherRepository {

	Optional<StoredWeatherData> findByLatitudeLongitude(double latitude, double longitude);

	StoredWeatherData add(StoredWeatherData weatherData);

	Optional<StoredWeatherData> update(StoredWeatherData weatherData);
}
