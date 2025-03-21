package com.bigboxer23.solar_moon.alarm;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.TestConstants;
import com.bigboxer23.solar_moon.TestUtils;
import com.bigboxer23.solar_moon.data.Alarm;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.data.LinkedDevice;
import com.bigboxer23.solar_moon.device.DeviceComponent;
import com.bigboxer23.solar_moon.search.OpenSearchUtils;
import com.bigboxer23.solar_moon.util.TimeConstants;
import com.bigboxer23.solar_moon.util.TimeUtils;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import javax.xml.xpath.XPathExpressionException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.ResponseException;

/** */
public class TestAlarmComponent implements IComponentRegistry, TestConstants, IAlarmConstants, ISolectriaConstants {
	public static final String TEST_ALARM_ID = "2883206e-9e35-4bdd-8c32-d1b35357b22f";

	@BeforeEach
	public void beforeEach() {
		alarmComponent.deleteAlarmsByCustomerId(CUSTOMER_ID);
	}

	@BeforeAll
	public static void before() {
		TestUtils.setupSite();
	}

	@AfterAll
	public static void after() {
		TestUtils.nukeCustomerId(CUSTOMER_ID);
		TestUtils.nukeCustomerId(CUSTOMER_ID + "invalid");
	}

	@Test
	public void testAlarmCRUD() {
		Alarm alarm = alarmComponent.getNewAlarm(
				CUSTOMER_ID,
				TestUtils.getDevice().getId(),
				TestUtils.getDevice().getSiteId(),
				null);
		Optional<Alarm> dbAlarm = alarmComponent.updateAlarm(alarm);
		assertTrue(dbAlarm.isPresent());
		assertEquals(alarm.getAlarmId(), dbAlarm.get().getAlarmId());
		assertTrue(alarmComponent
				.findAlarmByAlarmId(alarm.getAlarmId(), CUSTOMER_ID)
				.isPresent());
		assertEquals(TestUtils.getDevice().getSiteId(), dbAlarm.get().getSiteId());
		alarm.setState(ACTIVE);
		alarm.setSiteId(SITE);
		dbAlarm = alarmComponent.updateAlarm(alarm);
		assertTrue(dbAlarm.isPresent());
		assertEquals(SITE, dbAlarm.get().getSiteId());
		assertEquals(ACTIVE, dbAlarm.get().getState());
		alarmComponent.deleteAlarm(alarm.getAlarmId(), CUSTOMER_ID);
		assertFalse(alarmComponent
				.findAlarmByAlarmId(alarm.getAlarmId(), CUSTOMER_ID)
				.isPresent());
	}

	@Test
	public void testFilterByDeviceId() {
		Device device = TestUtils.getDevice();
		alarmComponent.updateAlarm(
				alarmComponent.getNewAlarm(device.getClientId(), device.getId(), device.getSiteId(), null));
		alarmComponent.updateAlarm(alarmComponent.getNewAlarm(CUSTOMER_ID, "Test-" + 2, SITE, null));
		List<Alarm> alarms = alarmComponent.findAlarmsByDevice(device.getClientId(), device.getId());
		assertEquals(1, alarms.size());
		assertEquals(device.getId(), alarms.getFirst().getDeviceId());
	}

	@Test
	public void testFilterBySiteId() {
		Device device = TestUtils.getDevice();
		alarmComponent.updateAlarm(
				alarmComponent.getNewAlarm(device.getClientId(), device.getId(), device.getSiteId(), null));
		alarmComponent.updateAlarm(alarmComponent.getNewAlarm(device.getClientId(), device.getId(), SITE + 1, null));
		List<Alarm> alarms = alarmComponent.findAlarmsBySite(CUSTOMER_ID, device.getSiteId());
		assertEquals(1, alarms.size());
		assertEquals(device.getSiteId(), alarms.getFirst().getSiteId());
	}

	@Test
	public void testFilterAlarms() {
		Device device = TestUtils.getDevice();
		alarmComponent.updateAlarm(
				alarmComponent.getNewAlarm(device.getClientId(), device.getId(), device.getSiteId(), null));
		alarmComponent.updateAlarm(
				alarmComponent.getNewAlarm(device.getClientId(), "Test-" + 2, device.getSiteId(), null));
		alarmComponent.updateAlarm(alarmComponent.getNewAlarm(device.getClientId(), "Test-" + 1, SITE + 1, null));

		assertEquals(0, alarmComponent.filterAlarms(null, null, null).size());
		assertEquals(3, alarmComponent.filterAlarms(CUSTOMER_ID, null, null).size());
		assertEquals(0, alarmComponent.filterAlarms(null, SITE, null).size());
		assertEquals(0, alarmComponent.filterAlarms(null, SITE, device.getId()).size());
		assertEquals(
				1,
				alarmComponent
						.filterAlarms(device.getClientId(), SITE, device.getId())
						.size());
		assertEquals(
				1,
				alarmComponent
						.filterAlarms(device.getClientId(), null, device.getId())
						.size());
		assertEquals(
				2,
				alarmComponent
						.filterAlarms(device.getClientId(), TestUtils.getSite().getSiteId(), null)
						.size());
	}

	@Test
	public void testAlarmConditionDetected() throws InterruptedException {
		Device device = TestUtils.getDevice();
		assertNotNull(device);
		Optional<Alarm> alarm = alarmComponent.alarmConditionDetected(
				device.getClientId(), device.getId(), device.getSiteId(), "Test alarm!");
		assertTrue(alarm.isPresent());
		assertEquals(NEEDS_EMAIL, alarm.get().getEmailed());
		Thread.sleep(1000);
		Optional<Alarm> alarm2 = alarmComponent.alarmConditionDetected(
				device.getClientId(), device.getId(), device.getSiteId(), "Test alarm!");
		assertTrue(alarm2.isPresent());
		assertTrue(alarm.get().getLastUpdate() < alarm2.get().getLastUpdate());
		assertEquals(alarm.get().getAlarmId(), alarm2.get().getAlarmId());
		alarm2.get().setState(RESOLVED);
		alarmComponent.updateAlarm(alarm2.get());
		alarm2 = alarmComponent.alarmConditionDetected(
				device.getClientId(), device.getId(), device.getSiteId(), "Test alarm!");
		assertTrue(alarm2.isPresent());
		assertNotEquals(alarm.get().getAlarmId(), alarm2.get().getAlarmId());
	}

	@Test
	public void getMostRecentAlarm() throws InterruptedException {
		Device device = TestUtils.getDevice();
		Optional<Alarm> alarm = alarmComponent.alarmConditionDetected(
				device.getClientId(), device.getId(), device.getSiteId(), "Test alarm!");
		assertTrue(alarm.isPresent());
		alarm.get().setState(RESOLVED);
		alarmComponent.updateAlarm(alarm.get());
		Thread.sleep(1000);
		Optional<Alarm> alarm2 = alarmComponent.alarmConditionDetected(
				device.getClientId(), device.getId(), device.getSiteId(), "Test alarm!");
		assertTrue(alarm2.isPresent());
		assertNotEquals(alarm.get().getAlarmId(), alarm2.get().getAlarmId());
	}

	@Test
	public void checkDevice() throws XPathExpressionException, ResponseException {
		// test invalid device
		assertFalse(alarmComponent.checkDevice(null).isPresent());

		// test device but no OpenSearch
		Optional<Device> device = deviceComponent.findDeviceByDeviceName(CUSTOMER_ID, deviceName + 0);
		assertTrue(device.isPresent());
		assertFalse(alarmComponent.checkDevice(device.get()).isPresent());

		// test device, but old OpenSearch
		LocalDateTime ldt = LocalDateTime.now();
		obviousIngestComponent.handleDeviceBody(
				TestUtils.getDeviceXML(
						deviceName + 0,
						Date.from(ldt.minusMinutes(61)
								.atZone(ZoneId.systemDefault())
								.toInstant()),
						55),
				CUSTOMER_ID);
		OpenSearchUtils.waitForIndexing();
		Optional<Alarm> alarm = alarmComponent.checkDevice(device.get());
		assertTrue(alarm.isPresent());

		// test update to existing alarm
		Optional<Alarm> checkedAlarm = alarmComponent.checkDevice(device.get());
		assertTrue(checkedAlarm.isPresent());
		assertEquals(alarm.get().getAlarmId(), checkedAlarm.get().getAlarmId());
		assertEquals(alarm.get().getStartDate(), checkedAlarm.get().getStartDate());
		assertNotEquals(alarm.get().getLastUpdate(), checkedAlarm.get().getLastUpdate());

		// test device, valid OpenSearch
		DeviceData deviceData = obviousIngestComponent.handleDeviceBody(
				TestUtils.getDeviceXML(
						deviceName + 0,
						Date.from(ldt.minusMinutes(59)
								.atZone(ZoneId.systemDefault())
								.toInstant()),
						55),
				CUSTOMER_ID);
		OpenSearchUtils.waitForIndexing();
		checkedAlarm = alarmComponent.checkDevice(device.get());
		assertFalse(checkedAlarm.isPresent());

		// Validate we have a record of the alarm and its properly "cleared"
		alarm = alarmComponent.findAlarmByAlarmId(
				alarm.get().getAlarmId(), alarm.get().getCustomerId());
		assertTrue(alarm.isPresent());

		// This is junk to have different results depending on the time of day, but that's the real
		// world
		assertEquals(deviceData.isDaylight() ? RESOLVED : ACTIVE, alarm.get().getState());
		if (deviceData.isDaylight()) {
			assertTrue(alarm.get().getEndDate() > 0);
		}

		// Check we create new alarm after current is cleared
		OSComponent.deleteByCustomerId(CUSTOMER_ID);
		obviousIngestComponent.handleDeviceBody(
				TestUtils.getDeviceXML(
						deviceName + 0,
						Date.from(ldt.minusMinutes(61)
								.atZone(ZoneId.systemDefault())
								.toInstant()),
						(55)),
				CUSTOMER_ID);
		OpenSearchUtils.waitForIndexing();
		checkedAlarm = alarmComponent.checkDevice(device.get());
		assertTrue(checkedAlarm.isPresent());
		assertEquals(ACTIVE, checkedAlarm.get().getState());
		if (deviceData.isDaylight()) {
			assertNotEquals(alarm.get().getAlarmId(), checkedAlarm.get().getAlarmId());
		} else {
			assertEquals(alarm.get().getAlarmId(), checkedAlarm.get().getAlarmId());
		}

		// test disabled device, old OpenSearch
		alarmComponent.deleteAlarmsByCustomerId(CUSTOMER_ID);
		device.get().setDisabled(true);
		deviceComponent.updateDevice(device.get());
		assertTrue(deviceComponent
				.findDeviceById(device.get().getId(), device.get().getClientId())
				.get()
				.isDisabled());
		alarm = alarmComponent.checkDevice(device.get());
		assertFalse(alarm.isPresent());

		alarmComponent.deleteAlarmsByCustomerId(CUSTOMER_ID);
	}

	@SneakyThrows
	@Test
	public void quickCheckDevices() {
		Device device = TestUtils.getDevice();
		assertEquals(0, alarmComponent.getAlarms(CUSTOMER_ID).size());
		assertEquals(0, quickCheckTestDevices().size());
		deviceUpdateComponent.update(
				device.getId(), System.currentTimeMillis() - AlarmComponent.QUICK_CHECK_THRESHOLD + 2000);
		assertEquals(0, quickCheckTestDevices().size());
		Thread.sleep(2000);
		assertEquals(1, quickCheckTestDevices().size());
		alarmComponent.deleteAlarmsByCustomerId(CUSTOMER_ID);
	}

	private List<Alarm> quickCheckTestDevices() {
		return alarmComponent.quickCheckDevices().stream()
				.filter(a -> CUSTOMER_ID.equals(a.getCustomerId()))
				.toList();
	}

	@SneakyThrows
	@Test
	public void findNonEmailedAlarms() {
		assertTrue(alarmComponent.findNonEmailedAlarms(CUSTOMER_ID).isEmpty());
		Device device = TestUtils.getDevice();
		assertNotNull(device);
		Optional<Alarm> alarm = alarmComponent.alarmConditionDetected(
				device.getClientId(), device.getId(), device.getSiteId(), "Test alarm!");
		assertTrue(alarm.isPresent());
		assertEquals(NEEDS_EMAIL, alarm.get().getEmailed());
		assertEquals(DONT_EMAIL, alarm.get().getResolveEmailed());

		Thread.sleep(1000);
		Optional<Alarm> alarm2 = alarmComponent.alarmConditionDetected(
				device.getClientId(), device.getId(), device.getSiteId(), "Test alarm!");
		assertTrue(alarm2.isPresent());

		assertEquals(1, alarmComponent.findNonEmailedAlarms(CUSTOMER_ID).size());

		alarmComponent.alarmConditionDetected(
				device.getClientId(), device.getId() + "invalild", device.getSiteId(), "Test alarm!");
		assertEquals(2, alarmComponent.findNonEmailedAlarms(CUSTOMER_ID).size());
		alarm.get().setEmailed(RESOLVED_NOT_EMAILED);
		alarmComponent.updateAlarm(alarm.get());
		assertEquals(1, alarmComponent.findNonEmailedAlarms(CUSTOMER_ID).size());
		assertTrue(alarmComponent.findNonEmailedAlarms(CUSTOMER_ID + "invalid").isEmpty());

		alarmComponent.alarmConditionDetected(
				CUSTOMER_ID + "invalid", device.getId(), device.getSiteId(), "Test alarm!");
		assertEquals(
				1, alarmComponent.findNonEmailedAlarms(CUSTOMER_ID + "invalid").size());
		assertEquals(1, alarmComponent.findNonEmailedAlarms(CUSTOMER_ID).size());
		assertEquals(2, alarmComponent.findNonEmailedActiveAlarms().size());
	}

	public void createConditionsForResolvedEmailToBeSent() {
		String customerId = "";
		String deviceId = "";

		Optional<Device> device = deviceComponent.findDeviceById(deviceId, customerId);
		assertTrue(device.isPresent());

		Optional<Alarm> alarm = alarmComponent.alarmConditionDetected(
				customerId, device.get().getId(), device.get().getSiteId(), "Test alarm!");
		assertTrue(alarm.isPresent());
		assertEquals(NEEDS_EMAIL, alarm.get().getEmailed());
		assertEquals(DONT_EMAIL, alarm.get().getResolveEmailed());
		alarm.get().setEmailed(System.currentTimeMillis());
		alarm = alarmComponent.updateAlarm(alarm.get());
		assertTrue(alarm.isPresent());

		DeviceData deviceData = new DeviceData(
				device.get().getSiteId(),
				device.get().getClientId(),
				device.get().getId());
		deviceData.setDate(new Date());
		deviceData.setDaylight(true);
		deviceData.setTotalRealPower(2);
		alarmComponent.resolveActiveAlarms(deviceData);
		System.out.println("alarm id: " + alarm.get().getAlarmId());
	}

	@Test
	public void findResolvedNonEmailedAlerts() {
		// Test for case where we've not sent an alert email and should not send a resolved email
		assertTrue(alarmComponent.findNonEmailedAlarms(CUSTOMER_ID).isEmpty());
		Device device = TestUtils.getDevice();
		assertNotNull(device);
		Optional<Alarm> alarm = alarmComponent.alarmConditionDetected(
				device.getClientId(), device.getId(), device.getSiteId(), "Test alarm!");
		assertTrue(alarm.isPresent());
		assertEquals(NEEDS_EMAIL, alarm.get().getEmailed());
		assertEquals(DONT_EMAIL, alarm.get().getResolveEmailed());
		DeviceData deviceData = new DeviceData(device.getSiteId(), device.getClientId(), device.getId());
		deviceData.setDate(new Date());
		deviceData.setDaylight(true);
		deviceData.setTotalRealPower(2);
		alarmComponent.resolveActiveAlarms(deviceData);
		alarm = alarmComponent.findAlarmByAlarmId(alarm.get().getAlarmId(), device.getClientId());
		assertTrue(alarm.isPresent());
		assertEquals(RESOLVED_NOT_EMAILED, alarm.get().getEmailed());
		assertEquals(DONT_EMAIL, alarm.get().getResolveEmailed());

		// Test for case where we have sent an alarm email, so on resolution we should send an email
		// that the alert is over
		alarm = alarmComponent.alarmConditionDetected(
				device.getClientId(), device.getId(), device.getSiteId(), "Test alarm!");
		assertTrue(alarm.isPresent());
		assertEquals(NEEDS_EMAIL, alarm.get().getEmailed());
		assertEquals(DONT_EMAIL, alarm.get().getResolveEmailed());
		alarm.get().setEmailed(System.currentTimeMillis());
		alarm = alarmComponent.updateAlarm(alarm.get());
		assertTrue(alarm.isPresent());
		alarmComponent.resolveActiveAlarms(deviceData);
		alarm = alarmComponent.findAlarmByAlarmId(alarm.get().getAlarmId(), device.getClientId());
		assertTrue(alarm.isPresent());
		assertNotEquals(RESOLVED_NOT_EMAILED, alarm.get().getEmailed());
		assertEquals(NEEDS_EMAIL, alarm.get().getResolveEmailed());
	}

	@Test
	public void resolveActiveAlarms() {
		// Test the totally valid case
		assertTrue(alarmComponent.findNonEmailedAlarms(CUSTOMER_ID).isEmpty());
		Device device = TestUtils.getDevice();
		assertNotNull(device);
		DeviceData deviceData = new DeviceData(device.getSiteId(), device.getClientId(), device.getId());
		deviceData.setDate(new Date());
		deviceData.setDaylight(true);
		deviceData.setTotalRealPower(2);
		validateAlertResolution(deviceData, false, IAlarmConstants.RESOLVED);
		validateAlertResolution(deviceData, true, IAlarmConstants.RESOLVED);

		// Test not daylight
		deviceData.setDaylight(false);
		validateAlertResolution(deviceData, false, IAlarmConstants.ACTIVE);
		validateAlertResolution(deviceData, true, IAlarmConstants.RESOLVED);
		deviceData.setDaylight(true);

		// test low power
		deviceData.setTotalRealPower(0);
		validateAlertResolution(deviceData, false, IAlarmConstants.ACTIVE);
		validateAlertResolution(deviceData, true, IAlarmConstants.RESOLVED);
		deviceData.setTotalRealPower(0.09f);
		validateAlertResolution(deviceData, false, IAlarmConstants.ACTIVE);
		validateAlertResolution(deviceData, true, IAlarmConstants.RESOLVED);
		deviceData.setTotalRealPower(0.1f);
		validateAlertResolution(deviceData, false, IAlarmConstants.RESOLVED);

		// test old date
		deviceData.setDate(new Date(deviceData.getDate().getTime() - (2 * TimeConstants.HOUR)));
		validateAlertResolution(deviceData, false, IAlarmConstants.ACTIVE);
		validateAlertResolution(deviceData, true, IAlarmConstants.ACTIVE);
	}

	@Test
	public void isDeviceOK() throws ResponseException, InterruptedException {
		DeviceData data = new DeviceData(
				TestUtils.getDevice().getSiteId(),
				CUSTOMER_ID,
				TestUtils.getDevice().getId());

		data.setDaylight(false);
		assertTrue(IComponentRegistry.alarmComponent.isDeviceOK(TestUtils.getDevice(), data, true));
		data.setDaylight(true);
		data.setTotalRealPower(1);
		assertTrue(IComponentRegistry.alarmComponent.isDeviceOK(TestUtils.getDevice(), data, true));
		// add some historic data and test against it

		// Test case where external factors shouldn't cause us to report OK
		data.setTotalRealPower(.01f);
		data.setUVIndex(.41f);
		seedData(data);
		assertFalse(IComponentRegistry.alarmComponent.isDeviceOK(TestUtils.getDevice(), data, true));
		assertTrue(IComponentRegistry.alarmComponent.isDeviceOK(TestUtils.getDevice(), data, false));

		// Test Low UV factor
		OSComponent.deleteByCustomerId(CUSTOMER_ID);
		data.setUVIndex(.09f);
		seedData(data);
		assertTrue(IComponentRegistry.alarmComponent.isDeviceOK(TestUtils.getDevice(), data, true));
		assertTrue(IComponentRegistry.alarmComponent.isDeviceOK(TestUtils.getDevice(), data, false));

		// Test daylight adjacent factor
		OSComponent.deleteByCustomerId(CUSTOMER_ID);
		data.setUVIndex(.2f);
		data.setDaylight(false);
		seedData(data);
		assertTrue(IComponentRegistry.alarmComponent.isDeviceOK(TestUtils.getDevice(), data, true));
		assertTrue(IComponentRegistry.alarmComponent.isDeviceOK(TestUtils.getDevice(), data, false));

		// Test good power generation adjacent
		OSComponent.deleteByCustomerId(CUSTOMER_ID);
		data.setDaylight(true);
		data.setTotalRealPower(0.11f);
		seedData(data);
		assertTrue(IComponentRegistry.alarmComponent.isDeviceOK(TestUtils.getDevice(), data, true));
		assertTrue(IComponentRegistry.alarmComponent.isDeviceOK(TestUtils.getDevice(), data, false));

		// Test site device not using the healthy site nodes check
		data.setDeviceId(TestUtils.getSite().getId());
		data.setTotalRealPower(.01f);
		data.setUVIndex(.41f);
		OSComponent.deleteByCustomerId(CUSTOMER_ID);
		seedData(data);
		assertFalse(IComponentRegistry.alarmComponent.isDeviceOK(TestUtils.getSite(), data, true));

		// Test device w/o site not using the healthy site nodes check
		Device noSite = new Device();
		noSite.setSite(DeviceComponent.NO_SITE);
		noSite.setClientId(TestUtils.getDevice().getClientId());
		noSite = TestUtils.addDevice("no site", noSite, false, DeviceComponent.NO_SITE);

		data.setDeviceId(noSite.getId());
		seedData(data);
		assertFalse(IComponentRegistry.alarmComponent.isDeviceOK(noSite, data, true));

		// Validate current device state returns non ok status
		data.setDeviceId(TestUtils.getDevice().getId());
		seedData(data);
		assertFalse(IComponentRegistry.alarmComponent.isDeviceOK(TestUtils.getDevice(), data, true));

		// Test adding dead data on another site node and validate check returns properly
		Device otherSiteDevice = IComponentRegistry.deviceComponent
				.getDevicesBySiteId(
						TestUtils.getDevice().getClientId(),
						TestUtils.getDevice().getSiteId())
				.stream()
				.filter(Device::isDeviceSite)
				.filter(d -> !d.getId().equalsIgnoreCase(TestUtils.getDevice().getId()))
				.findAny()
				.orElse(null);
		assertNotNull(otherSiteDevice);
		data.setDeviceId(otherSiteDevice.getId());
		data.setTotalRealPower(0);
		seedData(data);
		data.setDeviceId(TestUtils.getDevice().getId());
		assertFalse(IComponentRegistry.alarmComponent.isDeviceOK(TestUtils.getDevice(), data, true));

		// Test adding non-dead data on another site node and validate check returns properly
		data.setDeviceId(otherSiteDevice.getId());
		data.setTotalRealPower(.25f);
		seedData(data);
		data.setDeviceId(TestUtils.getDevice().getId());
		assertTrue(IComponentRegistry.alarmComponent.isDeviceOK(TestUtils.getDevice(), data, true));
	}

	private void seedData(DeviceData seed) throws ResponseException, InterruptedException {
		LocalDateTime ldt =
				LocalDateTime.ofInstant(TimeUtils.get15mRoundedDate().toInstant(), ZoneId.systemDefault());
		List<DeviceData> datas = new ArrayList<>();
		for (int ai = 0; ai < 8; ai++) {
			DeviceData deviceData = new DeviceData(seed);
			deviceData.setDate(Date.from(
					ldt.minusMinutes(15 * ai).atZone(ZoneId.systemDefault()).toInstant()));
			datas.add(deviceData);
		}
		IComponentRegistry.OSComponent.logData(null, datas);
		OpenSearchUtils.waitForIndexing();
		Thread.sleep(1000);
	}

	private void validateAlertResolution(DeviceData deviceData, boolean isAlarmFromNonUpdate, int expectedState) {
		Optional<Alarm> alarm = alarmComponent.alarmConditionDetected(
				TestUtils.getDevice().getClientId(),
				TestUtils.getDevice().getId(),
				TestUtils.getDevice().getSiteId(),
				isAlarmFromNonUpdate ? NO_DATA_RECENTLY + System.currentTimeMillis() : "Test alarm!");
		assertTrue(alarm.isPresent());
		assertEquals(IAlarmConstants.ACTIVE, alarm.get().getState());
		alarmComponent.resolveActiveAlarms(deviceData);
		alarm = alarmComponent.findAlarmByAlarmId(
				alarm.get().getAlarmId(), TestUtils.getDevice().getClientId());
		assertTrue(alarm.isPresent());
		assertEquals(expectedState, alarm.get().getState());
		alarmComponent.deleteAlarm(alarm.get().getAlarmId(), alarm.get().getCustomerId());
	}

	@SneakyThrows
	@Test
	public void cleanupOldAlarms() {
		Alarm alarm = new Alarm(TEST_ALARM_ID, CUSTOMER_ID);
		alarm.setStartDate(new Date().getTime());
		alarm.setState(0);
		Optional<Alarm> dbAlarm = alarmComponent.updateAlarm(alarm);
		assertTrue(dbAlarm.isPresent());
		Alarm alarm2 = new Alarm(TEST_ALARM_ID + 2, CUSTOMER_ID);
		alarm2.setStartDate(new Date().getTime());
		alarm2.setState(ACTIVE);
		Optional<Alarm> dbAlarm2 = alarmComponent.updateAlarm(alarm2);
		assertTrue(dbAlarm2.isPresent());
		alarmComponent.cleanupOldAlarms();
		assertTrue(alarmComponent.updateAlarm(alarm).isPresent());
		assertTrue(alarmComponent.updateAlarm(alarm2).isPresent());
		alarm.setStartDate(new Date().getTime() - TimeConstants.YEAR);
		alarm2.setStartDate(new Date().getTime() - TimeConstants.YEAR);
		alarmComponent.updateAlarm(alarm);
		alarmComponent.updateAlarm(alarm2);
		alarmComponent.cleanupOldAlarms();
		assertFalse(alarmComponent
				.findAlarmByAlarmId(alarm.getAlarmId(), alarm.getCustomerId())
				.isPresent());
		assertFalse(alarmComponent
				.findAlarmByAlarmId(alarm2.getAlarmId(), alarm2.getCustomerId())
				.isPresent());
	}

	@Test
	public void faultDetected() {
		Device device = TestUtils.getDevice();
		Optional<Alarm> alarm =
				alarmComponent.faultDetected(CUSTOMER_ID, device.getId(), device.getSiteId(), "Error content");
		assertTrue(alarm.isPresent());
		assertEquals(DONT_EMAIL, alarm.get().getEmailed());
		assertEquals(ACTIVE, alarm.get().getState());
		assertEquals("Error content", alarm.get().getMessage());
		alarm = alarmComponent.faultDetected(CUSTOMER_ID, device.getId(), device.getSiteId(), "Error content2222");
		assertTrue(alarm.isPresent());
		assertEquals(DONT_EMAIL, alarm.get().getEmailed());
		assertEquals(ACTIVE, alarm.get().getState());
		assertEquals("Error content", alarm.get().getMessage());
		alarm = alarmComponent.alarmConditionDetected(
				CUSTOMER_ID, device.getId(), device.getSiteId(), "Error content2");
		assertTrue(alarm.isPresent());
		assertEquals(NEEDS_EMAIL, alarm.get().getEmailed());
		assertEquals(ACTIVE, alarm.get().getState());
		assertEquals("Error content", alarm.get().getMessage());
	}

	@Test
	public void getNewAlarm() {
		Device device = TestUtils.getDevice();
		Alarm alarm = alarmComponent.getNewAlarm(device.getClientId(), device.getId(), device.getSiteId(), null);
		assertEquals(NEEDS_EMAIL, alarm.getEmailed());
		assertEquals(ACTIVE, alarm.getState());

		Device noNotifications = new Device(null, device.getClientId());
		noNotifications.setSite(TestConstants.SITE);
		noNotifications = TestUtils.addDevice("getNewAlarm", noNotifications, false, device.getSiteId());
		noNotifications.setNotificationsDisabled(true);
		deviceComponent.updateDevice(noNotifications);

		alarm = alarmComponent.getNewAlarm(device.getClientId(), noNotifications.getId(), device.getSiteId(), null);
		assertEquals(DONT_EMAIL, alarm.getEmailed());
		assertEquals(ACTIVE, alarm.getState());
	}

	public void migrateAlarmsFromSiteToSite() {
		String fromSiteName = "";
		String toSiteName = "";
		String customerId = "";
		Optional<Device> fromDevice = deviceComponent.findDeviceByName(customerId, fromSiteName);
		assertTrue(fromDevice.isPresent());

		Optional<Device> toDevice = deviceComponent.findDeviceByName(customerId, toSiteName);
		assertTrue(toDevice.isPresent());

		alarmComponent.findAlarmsBySite(customerId, fromDevice.get().getId()).stream()
				.peek(alarm -> alarm.setSiteId(toDevice.get().getId()))
				.forEach(alarmComponent::updateAlarm);
	}

	@Test
	public void getActiveAlarms() {
		List<Alarm> alarms = alarmComponent.getActiveAlarms();
		Device device = TestUtils.getDevice();
		Alarm alarm = alarmComponent.getNewAlarm(device.getClientId(), device.getId(), device.getSiteId(), null);
		alarmComponent.updateAlarm(alarm);
		assertTrue(alarmComponent.getActiveAlarms().size() > alarms.size());
		alarm.setState(RESOLVED);
		alarmComponent.updateAlarm(alarm);
		assertEquals(alarmComponent.getActiveAlarms().size(), alarms.size());
	}

	@Test
	public void clearDisabledResolvedAlarms() {
		List<Alarm> alarms = alarmComponent.getActiveAlarms();
		Device device = TestUtils.getDevice();
		Alarm alarm = alarmComponent.getNewAlarm(device.getClientId(), device.getId(), device.getSiteId(), null);
		alarmComponent.updateAlarm(alarm);
		assertTrue(alarmComponent.getActiveAlarms().size() > alarms.size());

		device.setDisabled(true);
		deviceComponent.updateDevice(device);
		alarmComponent.clearDisabledResolvedAlarms();
		assertEquals(alarmComponent.getActiveAlarms().size(), alarms.size());
	}

	@Test
	public void isLinkedDeviceErrored() {

		assertFalse(linkedDeviceComponent
				.queryBySerialNumber(TestUtils.getDevice().getSerialNumber(), CUSTOMER_ID)
				.isPresent());
		LinkedDevice linkedDevice = new LinkedDevice(TestUtils.getDevice().getSerialNumber(), CUSTOMER_ID);
		linkedDevice.setDate(System.currentTimeMillis());
		linkedDevice.setCriticalAlarm(Temperature_Sensor_Failure);
		linkedDevice.setInformativeAlarm(DC_voltage_Low);
		linkedDeviceComponent.update(linkedDevice);
		assertTrue(linkedDeviceComponent
				.queryBySerialNumber(TestUtils.getDevice().getSerialNumber(), CUSTOMER_ID)
				.isPresent());
		DeviceData deviceData = new DeviceData(
				TestUtils.getDevice().getSiteId(),
				CUSTOMER_ID,
				TestUtils.getDevice().getId());

		assertTrue(alarmComponent.isLinkedDeviceErrored(deviceData, null).isPresent());

		Device device = new Device(TestUtils.getDevice().getId(), CUSTOMER_ID);

		assertFalse(alarmComponent.isLinkedDeviceErrored(deviceData, device).isPresent()); // No serial number
		device.setSerialNumber(TestUtils.getDevice().getSerialNumber());
		assertTrue(alarmComponent.isLinkedDeviceErrored(deviceData, device).isPresent());

		assertFalse(alarmComponent.isLinkedDeviceErrored(null, null).isPresent()); // null props

		deviceData.setDeviceId("bad id");
		assertFalse(alarmComponent.isLinkedDeviceErrored(deviceData, null).isPresent()); // Test no device, bad id
		deviceData.setDeviceId(TestUtils.getDevice().getId());

		// Test normal values
		linkedDevice.setCriticalAlarm(NOMINAL);
		linkedDeviceComponent.update(linkedDevice);
		assertFalse(alarmComponent.isLinkedDeviceErrored(deviceData, device).isPresent());
		linkedDevice.setInformativeAlarm(NOMINAL);
		linkedDeviceComponent.update(linkedDevice);
		assertFalse(alarmComponent.isLinkedDeviceErrored(deviceData, device).isPresent());
		linkedDevice.setCriticalAlarm(Contactor_Failure);
		linkedDeviceComponent.update(linkedDevice);
		assertTrue(alarmComponent.isLinkedDeviceErrored(deviceData, device).isPresent());

		// test no linked device
		linkedDeviceComponent.delete(linkedDevice.getId(), linkedDevice.getCustomerId());
		assertFalse(alarmComponent.isLinkedDeviceErrored(deviceData, device).isPresent());
	}
}
