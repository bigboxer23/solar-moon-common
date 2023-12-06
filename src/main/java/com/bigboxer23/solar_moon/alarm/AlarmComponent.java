package com.bigboxer23.solar_moon.alarm;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.*;
import com.bigboxer23.solar_moon.device.DeviceComponent;
import com.bigboxer23.solar_moon.dynamodb.AbstractDynamodbComponent;
import com.bigboxer23.solar_moon.notifications.AlarmEmailTemplateContent;
import com.bigboxer23.solar_moon.notifications.NotificationComponent;
import com.bigboxer23.solar_moon.search.OpenSearchComponent;
import com.bigboxer23.solar_moon.search.OpenSearchQueries;
import com.bigboxer23.solar_moon.util.TimeConstants;
import com.bigboxer23.solar_moon.util.TokenGenerator;
import com.bigboxer23.solar_moon.web.TransactionUtil;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.utils.StringUtils;

/** */
public class AlarmComponent extends AbstractDynamodbComponent<Alarm> {

	private static final Logger logger = LoggerFactory.getLogger(AlarmComponent.class);

	private final DeviceComponent deviceComponent;

	private final OpenSearchComponent OSComponent;

	private final NotificationComponent notificationComponent;

	public AlarmComponent(
			DeviceComponent deviceComponent,
			OpenSearchComponent OSComponent,
			NotificationComponent notificationComponent) {
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
		IComponentRegistry.deviceUpdateComponent.update(device.getDeviceId(), System.currentTimeMillis());
		getMostRecentAlarm(device.getDeviceId())
				.filter(alarm -> alarm.getState() == 1)
				.ifPresent(alarm -> {
					logger.warn("Resolving alarm for "
							+ device.getName()
							+ " "
							+ device.getDate().getTime());
					alarm.setState(0);
					alarm.setEmailed(1);
					alarm.setEndDate(new Date().getTime());
					updateAlarm(alarm);
				});
		// TODO: inspect data to see if looks "normal"

	}

	public Optional<Alarm> alarmConditionDetected(String customerId, String deviceId, String site, String content) {
		logger.warn("Alarm condition detected: " + deviceId + " " + content);
		List<Alarm> alarms = findAlarmsByDevice(customerId, deviceId);
		Alarm alarm = alarms.stream().filter(a -> a.getState() == 1).findAny().orElseGet(() -> {
			Alarm newAlarm = new Alarm(
					TokenGenerator.generateNewToken(),
					customerId,
					deviceId,
					deviceComponent
							.findDeviceByDeviceName(customerId, site)
							.map(Device::getId)
							.orElse(null));
			newAlarm.setStartDate(System.currentTimeMillis());
			newAlarm.setState(1);
			return newAlarm;
		});
		alarm.setLastUpdate(System.currentTimeMillis());
		alarm.setMessage(content);
		return updateAlarm(alarm);
	}

	public void sendPendingNotifications() {
		logger.info("Checking for pending notifications");
		Map<String, List<Alarm>> customerSortedAlarms = new HashMap<>();
		findNonEmailedAlarms().forEach(alarm -> {
			if (!customerSortedAlarms.containsKey(alarm.getCustomerId())) {
				customerSortedAlarms.put(alarm.getCustomerId(), new ArrayList<>());
			}
			customerSortedAlarms.get(alarm.getCustomerId()).add(alarm);
		});

		customerSortedAlarms.forEach((customerId, alarms) -> {
			TransactionUtil.updateCustomerId(customerId);
			logger.info("Starting sending notifications");
			AlarmEmailTemplateContent alarmEmail = new AlarmEmailTemplateContent(customerId, alarms);
			if (alarmEmail.isNotificationEnabled()) {
				notificationComponent.sendNotification(alarmEmail.getRecipient(), alarmEmail.getSubject(), alarmEmail);
			} else {
				logger.warn("New notification detected, but not sending email as requested.");
			}
			alarms.forEach(a -> {
				a.setEmailed(System.currentTimeMillis());
				updateAlarm(a);
			});
		});
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

	public List<Alarm> findNonEmailedAlarms(String customerId) {
		return getTable()
				.index(Alarm.EMAILED_CUSTOMER_INDEX)
				.query(QueryConditional.keyEqualTo(
						builder -> builder.partitionValue(0).sortValue(customerId)))
				.stream()
				.flatMap(page -> page.items().stream())
				.toList();
	}

	public List<Alarm> findNonEmailedAlarms() {
		return getTable()
				.index(Alarm.EMAILED_CUSTOMER_INDEX)
				.query(QueryConditional.keyEqualTo(builder -> builder.partitionValue(0)))
				.stream()
				.flatMap(page -> page.items().stream())
				.toList();
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

	public void deleteAlarmByDeviceId(String customerId, String deviceId) {
		findAlarmsByDevice(customerId, deviceId).forEach(a -> deleteAlarm(a.getAlarmId(), a.getCustomerId()));
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

	public List<Alarm> quickCheckDevices() {
		logger.info("Checking for non-responsive devices");
		List<Alarm> alarms = new ArrayList<>();
		IComponentRegistry.deviceUpdateComponent
				.queryByTimeRange(System.currentTimeMillis() - TimeConstants.FORTY_FIVE_MINUTES)
				.forEach(d -> deviceComponent
						.findDeviceById(d.getDeviceId())
						.filter(d2 -> !d2.isDisabled())
						.flatMap(d2 -> {
							TransactionUtil.addDeviceId(d2.getId());
							TransactionUtil.updateCustomerId(d2.getClientId());
							return alarmConditionDetected(
									d2.getClientId(),
									d2.getId(),
									d2.getSite(),
									"No data recently from device. " + " Last data: " + d.getLastUpdate());
						})
						.ifPresent(alarms::add));
		return alarms;
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
					data.getDeviceId(),
					data.getSite(),
					"No data recently from device.  Last data: "
							+ data.getDate().getTime());
		}
		return Optional.empty();
	}

	public void cleanupOldAlarms() {
		long yearAgo = System.currentTimeMillis() - TimeConstants.YEAR;
		deleteAlarmsByStateAndDate(0, yearAgo);
		deleteAlarmsByStateAndDate(1, yearAgo);
	}

	private void deleteAlarmsByStateAndDate(int state, long deleteOlderThan) {
		getTable()
				.index(Alarm.STATE_STARTDATE_INDEX)
				.query(QueryConditional.sortLessThan(
						builder -> builder.partitionValue(state).sortValue(deleteOlderThan)))
				.stream()
				.flatMap(page -> page.items().stream())
				.forEach(a -> {
					System.out.println("here");
					deleteAlarm(a.getAlarmId(), a.getCustomerId());
				});
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
