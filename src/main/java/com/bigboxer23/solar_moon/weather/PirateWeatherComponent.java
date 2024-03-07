package com.bigboxer23.solar_moon.weather;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceAttribute;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.dynamodb.AbstractDynamodbComponent;
import com.bigboxer23.solar_moon.util.TimeConstants;
import com.bigboxer23.utils.http.OkHttpUtil;
import com.bigboxer23.utils.properties.PropertyUtils;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import okhttp3.Response;
import okhttp3.ResponseBody;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

/** */
public class PirateWeatherComponent extends AbstractDynamodbComponent<StoredWeatherData> {
	private static final String API_KEY = PropertyUtils.getProperty("pirate.weather.api");

	private static final String FORCAST_URL =
			"https://api.pirateweather.net/forecast/" + API_KEY + "/{0}%2C{1}?exclude=minutely%2Chourly%2Cdaily";

	public Optional<PirateWeatherDataResponse> fetchForecastData(double latitude, double longitude) {
		try (Response response =
				OkHttpUtil.getSynchronous(MessageFormat.format(FORCAST_URL, latitude, longitude), null)) {
			ResponseBody responseBody = response.body();
			if (responseBody == null) {
				IComponentRegistry.logger.warn("no forecast body");
				return Optional.empty();
			}
			String body = responseBody.string();
			IComponentRegistry.logger.debug("getForecastData body " + body);
			return Optional.ofNullable(IComponentRegistry.moshi
					.adapter(PirateWeatherDataResponse.class)
					.fromJson(body));
		} catch (IOException e) {
			IComponentRegistry.logger.error("getForcastData", e);
		}
		return Optional.empty();
	}

	public void addWeatherData(DeviceData deviceData, Device site) {
		if (site == null || deviceData == null || (site.getLatitude() == -1 && site.getLongitude() == -1)) {
			logger.debug("Not adding weather data");
			return;
		}
		if (getLastUpdate(site.getLatitude(), site.getLongitude())
				< (System.currentTimeMillis() - (TimeConstants.HOUR + TimeConstants.THIRTY_MINUTES))) {
			logger.warn("Stale weather data, not stamping " + site.getLatitude() + "," + site.getLongitude());
			return;
		}
		if (deviceData.getDate().getTime()
				< (System.currentTimeMillis() - (TimeConstants.HOUR + TimeConstants.THIRTY_MINUTES))) {
			logger.warn("Stale date, not stamping weather " + site.getLatitude() + "," + site.getLongitude());
			return;
		}
		getWeather(site.getLatitude(), site.getLongitude()).ifPresent(w -> {
			deviceData.addAttribute(new DeviceAttribute("weatherSummary", "", w.getSummary()));
			deviceData.addAttribute(new DeviceAttribute("temperature", "", w.getTemperature()));
			deviceData.addAttribute(new DeviceAttribute("cloudCover", "", w.getCloudCover()));
			deviceData.addAttribute(new DeviceAttribute("visibility", "", w.getVisibility()));
			deviceData.addAttribute(new DeviceAttribute("uvIndex", "", w.getUvIndex()));
			deviceData.addAttribute(new DeviceAttribute("precipIntensity", "", w.getPrecipIntensity()));
		});
	}

	public void fetchNewWeather() {
		IComponentRegistry.deviceComponent.getSites().stream()
				.filter(site -> (site.getLatitude() != -1 && site.getLongitude() != -1))
				.forEach(site -> {
					if (getLastUpdate(site.getLatitude(), site.getLongitude())
							>= System.currentTimeMillis() - TimeConstants.FIFTEEN_MINUTES) {
						logger.debug("Not getting new weather, previous data isn't old.");
						return;
					}
					try {
						if (IComponentRegistry.locationComponent.isDay(
										new Date(), site.getLatitude(), site.getLongitude())
								|| LocalDateTime.now().getMinute() == 0) {
							logger.info("fetching weather data for " + site.getLatitude() + "," + site.getLongitude());
							fetchForecastData(site.getLatitude(), site.getLongitude())
									.ifPresent(w ->
											updateWeather(site.getLatitude(), site.getLongitude(), w.getCurrently()));
						}
					} catch (Exception e) {
						logger.error("fetchNewWeather", e);
					}
				});
	}

	public long getLastUpdate(double latitude, double longitude) {
		return this.getTable()
				.query(QueryConditional.keyEqualTo((builder) -> builder.partitionValue(latitude + "," + longitude)))
				.stream()
				.findFirst()
				.flatMap((page) -> page.items().stream().findFirst())
				.map(StoredWeatherData::getTime)
				.orElse(-1L);
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
								.toJson(data),
						new Date().getTime())));
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
