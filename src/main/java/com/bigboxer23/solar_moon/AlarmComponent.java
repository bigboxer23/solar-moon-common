package com.bigboxer23.solar_moon;

import com.bigboxer23.solar_moon.data.*;
import com.bigboxer23.solar_moon.notifications.AlarmEmailTemplateContent;
import com.bigboxer23.solar_moon.notifications.NotificationComponent;
import com.bigboxer23.solar_moon.open_search.OpenSearchComponent;
import com.bigboxer23.solar_moon.open_search.OpenSearchQueries;
import com.bigboxer23.solar_moon.util.TimeConstants;
import com.bigboxer23.solar_moon.util.TokenGenerator;
import com.bigboxer23.solar_moon.web.TransactionUtil;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.utils.StringUtils;

/** */
// @Component
public class AlarmComponent extends AbstractDynamodbComponent<Alarm> {

	private static final Logger logger = LoggerFactory.getLogger(AlarmComponent.class);

	private final OpenWeatherComponent openWeatherComponent;

	private final DeviceComponent deviceComponent;

	private final OpenSearchComponent OSComponent;

	private final NotificationComponent notificationComponent;

	public AlarmComponent(
			OpenWeatherComponent openWeatherComponent,
			DeviceComponent deviceComponent,
			OpenSearchComponent OSComponent,
			NotificationComponent notificationComponent) {
		this.openWeatherComponent = openWeatherComponent;
		this.deviceComponent = deviceComponent;
		this.OSComponent = OSComponent;
		this.notificationComponent = notificationComponent;
	}

	/*public void fireAlarms(List<DeviceData> deviceData) throws IOException {
		logger.debug("checking alarms");
		// TODO: criteria for actually firing
		WeatherSystemData sunriseSunset =
				openWeatherComponent.getSunriseSunsetFromCityStateCountry("golden valley", "mn", 581);
		logger.debug("sunrise/sunset " + sunriseSunset.getSunrise() + "," + sunriseSunset.getSunset());
	}*/

	public Optional<Alarm> getMostRecentAlarm(String deviceId) {
		List<Alarm> alarms = getTable()
				.index(Alarm.DEVICEID_STARTDATE_INDEX)
				.query(theBuilder -> theBuilder
						.limit(1)
						.scanIndexForward(false)
						.queryConditional(QueryConditional.keyEqualTo(builder -> builder.partitionValue(deviceId))))
				.stream()
				.findFirst()
				.map(Page::items)
				.orElse(Collections.emptyList());
		return !alarms.isEmpty() ? Optional.ofNullable(alarms.get(0)) : Optional.empty();
	}

	public void resolveActiveAlarms(DeviceData device) {
		getMostRecentAlarm(device.getDeviceId())
				.filter(alarm -> alarm.getState() == 1)
				.ifPresent(alarm -> {
					alarm.setState(0);
					alarm.setEndDate(new Date().getTime());
					updateAlarm(alarm);
				});
		// TODO: inspect data to see if looks "normal"

	}

	public Optional<Alarm> alarmConditionDetected(String customerId, DeviceData deviceData, String content) {
		logger.warn("Alarm condition detected: " + customerId + " " + deviceData.getDeviceId() + " " + content);
		List<Alarm> alarms = findAlarmsByDevice(customerId, deviceData.getDeviceId());
		Alarm alarm = alarms.stream().filter(a -> a.getState() == 1).findAny().orElseGet(() -> {
			Alarm newAlarm = new Alarm(
					TokenGenerator.generateNewToken(),
					customerId,
					deviceData.getDeviceId(),
					deviceComponent
							.findDeviceByName(customerId, deviceData.getSite())
							.map(Device::getId)
							.orElse(null));
			newAlarm.setStartDate(System.currentTimeMillis());
			newAlarm.setState(1);
			AlarmEmailTemplateContent alarmEmail = new AlarmEmailTemplateContent(customerId, deviceData, newAlarm);
			if (alarmEmail.isNotificationEnabled()) {
				notificationComponent.sendNotification(alarmEmail.getRecipient(), alarmEmail.getSubject(), alarmEmail);
			} else {
				logger.warn("New notification detected, but not sending email"
						+ " as requested "
						+ customerId
						+ " "
						+ deviceData.getDeviceId());
			}
			return newAlarm;
		});
		alarm.setLastUpdate(System.currentTimeMillis());
		alarm.setMessage(content);
		return updateAlarm(alarm);
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
				.toList();
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
				.toList();
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

	public Optional<Alarm> checkDevice(Device device) {
		if (device == null) {
			logger.warn("Null device, can't check.");
			return Optional.empty();
		}
		DeviceData data =
				OSComponent.getLastDeviceEntry(device.getName(), OpenSearchQueries.getDeviceIdQuery(device.getId()));
		if (data == null) {
			logger.debug("likely new device with no data " + device.getId());
			return Optional.empty();
		}
		TransactionUtil.addDeviceId(device.getId());
		TransactionUtil.updateCustomerId(device.getClientId());
		if (!device.isDisabled()
				&& data.getDate().getTime() < new Date(System.currentTimeMillis() - TimeConstants.HOUR).getTime()) {
			return alarmConditionDetected(
					data.getCustomerId(),
					data,
					"No data recently from device.  Last data: "
							+ new SimpleDateFormat(MeterConstants.DATE_PATTERN).format(data.getDate()));
		}
		TransactionUtil.clear();
		return Optional.empty();
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
