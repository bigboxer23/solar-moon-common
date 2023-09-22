package com.bigboxer23.solar_moon;

import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.data.WeatherSystemData;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** */
@Component
public class AlarmComponent {

	private static final Logger logger = LoggerFactory.getLogger(AlarmComponent.class);

	private OpenWeatherComponent openWeatherComponent;

	public AlarmComponent(OpenWeatherComponent openWeatherComponent) {
		this.openWeatherComponent = openWeatherComponent;
	}

	public void fireAlarms(List<DeviceData> deviceData) throws IOException {
		logger.debug("checking alarms");
		// TODO: criteria for actually firing
		WeatherSystemData sunriseSunset =
				openWeatherComponent.getSunriseSunsetFromCityStateCountry("golden valley", "mn", 581);
		logger.debug("sunrise/sunset " + sunriseSunset.getSunrise() + "," + sunriseSunset.getSunset());
	}
}
