package com.bigboxer23.solar_moon.alarm;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.TestConstants;
import com.bigboxer23.solar_moon.TestUtils;
import com.bigboxer23.solar_moon.data.Alarm;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.search.OpenSearchUtils;
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
public class TestAlarmComponent implements IComponentRegistry, TestConstants, IAlarmConstants {
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
		Alarm alarm = new Alarm(TEST_ALARM_ID, CUSTOMER_ID);
		Optional<Alarm> dbAlarm = alarmComponent.updateAlarm(alarm);
		assertTrue(dbAlarm.isPresent());
		assertEquals(TEST_ALARM_ID, dbAlarm.get().getAlarmId());
		assertTrue(alarmComponent.findAlarmByAlarmId(TEST_ALARM_ID, CUSTOMER_ID).isPresent());
		assertNull(dbAlarm.get().getSiteId());
		alarm.setState(ACTIVE);
		alarm.setSiteId(SITE);
		dbAlarm = alarmComponent.updateAlarm(alarm);
		assertTrue(dbAlarm.isPresent());
		assertEquals(SITE, dbAlarm.get().getSiteId());
		assertEquals(ACTIVE, dbAlarm.get().getState());
		alarmComponent.deleteAlarm(TEST_ALARM_ID, CUSTOMER_ID);
		assertFalse(
				alarmComponent.findAlarmByAlarmId(TEST_ALARM_ID, CUSTOMER_ID).isPresent());
	}

	@Test
	public void testFilterByDeviceId() {
		Device device = TestUtils.getDevice();
		Alarm alarm = new Alarm(TEST_ALARM_ID, device.getClientId(), device.getId(), device.getSite());
		alarmComponent.updateAlarm(alarm);
		alarm = new Alarm(TokenGenerator.generateNewToken(), CUSTOMER_ID, "Test-" + 2, SITE);
		alarmComponent.updateAlarm(alarm);
		List<Alarm> alarms = alarmComponent.findAlarmsByDevice(device.getClientId(), device.getId());
		assertEquals(1, alarms.size());
		assertEquals(device.getId(), alarms.getFirst().getDeviceId());
	}

	@Test
	public void testFilterBySiteId() {
		Device device = TestUtils.getDevice();
		Alarm alarm = new Alarm(TEST_ALARM_ID, device.getClientId(), device.getId(), device.getSite());
		alarmComponent.updateAlarm(alarm);
		alarm = new Alarm(TokenGenerator.generateNewToken(), device.getClientId(), device.getId(), SITE + 1);
		alarmComponent.updateAlarm(alarm);
		List<Alarm> alarms = alarmComponent.findAlarmsBySite(CUSTOMER_ID, SITE);
		assertEquals(1, alarms.size());
		assertEquals(SITE, alarms.getFirst().getSiteId());
	}

	@Test
	public void testFilterAlarms() {
		Device device = TestUtils.getDevice();
		Alarm alarm = new Alarm(TEST_ALARM_ID, device.getClientId(), device.getId(), device.getSite());
		alarmComponent.updateAlarm(alarm);
		alarm = new Alarm(TokenGenerator.generateNewToken(), device.getClientId(), "Test-" + 2, device.getSite());
		alarmComponent.updateAlarm(alarm);
		alarm = new Alarm(TokenGenerator.generateNewToken(), device.getClientId(), "Test-" + 1, SITE + 1);
		alarmComponent.updateAlarm(alarm);

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
				2, alarmComponent.filterAlarms(device.getClientId(), SITE, null).size());
	}

	@Test
	public void testAlarmConditionDetected() throws InterruptedException {
		Device device = TestUtils.getDevice();
		assertNotNull(device);
		Optional<Alarm> alarm = alarmComponent.alarmConditionDetected(
				device.getClientId(), device.getId(), device.getSite(), "Test alarm!");
		assertTrue(alarm.isPresent());
		assertEquals(NEEDS_EMAIL, alarm.get().getEmailed());
		Thread.sleep(1000);
		Optional<Alarm> alarm2 = alarmComponent.alarmConditionDetected(
				device.getClientId(), device.getId(), device.getSite(), "Test alarm!");
		assertTrue(alarm2.isPresent());
		assertTrue(alarm.get().getLastUpdate() < alarm2.get().getLastUpdate());
		assertEquals(alarm.get().getAlarmId(), alarm2.get().getAlarmId());
		alarm2.get().setState(RESOLVED);
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
		alarm.get().setState(RESOLVED);
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
		Optional<Device> device = deviceComponent.findDeviceByDeviceName(CUSTOMER_ID, deviceName + 0);
		assertTrue(device.isPresent());
		assertFalse(alarmComponent.checkDevice(device.get()).isPresent());

		// test device, but old OpenSearch
		LocalDateTime ldt = LocalDateTime.now();
		generationComponent.handleDeviceBody(
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
		generationComponent.handleDeviceBody(
				TestUtils.getDeviceXML(
						deviceName + 0,
						Date.from(ldt.minusMinutes(59)
								.atZone(ZoneId.systemDefault())
								.toInstant()),
						(55)),
				CUSTOMER_ID);
		OpenSearchUtils.waitForIndexing();
		checkedAlarm = alarmComponent.checkDevice(device.get());
		assertFalse(checkedAlarm.isPresent());

		// Validate we have a record of the alarm and its properly "cleared"
		alarm = alarmComponent.findAlarmByAlarmId(
				alarm.get().getAlarmId(), alarm.get().getCustomerId());
		assertTrue(alarm.isPresent());
		assertEquals(RESOLVED, alarm.get().getState());
		assertTrue(alarm.get().getEndDate() > 0);

		// Check we create new alarm after current is cleared
		OSComponent.deleteByCustomerId(CUSTOMER_ID);
		generationComponent.handleDeviceBody(
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
		assertNotEquals(alarm.get().getAlarmId(), checkedAlarm.get().getAlarmId());

		// test disabled device, old OpenSearch
		alarmComponent.deleteAlarmsByCustomerId(CUSTOMER_ID);
		device.get().setDisabled(true);
		deviceComponent.updateDevice(device.get());
		assertTrue(deviceComponent
				.getDevice(device.get().getId(), device.get().getClientId())
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
		assertEquals(0, alarmComponent.quickCheckDevices().size());
		deviceUpdateComponent.update(device.getId(), System.currentTimeMillis() - TimeConstants.THIRTY_MINUTES + 2000);
		assertEquals(0, alarmComponent.quickCheckDevices().size());
		Thread.sleep(2000);
		assertEquals(1, alarmComponent.quickCheckDevices().size());
		alarmComponent.deleteAlarmsByCustomerId(CUSTOMER_ID);
	}

	@SneakyThrows
	@Test
	public void findNonEmailedAlarms() {
		assertTrue(alarmComponent.findNonEmailedAlarms(CUSTOMER_ID).isEmpty());
		Device device = TestUtils.getDevice();
		assertNotNull(device);
		Optional<Alarm> alarm = alarmComponent.alarmConditionDetected(
				device.getClientId(), device.getId(), device.getSite(), "Test alarm!");
		assertTrue(alarm.isPresent());
		assertEquals(NEEDS_EMAIL, alarm.get().getEmailed());
		Thread.sleep(1000);
		Optional<Alarm> alarm2 = alarmComponent.alarmConditionDetected(
				device.getClientId(), device.getId(), device.getSite(), "Test alarm!");
		assertTrue(alarm2.isPresent());

		assertEquals(1, alarmComponent.findNonEmailedAlarms(CUSTOMER_ID).size());

		alarmComponent.alarmConditionDetected(
				device.getClientId(), device.getId() + "invalild", device.getSite(), "Test alarm!");
		assertEquals(2, alarmComponent.findNonEmailedAlarms(CUSTOMER_ID).size());
		alarm.get().setEmailed(RESOLVED_NOT_EMAILED);
		alarmComponent.updateAlarm(alarm.get());
		assertEquals(1, alarmComponent.findNonEmailedAlarms(CUSTOMER_ID).size());
		assertTrue(alarmComponent.findNonEmailedAlarms(CUSTOMER_ID + "invalid").isEmpty());

		alarmComponent.alarmConditionDetected(CUSTOMER_ID + "invalid", device.getId(), device.getSite(), "Test alarm!");
		assertEquals(
				1, alarmComponent.findNonEmailedAlarms(CUSTOMER_ID + "invalid").size());
		assertEquals(1, alarmComponent.findNonEmailedAlarms(CUSTOMER_ID).size());
		assertEquals(2, alarmComponent.findNonEmailedAlarms().size());
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
				alarmComponent.faultDetected(CUSTOMER_ID, device.getId(), device.getSite(), "Error content");
		assertTrue(alarm.isPresent());
		assertEquals(DONT_EMAIL, alarm.get().getEmailed());
		assertEquals(ACTIVE, alarm.get().getState());
		assertEquals("Error content", alarm.get().getMessage());
		alarm = alarmComponent.faultDetected(CUSTOMER_ID, device.getId(), device.getSite(), "Error content2222");
		assertTrue(alarm.isPresent());
		assertEquals(DONT_EMAIL, alarm.get().getEmailed());
		assertEquals(ACTIVE, alarm.get().getState());
		assertEquals("Error content", alarm.get().getMessage());
		alarm = alarmComponent.alarmConditionDetected(CUSTOMER_ID, device.getId(), device.getSite(), "Error content2");
		assertTrue(alarm.isPresent());
		assertEquals(NEEDS_EMAIL, alarm.get().getEmailed());
		assertEquals(ACTIVE, alarm.get().getState());
		assertEquals("Error content", alarm.get().getMessage());
	}
}
