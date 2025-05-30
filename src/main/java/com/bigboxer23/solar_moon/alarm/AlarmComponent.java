package com.bigboxer23.solar_moon.alarm;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.*;
import com.bigboxer23.solar_moon.device.DeviceComponent;
import com.bigboxer23.solar_moon.dynamodb.AbstractDynamodbComponent;
import com.bigboxer23.solar_moon.notifications.AlarmEmailTemplateContent;
import com.bigboxer23.solar_moon.notifications.ResolvedAlertEmailTemplateContent;
import com.bigboxer23.solar_moon.search.OpenSearchQueries;
import com.bigboxer23.solar_moon.util.TimeConstants;
import com.bigboxer23.solar_moon.util.TokenGenerator;
import com.bigboxer23.solar_moon.weather.IWeatherConstants;
import com.bigboxer23.solar_moon.web.TransactionUtil;
import java.io.IOException;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.utils.StringUtils;

/** */
@Slf4j
public class AlarmComponent extends AbstractDynamodbComponent<Alarm> implements IAlarmConstants, ISolectriaConstants {
	protected static final long QUICK_CHECK_THRESHOLD = TimeConstants.FORTY_FIVE_MINUTES;

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
			log.warn("old data, not adding device update or resolving active alarms " + deviceData.getDate());
			return;
		}
		IComponentRegistry.deviceUpdateComponent.update(deviceData.getDeviceId());
		Optional<Alarm> maybeAlarm =
				getMostRecentAlarm(deviceData.getDeviceId()).filter(alarm -> alarm.getState() == ACTIVE);
		if (maybeAlarm.isEmpty()) {
			return;
		}
		Alarm alarm = maybeAlarm.get();
		boolean isAlarmFromNoResponse =
				!StringUtils.isEmpty(alarm.getMessage()) && alarm.getMessage().startsWith(NO_DATA_RECENTLY);
		if (!deviceData.isDaylight() && !isAlarmFromNoResponse) {
			log.debug("not resolving alarm, not daylight " + maybeAlarm.get().getAlarmId());
			return;
		}
		if (deviceData.getTotalRealPower() <= 0.1 && !isAlarmFromNoResponse) {
			log.warn("not resolving alarm, no real power reported "
					+ deviceData.getTotalRealPower()
					+ " "
					+ maybeAlarm.get().getAlarmId());
			return;
		}
		if (isLinkedDeviceErrored(deviceData, null).isPresent() && !isAlarmFromNoResponse) {
			return;
		}
		log.warn("Resolving alarm for "
				+ alarm.getAlarmId()
				+ " "
				+ deviceData.getDate().getTime()
				+ " "
				+ isAlarmFromNoResponse);
		alarm.setState(RESOLVED);
		// If we've sent a notification email to customer, should flag we need
		// to send a resolve email too
		if (alarm.getEmailed() > RESOLVED_NOT_EMAILED) {
			alarm.setResolveEmailed(NEEDS_EMAIL);
		}
		if (alarm.getEmailed() == NEEDS_EMAIL) {
			alarm.setEmailed(RESOLVED_NOT_EMAILED);
		}
		alarm.setEndDate(new Date().getTime());
		updateAlarm(alarm);
		// TODO: inspect data to see if looks "normal"

	}

	/**
	 * Return the linked device if error state is detected
	 *
	 * @param deviceData
	 * @param device
	 * @return
	 */
	protected Optional<LinkedDevice> isLinkedDeviceErrored(DeviceData deviceData, Device device) {
		if (deviceData == null) {
			log.debug("device data empty, linked device is normal.");
			return Optional.empty();
		}
		Optional<Device> optionalDevice = device != null
				? Optional.of(device)
				: IComponentRegistry.deviceComponent.findDeviceById(
						deviceData.getDeviceId(), deviceData.getCustomerId());
		if (optionalDevice.isEmpty() || StringUtils.isBlank(optionalDevice.get().getSerialNumber())) {
			log.debug("no device or no serial number found, linked device is normal.");
			return Optional.empty();
		}
		Optional<LinkedDevice> linkedDevice = IComponentRegistry.linkedDeviceComponent.queryBySerialNumber(
				optionalDevice.get().getSerialNumber(), deviceData.getCustomerId());
		if (linkedDevice.isEmpty()) {
			log.debug("no linked device. " + optionalDevice.get().getSerialNumber());
			return Optional.empty();
		}
		if (linkedDevice.get().getCriticalAlarm() == ISolectriaConstants.NOMINAL) {
			if (linkedDevice.get().getInformativeAlarm() != ISolectriaConstants.NOMINAL) {
				log.warn(optionalDevice.get().getSerialNumber()
						+ " Linked device has informative error: "
						+ SolectriaErrorOracle.translateError(linkedDevice.get().getInformativeAlarm(), false));
			}
			log.debug("Linked device looks normal. " + optionalDevice.get().getSerialNumber());
			return Optional.empty();
		}
		log.warn("Linked device is in critical error state. "
				+ optionalDevice.get().getSerialNumber()
				+ " "
				+ linkedDevice.get().getCriticalAlarm()
				+ " "
				+ linkedDevice.get().getInformativeAlarm());
		return linkedDevice;
	}

	protected Alarm getNewAlarm(String customerId, String deviceId, String siteId, String content) {
		Alarm newAlarm = new Alarm(TokenGenerator.generateNewToken(), customerId, deviceId, siteId);
		newAlarm.setStartDate(System.currentTimeMillis());
		newAlarm.setState(ACTIVE);
		newAlarm.setMessage(content);
		if (!IComponentRegistry.deviceComponent
				.findDeviceById(deviceId, customerId)
				.map(Device::isNotificationsDisabled)
				.orElse(false)) {
			newAlarm.setEmailed(NEEDS_EMAIL);
		}
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
		log.warn("fault condition detected: " + content);
		Alarm alarm = findAlarmsByDevice(customerId, deviceId).stream()
				.filter(a -> a.getState() == ACTIVE)
				.findAny()
				.orElseGet(() -> {
					Alarm newAlarm = getNewAlarm(customerId, deviceId, siteId, content);
					newAlarm.setEmailed(DONT_EMAIL);
					return newAlarm;
				});
		alarm.setLastUpdate(System.currentTimeMillis());
		return updateAlarm(alarm);
	}

	public Optional<Alarm> alarmConditionDetected(String customerId, String deviceId, String siteId, String content) {
		log.warn("Alarm condition detected: " + content);
		if (IComponentRegistry.maintenanceComponent.isInMaintenanceMode()) {
			log.warn("Maintenance mode activated, not flagging alarm condition.");
			return Optional.empty();
		}
		Alarm alarm = findAlarmsByDevice(customerId, deviceId).stream()
				.filter(a -> a.getState() == ACTIVE)
				.findAny()
				.orElseGet(() -> getNewAlarm(customerId, deviceId, siteId, content));
		if (alarm.getEmailed() == DONT_EMAIL
				&& !IComponentRegistry.deviceComponent
						.findDeviceById(deviceId, customerId)
						.map(Device::isNotificationsDisabled)
						.orElse(false)) {
			alarm.setEmailed(NEEDS_EMAIL); // We're turning a "fault" into an alert, should send email now
		}
		alarm.setLastUpdate(System.currentTimeMillis());
		if (StringUtils.isBlank(alarm.getMessage())) {
			alarm.setMessage(content);
		}
		return updateAlarm(alarm);
	}

	public void sendPendingNotifications() {
		log.info("Checking for pending active alert notifications");
		Map<String, List<Alarm>> customerSortedAlarms = new HashMap<>();
		findNonEmailedActiveAlarms().forEach(alarm -> {
			if (!customerSortedAlarms.containsKey(alarm.getCustomerId())) {
				customerSortedAlarms.put(alarm.getCustomerId(), new ArrayList<>());
			}
			customerSortedAlarms.get(alarm.getCustomerId()).add(alarm);
		});

		customerSortedAlarms.forEach((customerId, alarms) -> {
			TransactionUtil.updateCustomerId(customerId);
			log.info("Starting sending active notifications");
			AlarmEmailTemplateContent alarmEmail = new AlarmEmailTemplateContent(customerId, alarms);
			if (alarmEmail.isNotificationEnabled()
					&& !IComponentRegistry.OpenSearchStatusComponent.hasFailureWithLastThirtyMinutes()) {
				IComponentRegistry.notificationComponent.sendNotification(
						alarmEmail.getRecipient(), alarmEmail.getSubject(), alarmEmail);
			} else {
				if (IComponentRegistry.OpenSearchStatusComponent.hasFailureWithLastThirtyMinutes()) {
					log.warn("Not sending notification, opensearch failure has occurred " + " recently.");
				} else {
					log.warn("New notification detected, but not sending email as " + " requested.");
				}
			}
			alarms.forEach(a -> {
				a.setEmailed(System.currentTimeMillis());
				updateAlarm(a);
			});
		});

		log.info("Checking for pending resolved alert notifications");
		customerSortedAlarms.clear();
		findNonEmailedResolvedAlarms().forEach(alarm -> {
			if (!customerSortedAlarms.containsKey(alarm.getCustomerId())) {
				customerSortedAlarms.put(alarm.getCustomerId(), new ArrayList<>());
			}
			customerSortedAlarms.get(alarm.getCustomerId()).add(alarm);
		});
		customerSortedAlarms.forEach((customerId, alarms) -> {
			TransactionUtil.updateCustomerId(customerId);
			log.info("Starting sending resolved notifications");
			ResolvedAlertEmailTemplateContent alarmEmail = new ResolvedAlertEmailTemplateContent(customerId, alarms);
			IComponentRegistry.notificationComponent.sendNotification(
					alarmEmail.getRecipient(), alarmEmail.getSubject(), alarmEmail);
			alarms.forEach(a -> {
				a.setResolveEmailed(System.currentTimeMillis());
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

	public List<Alarm> findNonEmailedActiveAlarms() {
		return findNonEmailed(Alarm.EMAILED_CUSTOMER_INDEX);
	}

	public List<Alarm> findNonEmailedResolvedAlarms() {
		return findNonEmailed(Alarm.RESOLVED_EMAILED_INDEX);
	}

	private List<Alarm> findNonEmailed(String indexName) {
		return getTable()
				.index(indexName)
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
		log.debug("Fetching all alarms");
		return getTable()
				.index(Alarm.CUSTOMER_INDEX)
				.query(QueryConditional.keyEqualTo(builder -> builder.partitionValue(customerId)))
				.stream()
				.flatMap(page -> page.items().stream())
				.toList();
	}

	public Optional<Alarm> updateAlarm(Alarm alarm) {
		if (alarm == null || StringUtils.isBlank(alarm.getCustomerId()) || StringUtils.isBlank(alarm.getAlarmId())) {
			log.warn("invalid alarm, not updating");
			return Optional.empty();
		}
		log.warn("Updating alarm: " + alarm.getAlarmId());
		return Optional.ofNullable(getTable().updateItem(builder -> builder.item(alarm)));
	}

	public void deleteAlarmByDeviceId(String customerId, String deviceId) {
		findAlarmsByDevice(customerId, deviceId).forEach(a -> deleteAlarm(a.getAlarmId(), a.getCustomerId()));
	}

	public void deleteAlarm(String alarmId, String customerId) {
		log.warn("Deleting alarm: " + alarmId);
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
		log.info("Checking for non-responsive devices");
		List<Alarm> alarms = new ArrayList<>();
		IComponentRegistry.deviceUpdateComponent
				.queryByTimeRange(System.currentTimeMillis() - QUICK_CHECK_THRESHOLD)
				.forEach(d -> IComponentRegistry.deviceComponent
						.findDeviceById(d.getDeviceId())
						.filter(d2 -> !d2.isDisabled())
						.filter(d2 -> {
							boolean isDay = IComponentRegistry.deviceComponent
									.findDeviceById(d2.getSiteId(), d2.getClientId())
									.map(site -> IComponentRegistry.locationComponent
											.isDay(new Date(), site.getLatitude(), site.getLongitude())
											.orElse(true))
									.orElse(true);
							TransactionUtil.updateCustomerId(d2.getClientId());
							// TODO:remove this after  burn in
							TransactionUtil.addDeviceId(d2.getId(), d2.getSiteId());
							log.info("quickCheck is day: " + isDay);
							return isDay;
						})
						.flatMap(d2 -> {
							TransactionUtil.updateCustomerId(d2.getClientId());
							TransactionUtil.addDeviceId(d2.getId(), d2.getSiteId());
							log.warn("Quick check shows no updates for" + " device in last 45 min.");
							return alarmConditionDetected(
									d2.getClientId(), d2.getId(), d2.getSiteId(), NO_DATA_RECENTLY + d.getLastUpdate());
						})
						.ifPresent(alarms::add));
		return alarms;
	}

	public Optional<Alarm> checkDevice(Device device) {
		if (device == null) {
			log.warn("Null device, can't check.");
			return Optional.empty();
		}
		TransactionUtil.updateCustomerId(device.getClientId());
		TransactionUtil.addDeviceId(device.getId(), device.getSiteId());
		DeviceData data = IComponentRegistry.OSComponent.getLastDeviceEntry(
				device.getId(), OpenSearchQueries.getDeviceIdQuery(device.getId()));
		// Check disabled or new
		if (data == null || device.isDisabled()) {
			log.debug("likely new device with no data (or disabled) " + device.getId());
			return Optional.empty();
		}
		// If stale and open search is healthy, fail
		boolean isOpenSearchOk = !IComponentRegistry.OpenSearchStatusComponent.hasFailureWithLastThirtyMinutes();
		if (data.getDate().getTime() < new Date(System.currentTimeMillis() - TimeConstants.HOUR).getTime()
				&& isOpenSearchOk) {
			log.warn("Check shows no updates for device in last 60 min.");
			return alarmConditionDetected(
					data.getCustomerId(),
					data.getDeviceId(),
					data.getSiteId(),
					NO_DATA_RECENTLY + data.getDate().getTime());
		}
		// Inspect device, weather, site data
		if (!isDeviceOK(device, data, isOpenSearchOk)) {
			return alarmConditionDetected(
					data.getCustomerId(), data.getDeviceId(), data.getSiteId(), "Device not generating power");
		}
		Optional<LinkedDevice> linkedDevice = isLinkedDeviceErrored(data, device);
		if (linkedDevice.isPresent()) {
			return alarmConditionDetected(
					data.getCustomerId(),
					data.getDeviceId(),
					data.getSiteId(),
					"Linked device has error(s): "
							+ SolectriaErrorOracle.translateError(
									linkedDevice.get().getCriticalAlarm(), true));
		}
		return Optional.empty();
	}

	protected boolean isDeviceOK(Device device, DeviceData deviceData, boolean isOpenSearchOK) {
		if (!deviceData.isDaylight()) {
			return true;
		}
		if (deviceData.getTotalRealPower() > 0.1) {
			return true;
		}
		if (!isOpenSearchOK) {
			log.info("OpenSearch failures seen within last 30m so ignoring alerting while system" + " recovers.");
			return true;
		}
		if (device.isDeviceSite()) {
			log.debug("site device, shouldn't throw any alarms");
			return true;
		}
		try {
			List<DeviceData> historicData = IComponentRegistry.OSComponent.getRecentDeviceData(
					device.getClientId(), device.getId(), TimeConstants.HOUR * 2);

			boolean isDarkAdjacent = historicData.stream().anyMatch(d -> !d.isDaylight());
			if (isDarkAdjacent) {
				log.debug("dark detected in the recent data, device is assumed to be OK");
				return true;
			}
			if (deviceData.getDate() != null
					&& !IComponentRegistry.locationComponent
							.isDay(
									new Date(deviceData.getDate().getTime() + TimeConstants.HOUR * 2),
									device.getLatitude(),
									device.getLongitude())
							.orElse(true)) {
				log.debug("close to sunset, device is assumed to be OK");
				return true;
			}
			double averageRealPower = historicData.stream()
					.map(DeviceData::getTotalRealPower)
					.mapToDouble(Double::valueOf)
					.average()
					.orElse(-1);
			if (averageRealPower > 0.1) {
				log.debug("average production over .1kW detected");
				return true;
			}
			// Check weather
			double uvIndex = historicData.stream()
					.map(DeviceData::getUVIndex)
					.mapToDouble(Double::valueOf)
					.average()
					.orElse(-1);
			// Check for -1 as could be a site w/o location data set (or we could be missing weather
			// data)
			if (uvIndex != -1 && uvIndex <= 0.3) {
				log.info("average uv conditions are very low, panel may be OK " + uvIndex);
				return true;
			}
			boolean hasPrecipitation = historicData.stream()
					.anyMatch(data -> IWeatherConstants.RAIN.equalsIgnoreCase(data.getWeatherSummary())
							|| IWeatherConstants.SNOW.equalsIgnoreCase(data.getWeatherSummary()));
			double precipIntensity = historicData.stream()
					.map(DeviceData::getPrecipitationIntensity)
					.mapToDouble(Double::valueOf)
					.average()
					.orElse(-1);
			if (hasPrecipitation && precipIntensity > .01) {
				log.info("rain recently, panel may be OK " + precipIntensity);
				return true;
			}
			if (!anySiteDevicesHealthy(device)) {
				log.info("site devices all report low power, panel(s) may be OK");
				return true;
			}
			log.warn(
					"Device not generating power {}: {}: {}: {}: {}: {}",
					historicData.size(),
					isDarkAdjacent,
					averageRealPower,
					uvIndex,
					precipIntensity,
					hasPrecipitation);
			return false;
		} catch (IOException e) {
			log.error("isDeviceGeneratingPower", e);
			return true;
		}
	}

	/**
	 * Parse other site devices to see if they're producing power or not. If they are not, device
	 * may be ok.
	 *
	 * @param device
	 * @return true if site or device is not within a site or if any site devices report power > .1
	 *     W
	 */
	private boolean anySiteDevicesHealthy(Device device) {
		if (device == null
				|| device.isDeviceSite()
				|| StringUtils.isEmpty(device.getSiteId())
				|| DeviceComponent.NO_SITE.equalsIgnoreCase(device.getSiteId())) {
			log.info("no site attached to device or is site. Won't include in OK check.");
			return true;
		}
		List<Device> siteDevices =
				IComponentRegistry.deviceComponent.getDevicesBySiteId(device.getClientId(), device.getSiteId());
		boolean hasValidSiteDeviceData = false;
		for (Device siteDevice : siteDevices) {
			if (!device.getId().equalsIgnoreCase(siteDevice.getId()) && !siteDevice.isDeviceSite()) {
				DeviceData siteDeviceData = IComponentRegistry.OSComponent.getLastDeviceEntry(
						siteDevice.getId(), OpenSearchQueries.getDeviceIdQuery(siteDevice.getId()));
				hasValidSiteDeviceData = hasValidSiteDeviceData || siteDeviceData != null;
				if (siteDeviceData != null && siteDeviceData.getTotalRealPower() > 0.25) {
					log.info(
							"average production over .25kW detected on site device {} : {} : {}",
							siteDevice.getId(),
							siteDeviceData.getTotalRealPower(),
							siteDeviceData.getDate());
					return true;
				}
			}
		}
		if (!hasValidSiteDeviceData) {
			log.warn("no other valid site data exists to check, returning true");
			return true;
		}
		return false;
	}

	public void clearDisabledResolvedAlarms() {
		getActiveAlarms().forEach(alarm -> IComponentRegistry.deviceComponent
				.findDeviceById(alarm.getDeviceId(), alarm.getCustomerId())
				.filter(Device::isDisabled)
				.ifPresent(device -> {
					TransactionUtil.addDeviceId(device.getId(), device.getSiteId());
					log.warn("resolving alarm for disabled device");
					alarm.setState(RESOLVED);
					alarm.setEmailed(RESOLVED_NOT_EMAILED);
					alarm.setEndDate(new Date().getTime());
					updateAlarm(alarm);
				}));
	}

	public List<Alarm> getActiveAlarms() {
		return getTable()
				.index(Alarm.STATE_CUSTOMER_INDEX)
				.query(QueryConditional.keyEqualTo(builder -> builder.partitionValue(ACTIVE)))
				.stream()
				.flatMap(page -> page.items().stream())
				.toList();
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
