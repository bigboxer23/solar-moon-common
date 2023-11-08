package com.bigboxer23.solar_moon;

import com.bigboxer23.solar_moon.data.*;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.utils.StringUtils;

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

	public List<Alarm> filterAlarms(String customerId, String siteId, String deviceId) {
		if (StringUtils.isBlank(customerId)) {
			return Collections.emptyList();
		}
		if (StringUtils.isBlank(siteId) && StringUtils.isBlank(deviceId)) {
			return getAlarms(customerId);
		}
		if (!StringUtils.isBlank(deviceId)) {
			return findAlarmsByDevice(customerId, deviceId);
		}
		return findAlarmsBySite(customerId, siteId);
	}

	public List<Alarm> findAlarmsByDevice(String customerId, String deviceId) {
		return findAlarmsByIndex(Alarm.DEVICE_CUSTOMER_INDEX, deviceId, customerId);
	}

	public List<Alarm> findAlarmsBySite(String customerId, String siteId) {
		return findAlarmsByIndex(Alarm.SITE_CUSTOMER_INDEX, siteId, customerId);
	}

	private List<Alarm> findAlarmsByIndex(String indexName, String partitionId, String sort) {
		return getTable()
				.index(indexName)
				.query(QueryConditional.keyEqualTo(
						builder -> builder.partitionValue(partitionId).sortValue(sort)))
				.stream()
				.flatMap(page -> page.items().stream())
				.collect(Collectors.toList());
	}

	public List<Alarm> getAlarms(String customerId) {
		if (StringUtils.isBlank(customerId)) {
			return Collections.emptyList();
		}
		logger.debug("Fetching all alarms");
		return getTable()
				.index(Alarm.CUSTOMER_INDEX)
				.query(QueryConditional.keyEqualTo(builder -> builder.partitionValue(customerId)))
				.stream()
				.flatMap(page -> page.items().stream())
				.collect(Collectors.toList());
	}

	public Optional<Alarm> updateAlarm(Alarm alarm) {
		if (alarm == null || StringUtils.isBlank(alarm.getCustomerId()) || StringUtils.isBlank(alarm.getAlarmId())) {
			logger.warn("invalid alarm, not updating");
			return Optional.empty();
		}
		logger.warn("Updating alarm: " + alarm.getAlarmId());
		return Optional.ofNullable(getTable().updateItem(builder -> builder.item(alarm)));
	}

	public void deleteAlarm(String alarmId, String customerId) {
		logger.warn("Deleting alarm: " + alarmId);
		getTable().deleteItem(new Alarm(alarmId, customerId));
	}

	public void deleteAlarmsByCustomerId(String customerId) {
		getAlarms(customerId).forEach(alarm -> deleteAlarm(alarm.getAlarmId(), customerId));
	}

	public Optional<Alarm> findAlarmByAlarmId(String alarmId, String customerId) {
		return !StringUtils.isBlank(alarmId) && !StringUtils.isBlank(customerId)
				? Optional.ofNullable(this.getTable().getItem(new Alarm(alarmId, customerId)))
				: Optional.empty();
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
