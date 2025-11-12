package com.bigboxer23.solar_moon.alarm;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.data.Alarm;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AlarmComponentTest {

	@Mock
	private AlarmRepository mockRepository;

	private TestableAlarmComponent alarmComponent;

	private static final String ALARM_ID = "alarm-123";
	private static final String CUSTOMER_ID = "customer-123";
	private static final String DEVICE_ID = "device-123";
	private static final String SITE_ID = "site-123";
	private static final String MESSAGE = "Test alarm message";

	private static class TestableAlarmComponent extends AlarmComponent {
		private final AlarmRepository repository;

		public TestableAlarmComponent(AlarmRepository repository) {
			this.repository = repository;
		}

		@Override
		protected AlarmRepository getRepository() {
			return repository;
		}
	}

	@BeforeEach
	void setUp() {
		alarmComponent = new TestableAlarmComponent(mockRepository);
	}

	@Test
	void testGetMostRecentAlarm_delegatesToRepository() {
		Alarm expectedAlarm = createTestAlarm();
		when(mockRepository.findMostRecentAlarm(DEVICE_ID)).thenReturn(Optional.of(expectedAlarm));

		Optional<Alarm> result = alarmComponent.getMostRecentAlarm(DEVICE_ID);

		assertTrue(result.isPresent());
		assertEquals(expectedAlarm, result.get());
		verify(mockRepository).findMostRecentAlarm(DEVICE_ID);
	}

	@Test
	void testFindAlarmsByDevice_delegatesToRepository() {
		List<Alarm> expectedAlarms = Arrays.asList(createTestAlarm());
		when(mockRepository.findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID)).thenReturn(expectedAlarms);

		List<Alarm> result = alarmComponent.findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID);

		assertEquals(expectedAlarms, result);
		verify(mockRepository).findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID);
	}

	@Test
	void testFindAlarmsBySite_delegatesToRepository() {
		List<Alarm> expectedAlarms = Arrays.asList(createTestAlarm());
		when(mockRepository.findAlarmsBySite(CUSTOMER_ID, SITE_ID)).thenReturn(expectedAlarms);

		List<Alarm> result = alarmComponent.findAlarmsBySite(CUSTOMER_ID, SITE_ID);

		assertEquals(expectedAlarms, result);
		verify(mockRepository).findAlarmsBySite(CUSTOMER_ID, SITE_ID);
	}

	@Test
	void testFindNonEmailedAlarms_delegatesToRepository() {
		List<Alarm> expectedAlarms = Arrays.asList(createTestAlarm());
		when(mockRepository.findNonEmailedAlarms(CUSTOMER_ID)).thenReturn(expectedAlarms);

		List<Alarm> result = alarmComponent.findNonEmailedAlarms(CUSTOMER_ID);

		assertEquals(expectedAlarms, result);
		verify(mockRepository).findNonEmailedAlarms(CUSTOMER_ID);
	}

	@Test
	void testFindNonEmailedActiveAlarms_delegatesToRepository() {
		List<Alarm> expectedAlarms = Arrays.asList(createTestAlarm());
		when(mockRepository.findNonEmailedActiveAlarms()).thenReturn(expectedAlarms);

		List<Alarm> result = alarmComponent.findNonEmailedActiveAlarms();

		assertEquals(expectedAlarms, result);
		verify(mockRepository).findNonEmailedActiveAlarms();
	}

	@Test
	void testFindNonEmailedResolvedAlarms_delegatesToRepository() {
		List<Alarm> expectedAlarms = Arrays.asList(createTestAlarm());
		when(mockRepository.findNonEmailedResolvedAlarms()).thenReturn(expectedAlarms);

		List<Alarm> result = alarmComponent.findNonEmailedResolvedAlarms();

		assertEquals(expectedAlarms, result);
		verify(mockRepository).findNonEmailedResolvedAlarms();
	}

	@Test
	void testGetAlarms_delegatesToRepository() {
		List<Alarm> expectedAlarms = Arrays.asList(createTestAlarm());
		when(mockRepository.findAlarms(CUSTOMER_ID)).thenReturn(expectedAlarms);

		List<Alarm> result = alarmComponent.getAlarms(CUSTOMER_ID);

		assertEquals(expectedAlarms, result);
		verify(mockRepository).findAlarms(CUSTOMER_ID);
	}

	@Test
	void testFindAlarmByAlarmId_delegatesToRepository() {
		Alarm expectedAlarm = createTestAlarm();
		when(mockRepository.findAlarmByAlarmId(ALARM_ID, CUSTOMER_ID)).thenReturn(Optional.of(expectedAlarm));

		Optional<Alarm> result = alarmComponent.findAlarmByAlarmId(ALARM_ID, CUSTOMER_ID);

		assertTrue(result.isPresent());
		assertEquals(expectedAlarm, result.get());
		verify(mockRepository).findAlarmByAlarmId(ALARM_ID, CUSTOMER_ID);
	}

	@Test
	void testGetActiveAlarms_delegatesToRepository() {
		List<Alarm> expectedAlarms = Arrays.asList(createTestAlarm());
		when(mockRepository.findActiveAlarms()).thenReturn(expectedAlarms);

		List<Alarm> result = alarmComponent.getActiveAlarms();

		assertEquals(expectedAlarms, result);
		verify(mockRepository).findActiveAlarms();
	}

	@Test
	void testUpdateAlarm_withValidAlarm_updatesSuccessfully() {
		Alarm alarm = createTestAlarm();
		when(mockRepository.update(alarm)).thenReturn(Optional.of(alarm));

		Optional<Alarm> result = alarmComponent.updateAlarm(alarm);

		assertTrue(result.isPresent());
		assertEquals(alarm, result.get());
		verify(mockRepository).update(alarm);
	}

	@Test
	void testUpdateAlarm_withNullAlarm_doesNotUpdate() {
		Optional<Alarm> result = alarmComponent.updateAlarm(null);

		assertFalse(result.isPresent());
		verify(mockRepository, never()).update(any(Alarm.class));
	}

	@Test
	void testUpdateAlarm_withNullCustomerId_doesNotUpdate() {
		Alarm alarm = createTestAlarm();
		alarm.setCustomerId(null);

		Optional<Alarm> result = alarmComponent.updateAlarm(alarm);

		assertFalse(result.isPresent());
		verify(mockRepository, never()).update(any(Alarm.class));
	}

	@Test
	void testUpdateAlarm_withNullAlarmId_doesNotUpdate() {
		Alarm alarm = createTestAlarm();
		alarm.setAlarmId(null);

		Optional<Alarm> result = alarmComponent.updateAlarm(alarm);

		assertFalse(result.isPresent());
		verify(mockRepository, never()).update(any(Alarm.class));
	}

	@Test
	void testDeleteAlarm_deletesSuccessfully() {
		alarmComponent.deleteAlarm(ALARM_ID, CUSTOMER_ID);

		verify(mockRepository).delete(any(Alarm.class));
	}

	@Test
	void testDeleteAlarmByDeviceId_deletesAllAlarmsForDevice() {
		Alarm alarm1 = createTestAlarm();
		Alarm alarm2 = createTestAlarm();
		alarm2.setAlarmId("alarm-456");
		List<Alarm> alarms = Arrays.asList(alarm1, alarm2);

		when(mockRepository.findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID)).thenReturn(alarms);

		alarmComponent.deleteAlarmByDeviceId(CUSTOMER_ID, DEVICE_ID);

		verify(mockRepository).findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID);
		verify(mockRepository, times(2)).delete(any(Alarm.class));
	}

	@Test
	void testDeleteAlarmsByCustomerId_deletesAllAlarmsForCustomer() {
		Alarm alarm1 = createTestAlarm();
		Alarm alarm2 = createTestAlarm();
		alarm2.setAlarmId("alarm-456");
		List<Alarm> alarms = Arrays.asList(alarm1, alarm2);

		when(mockRepository.findAlarms(CUSTOMER_ID)).thenReturn(alarms);

		alarmComponent.deleteAlarmsByCustomerId(CUSTOMER_ID);

		verify(mockRepository).findAlarms(CUSTOMER_ID);
		verify(mockRepository, times(2)).delete(any(Alarm.class));
	}

	@Test
	void testFilterAlarms_withBlankCustomerId_returnsEmptyList() {
		List<Alarm> result = alarmComponent.filterAlarms("", SITE_ID, DEVICE_ID);

		assertTrue(result.isEmpty());
		verify(mockRepository, never()).findAlarmsByDevice(anyString(), anyString());
		verify(mockRepository, never()).findAlarmsBySite(anyString(), anyString());
		verify(mockRepository, never()).findAlarms(anyString());
	}

	@Test
	void testFilterAlarms_withDeviceId_returnsDeviceAlarms() {
		List<Alarm> expectedAlarms = Arrays.asList(createTestAlarm());
		when(mockRepository.findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID)).thenReturn(expectedAlarms);

		List<Alarm> result = alarmComponent.filterAlarms(CUSTOMER_ID, SITE_ID, DEVICE_ID);

		assertEquals(expectedAlarms, result);
		verify(mockRepository).findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID);
		verify(mockRepository, never()).findAlarmsBySite(anyString(), anyString());
	}

	@Test
	void testFilterAlarms_withSiteIdOnly_returnsSiteAlarms() {
		List<Alarm> expectedAlarms = Arrays.asList(createTestAlarm());
		when(mockRepository.findAlarmsBySite(CUSTOMER_ID, SITE_ID)).thenReturn(expectedAlarms);

		List<Alarm> result = alarmComponent.filterAlarms(CUSTOMER_ID, SITE_ID, "");

		assertEquals(expectedAlarms, result);
		verify(mockRepository).findAlarmsBySite(CUSTOMER_ID, SITE_ID);
		verify(mockRepository, never()).findAlarmsByDevice(anyString(), anyString());
	}

	@Test
	void testFilterAlarms_withOnlyCustomerId_returnsAllCustomerAlarms() {
		List<Alarm> expectedAlarms = Arrays.asList(createTestAlarm());
		when(mockRepository.findAlarms(CUSTOMER_ID)).thenReturn(expectedAlarms);

		List<Alarm> result = alarmComponent.filterAlarms(CUSTOMER_ID, "", "");

		assertEquals(expectedAlarms, result);
		verify(mockRepository).findAlarms(CUSTOMER_ID);
		verify(mockRepository, never()).findAlarmsByDevice(anyString(), anyString());
		verify(mockRepository, never()).findAlarmsBySite(anyString(), anyString());
	}

	@Test
	void testGetMostRecentAlarm_withNonExistentDevice_returnsEmpty() {
		when(mockRepository.findMostRecentAlarm(DEVICE_ID)).thenReturn(Optional.empty());

		Optional<Alarm> result = alarmComponent.getMostRecentAlarm(DEVICE_ID);

		assertFalse(result.isPresent());
		verify(mockRepository).findMostRecentAlarm(DEVICE_ID);
	}

	@Test
	void testFindAlarmByAlarmId_withNonExistentAlarm_returnsEmpty() {
		when(mockRepository.findAlarmByAlarmId(ALARM_ID, CUSTOMER_ID)).thenReturn(Optional.empty());

		Optional<Alarm> result = alarmComponent.findAlarmByAlarmId(ALARM_ID, CUSTOMER_ID);

		assertFalse(result.isPresent());
		verify(mockRepository).findAlarmByAlarmId(ALARM_ID, CUSTOMER_ID);
	}

	@Test
	void testDeleteAlarmByDeviceId_withNoAlarms_doesNotCallDelete() {
		when(mockRepository.findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID)).thenReturn(Collections.emptyList());

		alarmComponent.deleteAlarmByDeviceId(CUSTOMER_ID, DEVICE_ID);

		verify(mockRepository).findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID);
		verify(mockRepository, never()).delete(any(Alarm.class));
	}

	@Test
	void testDeleteAlarmsByCustomerId_withNoAlarms_doesNotCallDelete() {
		when(mockRepository.findAlarms(CUSTOMER_ID)).thenReturn(Collections.emptyList());

		alarmComponent.deleteAlarmsByCustomerId(CUSTOMER_ID);

		verify(mockRepository).findAlarms(CUSTOMER_ID);
		verify(mockRepository, never()).delete(any(Alarm.class));
	}

	private Alarm createTestAlarm() {
		Alarm alarm = new Alarm(ALARM_ID, CUSTOMER_ID, DEVICE_ID, SITE_ID);
		alarm.setMessage(MESSAGE);
		alarm.setState(IAlarmConstants.ACTIVE);
		alarm.setStartDate(System.currentTimeMillis());
		return alarm;
	}
}
