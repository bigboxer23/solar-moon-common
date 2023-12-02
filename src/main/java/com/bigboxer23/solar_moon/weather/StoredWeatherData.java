package com.bigboxer23.solar_moon.weather;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/** */
@DynamoDbBean
@Data
public class StoredWeatherData {
	private String latitudeLongitude;

	private String weather;

	public StoredWeatherData() {}

	public StoredWeatherData(double latitude, double longitude, String weather) {
		setLatitudeLongitude(latitude + "," + longitude);
		setWeather(weather);
	}

	@DynamoDbPartitionKey
	public String getLatitudeLongitude() {
		return latitudeLongitude;
	}
}
