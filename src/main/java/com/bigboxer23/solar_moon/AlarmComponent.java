package com.bigboxer23.solar_moon;

import com.bigboxer23.solar_moon.data.Alarm;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.data.WeatherSystemData;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** */
// @Component
public class AlarmComponent extends AbstractDynamodbComponent<Alarm> {

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

	@Override
	protected String getTableName() {
		return "alarms";
	}

	@Override
	protected Class<Alarm> getObjectClass() {
		return Alarm.class;
	}
}
