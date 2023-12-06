package com.bigboxer23.solar_moon.weather;

import com.bigboxer23.solar_moon.util.TimeConstants;
import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/** */
@DynamoDbBean
@Data
public class StoredWeatherData {
	private String latitudeLongitude;

	private String weather;

	private long time;

	private long expiration;

	public StoredWeatherData() {}

	public StoredWeatherData(double latitude, double longitude, String weather, long time) {
		setLatitudeLongitude(latitude + "," + longitude);
		setWeather(weather);
		setTime(time);
		setExpiration((System.currentTimeMillis() + TimeConstants.THIRTY_DAYS) / 1000);
	}

	@DynamoDbPartitionKey
	public String getLatitudeLongitude() {
		return latitudeLongitude;
	}
}
