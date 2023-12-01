package com.bigboxer23.solar_moon.weather;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.lambda.utils.PropertyUtils;
import com.bigboxer23.utils.http.OkHttpUtil;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Optional;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** */
public class PirateWeatherComponent {
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
}
