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
public class AlarmComponent extends AbstractDynamodbComponent<Alarm> implements IAlarmConstants {
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
		return !alarms.isEmpty() ? Optional.ofNullable(alarms.getFirst()) : Optional.empty();
	}

	public void resolveActiveAlarms(DeviceData deviceData) {
		if (deviceData.getDate() == null
				|| deviceData.getDate().getTime() <= System.currentTimeMillis() - TimeConstants.HOUR) {
			logger.warn("old data, not adding device update or resolving active alarms " + deviceData.getDate());
			return;
		}
		IComponentRegistry.deviceUpdateComponent.update(deviceData.getDeviceId());
		if (deviceData.getTotalRealPower() <= 0) {
			logger.warn("not resolving alarm, no real power reported");
			return;
		}
		getMostRecentAlarm(deviceData.getDeviceId())
				.filter(alarm -> alarm.getState() == ACTIVE)
				.ifPresent(alarm -> {
					logger.warn("Resolving alarm for "
							+ deviceData.getName()
							+ " "
							+ deviceData.getDate().getTime());
					alarm.setState(RESOLVED);
					alarm.setEmailed(RESOLVED_NOT_EMAILED);
					alarm.setEndDate(new Date().getTime());
					updateAlarm(alarm);
				});
		// TODO: inspect data to see if looks "normal"

	}

	private Alarm getNewAlarm(String customerId, String deviceId, String siteId, String content) {
		Alarm newAlarm = new Alarm(TokenGenerator.generateNewToken(), customerId, deviceId, siteId);
		newAlarm.setStartDate(System.currentTimeMillis());
		newAlarm.setState(ACTIVE);
		newAlarm.setEmailed(NEEDS_EMAIL);
		return newAlarm;
	}

	/**
	 * Device is faulting. Could resolve on its own (frequently does) so log it, set it as don't
	 * email but active
	 *
	 * @param customerId
	 * @param deviceId
	 * @param site
	 * @param content
	 * @return
	 */
	public Optional<Alarm> faultDetected(String customerId, String deviceId, String siteId, String content) {
		logger.warn("fault condition detected: " + content);
		Alarm alarm = findAlarmsByDevice(customerId, deviceId).stream()
				.filter(a -> a.getState() == ACTIVE)
				.findAny()
				.orElseGet(() -> {
					Alarm newAlarm = getNewAlarm(customerId, deviceId, siteId, content);
					newAlarm.setEmailed(DONT_EMAIL);
					newAlarm.setMessage(content); // Write alarm on first
					return newAlarm;
				});
		alarm.setLastUpdate(System.currentTimeMillis());
		return updateAlarm(alarm);
	}

	public Optional<Alarm> alarmConditionDetected(String customerId, String deviceId, String siteId, String content) {
		logger.warn("Alarm condition detected: " + content);
		Alarm alarm = findAlarmsByDevice(customerId, deviceId).stream()
				.filter(a -> a.getState() == ACTIVE)
				.findAny()
				.orElseGet(() -> getNewAlarm(customerId, deviceId, siteId, content));
		if (alarm.getEmailed() == DONT_EMAIL) {
			alarm.setEmailed(NEEDS_EMAIL); // We're turning a "fault" into an alert, should send email now
		}
		alarm.setLastUpdate(System.currentTimeMillis());
		if (StringUtils.isBlank(alarm.getMessage())) {
			alarm.setMessage(content);
		}
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
			if (alarmEmail.isNotificationEnabled()
					&& !IComponentRegistry.OpenSearchStatusComponent.hasFailureWithLastThirtyMinutes()) {
				notificationComponent.sendNotification(alarmEmail.getRecipient(), alarmEmail.getSubject(), alarmEmail);
			} else {
				if (IComponentRegistry.OpenSearchStatusComponent.hasFailureWithLastThirtyMinutes()) {
					logger.warn("Not sending notification, opensearch failure has occurred" + " recently.");
				} else {
					logger.warn("New notification detected, but not sending email as" + " requested.");
				}
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
						builder -> builder.partitionValue(NEEDS_EMAIL).sortValue(customerId)))
				.stream()
				.flatMap(page -> page.items().stream())
				.toList();
	}

	public List<Alarm> findNonEmailedAlarms() {
		return getTable()
				.index(Alarm.EMAILED_CUSTOMER_INDEX)
				.query(QueryConditional.keyEqualTo(builder -> builder.partitionValue(NEEDS_EMAIL)))
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
									d2.getSiteId(),
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
		TransactionUtil.addDeviceId(device.getId());
		TransactionUtil.updateCustomerId(device.getClientId());
		DeviceData data =
				OSComponent.getLastDeviceEntry(device.getName(), OpenSearchQueries.getDeviceIdQuery(device.getId()));
		if (data == null || device.isDisabled()) {
			logger.debug("likely new device with no data (or disabled) " + device.getId());
			return Optional.empty();
		}
		if (data.getDate().getTime() < new Date(System.currentTimeMillis() - TimeConstants.HOUR).getTime()) {
			return alarmConditionDetected(
					data.getCustomerId(),
					data.getDeviceId(),
					data.getSiteId(),
					"No data recently from device.  Last data: "
							+ data.getDate().getTime());
		}
		return checkDeviceExistingData(data);
	}

	public Optional<Alarm> checkDeviceExistingData(DeviceData deviceData) {
		if (!deviceData.isDayLight()) {
			return Optional.empty();
		}
		if (deviceData.getTotalRealPower() == 0
				&& OSComponent.isDeviceGeneratingPower(
						deviceData.getCustomerId(), deviceData.getDeviceId(), TimeConstants.HOUR * 2)) {
			return alarmConditionDetected(
					deviceData.getCustomerId(),
					deviceData.getDeviceId(),
					deviceData.getSiteId(),
					"Device not generating power");
		}
		return Optional.empty();
	}

	public void cleanupOldAlarms() {
		long yearAgo = System.currentTimeMillis() - TimeConstants.YEAR;
		deleteAlarmsByStateAndDate(RESOLVED, yearAgo);
		deleteAlarmsByStateAndDate(ACTIVE, yearAgo);
	}

	private void deleteAlarmsByStateAndDate(int state, long deleteOlderThan) {
		getTable()
				.index(Alarm.STATE_STARTDATE_INDEX)
				.query(QueryConditional.sortLessThan(
						builder -> builder.partitionValue(state).sortValue(deleteOlderThan)))
				.stream()
				.flatMap(page -> page.items().stream())
				.forEach(a -> deleteAlarm(a.getAlarmId(), a.getCustomerId()));
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
