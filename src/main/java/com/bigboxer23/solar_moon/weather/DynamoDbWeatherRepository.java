package com.bigboxer23.solar_moon.weather;

import com.bigboxer23.solar_moon.dynamodb.AuditableAbstractDynamodbRepository;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

@Slf4j
public class DynamoDbWeatherRepository extends AuditableAbstractDynamodbRepository<StoredWeatherData>
		implements WeatherRepository {

	@Override
	public Optional<StoredWeatherData> findByLatitudeLongitude(double latitude, double longitude) {
		return getTable()
				.query(QueryConditional.keyEqualTo((builder) -> builder.partitionValue(latitude + "," + longitude)))
				.stream()
				.findFirst()
				.flatMap((page) -> page.items().stream().findFirst());
	}

	@Override
	protected String getTableName() {
		return "weather";
	}

	@Override
	protected Class<StoredWeatherData> getObjectClass() {
		return StoredWeatherData.class;
	}
}
