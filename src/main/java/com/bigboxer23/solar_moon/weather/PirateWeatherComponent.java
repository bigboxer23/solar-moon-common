package com.bigboxer23.solar_moon.weather;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.dynamodb.AbstractDynamodbComponent;
import com.bigboxer23.solar_moon.lambda.utils.PropertyUtils;
import com.bigboxer23.utils.http.OkHttpUtil;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Optional;
import okhttp3.Response;
import okhttp3.ResponseBody;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

/** */
public class PirateWeatherComponent extends AbstractDynamodbComponent<StoredWeatherData> {
	private static final String API_KEY = PropertyUtils.getProperty("pirate.weather.api");

	private static final String FORCAST_URL =
			"https://api.pirateweather.net/forecast/" + API_KEY + "/{0}%2C{1}?exclude=minutely%2Chourly%2Cdaily";

	public Optional<PirateWeatherDataResponse> getForcastData(double latitude, double longitude) {
		try (Response response =
				OkHttpUtil.getSynchronous(MessageFormat.format(FORCAST_URL, latitude, longitude), null)) {
			ResponseBody responseBody = response.body();
			if (responseBody == null) {
				IComponentRegistry.logger.warn("no forcast body");
				return Optional.empty();
			}
			String body = responseBody.string();
			IComponentRegistry.logger.debug("getForcastData body " + body);
			return Optional.ofNullable(IComponentRegistry.moshi
					.adapter(PirateWeatherDataResponse.class)
					.fromJson(body));
		} catch (IOException e) {
			IComponentRegistry.logger.error("getForcastData", e);
		}
		return Optional.empty();
	}

	public void addWeatherData(DeviceData deviceData, Device site) {
		// TODO:
	}

	public Optional<PirateWeatherData> getWeather(double latitude, double longitude) {
		StoredWeatherData data = this.getTable()
				.query(QueryConditional.keyEqualTo((builder) -> builder.partitionValue(latitude + "," + longitude)))
				.stream()
				.findFirst()
				.flatMap((page) -> page.items().stream().findFirst())
				.orElse(null);
		if (data == null) {
			return Optional.empty();
		}
		try {
			return Optional.ofNullable(
					IComponentRegistry.moshi.adapter(PirateWeatherData.class).fromJson(data.getWeather()));
		} catch (IOException e) {
			logger.error("getWeather", e);
			return Optional.empty();
		}
	}

	public void updateWeather(double latitude, double longitude, PirateWeatherData data) {
		if (data == null) {
			logger.warn("invalid weather, not storing");
			return;
		}
		logger.info("Updating weather for: " + latitude + "," + longitude);
		getTable()
				.updateItem(builder -> builder.item(new StoredWeatherData(
						latitude,
						longitude,
						IComponentRegistry.moshi
								.adapter(PirateWeatherData.class)
								.toJson(data))));
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
