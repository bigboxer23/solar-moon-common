package com.bigboxer23.solar_moon.weather;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceAttribute;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.dynamodb.AbstractDynamodbComponent;
import com.bigboxer23.solar_moon.ingest.MeterConstants;
import com.bigboxer23.solar_moon.util.TimeConstants;
import com.bigboxer23.solar_moon.web.TransactionUtil;
import com.bigboxer23.utils.command.RetryingCommand;
import com.bigboxer23.utils.http.OkHttpUtil;
import com.bigboxer23.utils.properties.PropertyUtils;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import okhttp3.Response;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

/** */
public class PirateWeatherComponent extends AbstractDynamodbComponent<StoredWeatherData> {
	private static final String API_KEY = PropertyUtils.getProperty("pirate.weather.api");

	private static final String FORCAST_URL =
			"https://api.pirateweather.net/forecast/" + API_KEY + "/{0}%2C{1}?exclude=minutely%2Chourly%2Cdaily";

	public Optional<PirateWeatherDataResponse> fetchForecastData(double latitude, double longitude) {
		try (Response response =
				OkHttpUtil.getSynchronous(MessageFormat.format(FORCAST_URL, latitude, longitude), null)) {
			return OkHttpUtil.getBody(response, PirateWeatherDataResponse.class);
		} catch (IOException e) {
			IComponentRegistry.logger.error("getForecastData", e);
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
			deviceData.addAttribute(new DeviceAttribute(MeterConstants.WEATHER_SUMMARY, "", w.getSummary()));
			deviceData.addAttribute(new DeviceAttribute(MeterConstants.TEMPERATURE, "", w.getTemperature()));
			deviceData.addAttribute(new DeviceAttribute(MeterConstants.CLOUD_COVER, "", w.getCloudCover()));
			deviceData.addAttribute(new DeviceAttribute(MeterConstants.VISIBILITY, "", w.getVisibility()));
			deviceData.addAttribute(new DeviceAttribute(MeterConstants.UV_INDEX, "", w.getUvIndex()));
			deviceData.addAttribute(
					new DeviceAttribute(MeterConstants.PRECIPITATION_INTENSITY, "", w.getPrecipIntensity()));
		});
	}

	public void fetchNewWeather() {
		boolean isTopOfHour = LocalDateTime.now().getMinute() == 0;
		Map<String, Device> sites = IComponentRegistry.deviceComponent.getSites().stream()
				.filter(site -> (site.getLatitude() != -1 && site.getLongitude() != -1))
				.collect(Collectors.toMap(
						(site) -> site.getLatitude() + ":" + site.getLongitude(),
						Function.identity(),
						(existing, replacement) -> existing));
		sites.values().forEach(site -> {
			TransactionUtil.updateCustomerId(site.getClientId());
			long lastUpdate = getLastUpdate(site.getLatitude(), site.getLongitude());
			if (lastUpdate >= System.currentTimeMillis() - TimeConstants.FIFTEEN_MINUTES) {
				logger.info("Not getting new weather, previous data isn't old. "
						+ site.getLatitude()
						+ ":"
						+ site.getLongitude());
				return;
			}
			try {
				if (IComponentRegistry.locationComponent
								.isDay(new Date(), site.getLatitude(), site.getLongitude())
								.orElse(true)
						|| isTopOfHour) {
					logger.info("fetching weather data for " + site.getLatitude() + "," + site.getLongitude());
					Optional<PirateWeatherDataResponse> response = RetryingCommand.execute(
							() -> {
								Optional<PirateWeatherDataResponse> r =
										fetchForecastData(site.getLatitude(), site.getLongitude());
								if (r.isEmpty()) {
									throw new IOException(
											"fetchNewWeather " + site.getLatitude() + ":" + site.getLongitude());
								}
								return r;
							},
							site.getLatitude() + ":" + site.getLongitude(),
							2);
					response.ifPresent(w -> updateWeather(site.getLatitude(), site.getLongitude(), w.getCurrently()));
				}
			} catch (Exception e) {
				logger.error("fetchNewWeather " + site.getLatitude() + ":" + site.getLongitude(), e);
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
			logger.warn("invalid weather, not storing " + latitude + "," + longitude);
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
