package com.bigboxer23.solar_moon;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.data.Alarm;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.util.TokenGenerator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** */
public class TestAlarmComponent {
	public static final String TEST_ALARM_ID = "2883206e-9e35-4bdd-8c32-d1b35357b22f";

	private final DeviceComponent deviceComponent = new DeviceComponent(new SubscriptionComponent());

	private final AlarmComponent component = new AlarmComponent(new OpenWeatherComponent(), deviceComponent);

	@BeforeEach
	public void beforeEach() {
		component.deleteAlarmsByCustomerId(TestDeviceComponent.clientId);
	}

	@Test
	public void testAlarmCRUD() {
		Alarm alarm = new Alarm(TEST_ALARM_ID, TestDeviceComponent.clientId);
		Optional<Alarm> dbAlarm = component.updateAlarm(alarm);
		assertTrue(dbAlarm.isPresent());
		assertEquals(TEST_ALARM_ID, dbAlarm.get().getAlarmId());
		assertTrue(component
				.findAlarmByAlarmId(TEST_ALARM_ID, TestDeviceComponent.clientId)
				.isPresent());
		assertNull(dbAlarm.get().getSiteId());
		alarm.setState(1);
		alarm.setSiteId(TestDeviceComponent.SITE);
		dbAlarm = component.updateAlarm(alarm);
		assertTrue(dbAlarm.isPresent());
		assertEquals(TestDeviceComponent.SITE, dbAlarm.get().getSiteId());
		assertEquals(1, dbAlarm.get().getState());
		component.deleteAlarm(TEST_ALARM_ID, TestDeviceComponent.clientId);
		assertFalse(component
				.findAlarmByAlarmId(TEST_ALARM_ID, TestDeviceComponent.clientId)
				.isPresent());
	}

	@Test
	public void testFilterByDeviceId() {
		Alarm alarm = new Alarm(
				TEST_ALARM_ID, TestDeviceComponent.clientId, TestDeviceComponent.deviceId, TestDeviceComponent.SITE);
		component.updateAlarm(alarm);
		alarm = new Alarm(
				TokenGenerator.generateNewToken(),
				TestDeviceComponent.clientId,
				TestDeviceComponent.deviceId + 2,
				TestDeviceComponent.SITE);
		component.updateAlarm(alarm);
		List<Alarm> alarms = component.findAlarmsByDevice(TestDeviceComponent.clientId, TestDeviceComponent.deviceId);
		assertEquals(1, alarms.size());
		assertEquals(TestDeviceComponent.deviceId, alarms.get(0).getDeviceId());
	}

	@Test
	public void testFilterBySiteId() {
		Alarm alarm = new Alarm(
				TEST_ALARM_ID, TestDeviceComponent.clientId, TestDeviceComponent.deviceId, TestDeviceComponent.SITE);
		component.updateAlarm(alarm);
		alarm = new Alarm(
				TokenGenerator.generateNewToken(),
				TestDeviceComponent.clientId,
				TestDeviceComponent.deviceId,
				TestDeviceComponent.SITE + 1);
		component.updateAlarm(alarm);
		List<Alarm> alarms = component.findAlarmsBySite(TestDeviceComponent.clientId, TestDeviceComponent.SITE);
		assertEquals(1, alarms.size());
		assertEquals(TestDeviceComponent.SITE, alarms.get(0).getSiteId());
	}

	@Test
	public void testFilterAlarms() {
		Alarm alarm = new Alarm(
				TEST_ALARM_ID, TestDeviceComponent.clientId, TestDeviceComponent.deviceId, TestDeviceComponent.SITE);
		component.updateAlarm(alarm);
		alarm = new Alarm(
				TokenGenerator.generateNewToken(),
				TestDeviceComponent.clientId,
				TestDeviceComponent.deviceId + 2,
				TestDeviceComponent.SITE);
		component.updateAlarm(alarm);
		alarm = new Alarm(
				TokenGenerator.generateNewToken(),
				TestDeviceComponent.clientId,
				TestDeviceComponent.deviceId + 1,
				TestDeviceComponent.SITE + 1);
		component.updateAlarm(alarm);

		assertEquals(0, component.filterAlarms(null, null, null).size());
		assertEquals(
				3,
				component.filterAlarms(TestDeviceComponent.clientId, null, null).size());
		assertEquals(
				0, component.filterAlarms(null, TestDeviceComponent.SITE, null).size());
		assertEquals(
				0,
				component
						.filterAlarms(null, TestDeviceComponent.SITE, TestDeviceComponent.deviceId)
						.size());
		assertEquals(
				1,
				component
						.filterAlarms(
								TestDeviceComponent.clientId, TestDeviceComponent.SITE, TestDeviceComponent.deviceId)
						.size());
		assertEquals(
				1,
				component
						.filterAlarms(TestDeviceComponent.clientId, null, TestDeviceComponent.deviceId)
						.size());
		assertEquals(
				2,
				component
						.filterAlarms(TestDeviceComponent.clientId, TestDeviceComponent.SITE, null)
						.size());
	}

	@Test
	public void testAlarmConditionDetected() throws InterruptedException {
		Optional<Alarm> alarm = component.alarmConditionDetected(
				TestDeviceComponent.clientId,
				new DeviceData(
						TestDeviceComponent.SITE,
						TestDeviceComponent.deviceName,
						TestDeviceComponent.clientId,
						TestDeviceComponent.deviceId),
				"Test alarm!");
		assertTrue(alarm.isPresent());
		Thread.sleep(1000);
		Optional<Alarm> alarm2 = component.alarmConditionDetected(
				TestDeviceComponent.clientId,
				new DeviceData(
						TestDeviceComponent.SITE,
						TestDeviceComponent.deviceName,
						TestDeviceComponent.clientId,
						TestDeviceComponent.deviceId),
				"Test alarm!");
		assertTrue(alarm2.isPresent());
		assertTrue(alarm.get().getLastUpdate() < alarm2.get().getLastUpdate());
		assertEquals(alarm.get().getAlarmId(), alarm2.get().getAlarmId());
		alarm2.get().setState(0);
		component.updateAlarm(alarm2.get());
		alarm2 = component.alarmConditionDetected(
				TestDeviceComponent.clientId,
				new DeviceData(
						TestDeviceComponent.SITE,
						TestDeviceComponent.deviceName,
						TestDeviceComponent.clientId,
						TestDeviceComponent.deviceId),
				"Test alarm!");
		assertTrue(alarm2.isPresent());
		assertNotEquals(alarm.get().getAlarmId(), alarm2.get().getAlarmId());
	}
}
