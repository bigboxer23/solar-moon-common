package com.bigboxer23.solar_moon;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.data.Alarm;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** */
public class TestAlarmComponent {
	public static final String TEST_ALARM_ID = "2883206e-9e35-4bdd-8c32-d1b35357b22f";
	private final AlarmComponent component = new AlarmComponent(new OpenWeatherComponent());

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
}
