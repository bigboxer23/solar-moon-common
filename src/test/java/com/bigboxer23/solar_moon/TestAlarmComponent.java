package com.bigboxer23.solar_moon;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.data.Alarm;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.open_search.OpenSearchUtils;
import com.bigboxer23.solar_moon.util.TimeConstants;
import com.bigboxer23.solar_moon.util.TokenGenerator;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.xml.xpath.XPathExpressionException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** */
public class TestAlarmComponent implements IComponentRegistry {
	public static final String TEST_ALARM_ID = "2883206e-9e35-4bdd-8c32-d1b35357b22f";

	@BeforeEach
	public void beforeEach() {
		alarmComponent.deleteAlarmsByCustomerId(TestDeviceComponent.clientId);
	}

	@BeforeAll
	public static void before() {
		TestUtils.setupSite();
	}

	@AfterAll
	public static void after() {
		TestUtils.nukeCustomerId(TestDeviceComponent.clientId);
		TestUtils.nukeCustomerId(TestDeviceComponent.clientId + "invalid");
	}

	@Test
	public void testAlarmCRUD() {
		Alarm alarm = new Alarm(TEST_ALARM_ID, TestDeviceComponent.clientId);
		Optional<Alarm> dbAlarm = alarmComponent.updateAlarm(alarm);
		assertTrue(dbAlarm.isPresent());
		assertEquals(TEST_ALARM_ID, dbAlarm.get().getAlarmId());
		assertTrue(alarmComponent
				.findAlarmByAlarmId(TEST_ALARM_ID, TestDeviceComponent.clientId)
				.isPresent());
		assertNull(dbAlarm.get().getSiteId());
		alarm.setState(1);
		alarm.setSiteId(TestDeviceComponent.SITE);
		dbAlarm = alarmComponent.updateAlarm(alarm);
		assertTrue(dbAlarm.isPresent());
		assertEquals(TestDeviceComponent.SITE, dbAlarm.get().getSiteId());
		assertEquals(1, dbAlarm.get().getState());
		alarmComponent.deleteAlarm(TEST_ALARM_ID, TestDeviceComponent.clientId);
		assertFalse(alarmComponent
				.findAlarmByAlarmId(TEST_ALARM_ID, TestDeviceComponent.clientId)
				.isPresent());
	}

	@Test
	public void testFilterByDeviceId() {
		Device device = TestUtils.getDevice();
		Alarm alarm = new Alarm(TEST_ALARM_ID, device.getClientId(), device.getId(), device.getSite());
		alarmComponent.updateAlarm(alarm);
		alarm = new Alarm(
				TokenGenerator.generateNewToken(), TestDeviceComponent.clientId, "Test-" + 2, TestDeviceComponent.SITE);
		alarmComponent.updateAlarm(alarm);
		List<Alarm> alarms = alarmComponent.findAlarmsByDevice(device.getClientId(), device.getId());
		assertEquals(1, alarms.size());
		assertEquals(device.getId(), alarms.get(0).getDeviceId());
	}

	@Test
	public void testFilterBySiteId() {
		Device device = TestUtils.getDevice();
		Alarm alarm = new Alarm(TEST_ALARM_ID, device.getClientId(), device.getId(), device.getSite());
		alarmComponent.updateAlarm(alarm);
		alarm = new Alarm(
				TokenGenerator.generateNewToken(), device.getClientId(), device.getId(), TestDeviceComponent.SITE + 1);
		alarmComponent.updateAlarm(alarm);
		List<Alarm> alarms = alarmComponent.findAlarmsBySite(TestDeviceComponent.clientId, TestDeviceComponent.SITE);
		assertEquals(1, alarms.size());
		assertEquals(TestDeviceComponent.SITE, alarms.get(0).getSiteId());
	}

	@Test
	public void testFilterAlarms() {
		Device device = TestUtils.getDevice();
		Alarm alarm = new Alarm(TEST_ALARM_ID, device.getClientId(), device.getId(), device.getSite());
		alarmComponent.updateAlarm(alarm);
		alarm = new Alarm(TokenGenerator.generateNewToken(), device.getClientId(), "Test-" + 2, device.getSite());
		alarmComponent.updateAlarm(alarm);
		alarm = new Alarm(
				TokenGenerator.generateNewToken(), device.getClientId(), "Test-" + 1, TestDeviceComponent.SITE + 1);
		alarmComponent.updateAlarm(alarm);

		assertEquals(0, alarmComponent.filterAlarms(null, null, null).size());
		assertEquals(
				3,
				alarmComponent
						.filterAlarms(TestDeviceComponent.clientId, null, null)
						.size());
		assertEquals(
				0,
				alarmComponent
						.filterAlarms(null, TestDeviceComponent.SITE, null)
						.size());
		assertEquals(
				0,
				alarmComponent
						.filterAlarms(null, TestDeviceComponent.SITE, device.getId())
						.size());
		assertEquals(
				1,
				alarmComponent
						.filterAlarms(device.getClientId(), TestDeviceComponent.SITE, device.getId())
						.size());
		assertEquals(
				1,
				alarmComponent
						.filterAlarms(device.getClientId(), null, device.getId())
						.size());
		assertEquals(
				2,
				alarmComponent
						.filterAlarms(device.getClientId(), TestDeviceComponent.SITE, null)
						.size());
	}

	@Test
	public void testAlarmConditionDetected() throws InterruptedException {
		Device device = TestUtils.getDevice();
		assertNotNull(device);
		Optional<Alarm> alarm = alarmComponent.alarmConditionDetected(
				device.getClientId(), device.getId(), device.getSite(), "Test alarm!");
		assertTrue(alarm.isPresent());
		assertEquals(0, alarm.get().getEmailed());
		Thread.sleep(1000);
		Optional<Alarm> alarm2 = alarmComponent.alarmConditionDetected(
				device.getClientId(), device.getId(), device.getSite(), "Test alarm!");
		assertTrue(alarm2.isPresent());
		assertTrue(alarm.get().getLastUpdate() < alarm2.get().getLastUpdate());
		assertEquals(alarm.get().getAlarmId(), alarm2.get().getAlarmId());
		alarm2.get().setState(0);
		alarmComponent.updateAlarm(alarm2.get());
		alarm2 = alarmComponent.alarmConditionDetected(
				device.getClientId(), device.getId(), device.getSite(), "Test alarm!");
		assertTrue(alarm2.isPresent());
		assertNotEquals(alarm.get().getAlarmId(), alarm2.get().getAlarmId());
	}

	@Test
	public void getMostRecentAlarm() throws InterruptedException {
		Device device = TestUtils.getDevice();
		Optional<Alarm> alarm = alarmComponent.alarmConditionDetected(
				device.getClientId(), device.getId(), device.getSite(), "Test alarm!");
		assertTrue(alarm.isPresent());
		alarm.get().setState(0);
		alarmComponent.updateAlarm(alarm.get());
		Thread.sleep(1000);
		Optional<Alarm> alarm2 = alarmComponent.alarmConditionDetected(
				device.getClientId(), device.getId(), device.getSite(), "Test alarm!");
		assertTrue(alarm2.isPresent());
		assertNotEquals(alarm.get().getAlarmId(), alarm2.get().getAlarmId());
	}

	@Test
	public void checkDevice() throws XPathExpressionException {
		// test invalid device
		assertFalse(alarmComponent.checkDevice(null).isPresent());
		// test device but no OpenSearch
		Optional<Device> device = deviceComponent.findDeviceByDeviceName(
				TestDeviceComponent.clientId, TestDeviceComponent.deviceName + 0);
		assertTrue(device.isPresent());
		assertFalse(alarmComponent.checkDevice(device.get()).isPresent());

		// test device, but old OpenSearch
		LocalDateTime ldt = LocalDateTime.now();
		generationComponent.handleDeviceBody(
				TestUtils.getDeviceXML(
						TestDeviceComponent.deviceName + 0,
						Date.from(ldt.minusMinutes(61)
								.atZone(ZoneId.systemDefault())
								.toInstant()),
						55),
				TestDeviceComponent.clientId);
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
		generationComponent.handleDeviceBody(
				TestUtils.getDeviceXML(
						TestDeviceComponent.deviceName + 0,
						Date.from(ldt.minusMinutes(59)
								.atZone(ZoneId.systemDefault())
								.toInstant()),
						(55)),
				TestDeviceComponent.clientId);
		OpenSearchUtils.waitForIndexing();
		checkedAlarm = alarmComponent.checkDevice(device.get());
		assertFalse(checkedAlarm.isPresent());

		// Validate we have a record of the alarm and its properly "cleared"
		alarm = alarmComponent.findAlarmByAlarmId(
				alarm.get().getAlarmId(), alarm.get().getCustomerId());
		assertTrue(alarm.isPresent());
		assertEquals(0, alarm.get().getState());
		assertTrue(alarm.get().getEndDate() > 0);

		// Check we create new alarm after current is cleared
		OSComponent.deleteByCustomerId(TestDeviceComponent.clientId);
		generationComponent.handleDeviceBody(
				TestUtils.getDeviceXML(
						TestDeviceComponent.deviceName + 0,
						Date.from(ldt.minusMinutes(61)
								.atZone(ZoneId.systemDefault())
								.toInstant()),
						(55)),
				TestDeviceComponent.clientId);
		OpenSearchUtils.waitForIndexing();
		checkedAlarm = alarmComponent.checkDevice(device.get());
		assertTrue(checkedAlarm.isPresent());
		assertEquals(1, checkedAlarm.get().getState());
		assertNotEquals(alarm.get().getAlarmId(), checkedAlarm.get().getAlarmId());

		// test disabled device, old OpenSearch
		alarmComponent.deleteAlarmsByCustomerId(TestDeviceComponent.clientId);
		device.get().setDisabled(true);
		deviceComponent.updateDevice(device.get());
		assertTrue(deviceComponent
				.getDevice(device.get().getId(), device.get().getClientId())
				.isDisabled());
		alarm = alarmComponent.checkDevice(device.get());
		assertFalse(alarm.isPresent());

		alarmComponent.deleteAlarmsByCustomerId(TestDeviceComponent.clientId);
	}

	@SneakyThrows
	@Test
	public void quickCheckDevices() {
		Device device = TestUtils.getDevice();
		assertEquals(0, alarmComponent.getAlarms(TestDeviceComponent.clientId).size());
		assertEquals(0, alarmComponent.quickCheckDevices().size());
		deviceUpdateComponent.update(device.getId(), System.currentTimeMillis() - TimeConstants.THIRTY_MINUTES + 2000);
		assertEquals(0, alarmComponent.quickCheckDevices().size());
		Thread.sleep(2000);
		assertEquals(1, alarmComponent.quickCheckDevices().size());
		alarmComponent.deleteAlarmsByCustomerId(TestDeviceComponent.clientId);
	}

	@SneakyThrows
	@Test
	public void findNonEmailedAlarms() {
		assertTrue(alarmComponent
				.findNonEmailedAlarms(TestDeviceComponent.clientId)
				.isEmpty());
		Device device = TestUtils.getDevice();
		assertNotNull(device);
		Optional<Alarm> alarm = alarmComponent.alarmConditionDetected(
				device.getClientId(), device.getId(), device.getSite(), "Test alarm!");
		assertTrue(alarm.isPresent());
		assertEquals(0, alarm.get().getEmailed());
		Thread.sleep(1000);
		Optional<Alarm> alarm2 = alarmComponent.alarmConditionDetected(
				device.getClientId(), device.getId(), device.getSite(), "Test alarm!");
		assertTrue(alarm2.isPresent());

		assertEquals(
				1,
				alarmComponent
						.findNonEmailedAlarms(TestDeviceComponent.clientId)
						.size());

		alarmComponent.alarmConditionDetected(
				device.getClientId(), device.getId() + "invalild", device.getSite(), "Test alarm!");
		assertEquals(
				2,
				alarmComponent
						.findNonEmailedAlarms(TestDeviceComponent.clientId)
						.size());
		alarm.get().setEmailed(1);
		alarmComponent.updateAlarm(alarm.get());
		assertEquals(
				1,
				alarmComponent
						.findNonEmailedAlarms(TestDeviceComponent.clientId)
						.size());
		assertTrue(alarmComponent
				.findNonEmailedAlarms(TestDeviceComponent.clientId + "invalid")
				.isEmpty());

		alarmComponent.alarmConditionDetected(
				TestDeviceComponent.clientId + "invalid", device.getId(), device.getSite(), "Test alarm!");
		assertEquals(
				1,
				alarmComponent
						.findNonEmailedAlarms(TestDeviceComponent.clientId + "invalid")
						.size());
		assertEquals(
				1,
				alarmComponent
						.findNonEmailedAlarms(TestDeviceComponent.clientId)
						.size());
		assertEquals(2, alarmComponent.findNonEmailedAlarms().size());
	}

	/*@Test
	public void sendPendingNotifications() {
		alarmComponent.sendPendingNotifications();
	}*/
}
