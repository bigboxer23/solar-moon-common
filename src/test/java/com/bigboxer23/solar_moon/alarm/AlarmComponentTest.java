package com.bigboxer23.solar_moon.alarm;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.data.Alarm;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.data.LinkedDevice;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AlarmComponentTest implements IAlarmConstants {

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

		@Override
		protected Optional<LinkedDevice> isLinkedDeviceErrored(DeviceData deviceData, Device device) {
			return Optional.empty();
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
		List<Alarm> expectedAlarms = List.of(createTestAlarm());
		when(mockRepository.findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID)).thenReturn(expectedAlarms);

		List<Alarm> result = alarmComponent.findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID);

		assertEquals(expectedAlarms, result);
		verify(mockRepository).findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID);
	}

	@Test
	void testFindAlarmsBySite_delegatesToRepository() {
		List<Alarm> expectedAlarms = List.of(createTestAlarm());
		when(mockRepository.findAlarmsBySite(CUSTOMER_ID, SITE_ID)).thenReturn(expectedAlarms);

		List<Alarm> result = alarmComponent.findAlarmsBySite(CUSTOMER_ID, SITE_ID);

		assertEquals(expectedAlarms, result);
		verify(mockRepository).findAlarmsBySite(CUSTOMER_ID, SITE_ID);
	}

	@Test
	void testFindNonEmailedAlarms_delegatesToRepository() {
		List<Alarm> expectedAlarms = List.of(createTestAlarm());
		when(mockRepository.findNonEmailedAlarms(CUSTOMER_ID)).thenReturn(expectedAlarms);

		List<Alarm> result = alarmComponent.findNonEmailedAlarms(CUSTOMER_ID);

		assertEquals(expectedAlarms, result);
		verify(mockRepository).findNonEmailedAlarms(CUSTOMER_ID);
	}

	@Test
	void testFindNonEmailedActiveAlarms_delegatesToRepository() {
		List<Alarm> expectedAlarms = List.of(createTestAlarm());
		when(mockRepository.findNonEmailedActiveAlarms()).thenReturn(expectedAlarms);

		List<Alarm> result = alarmComponent.findNonEmailedActiveAlarms();

		assertEquals(expectedAlarms, result);
		verify(mockRepository).findNonEmailedActiveAlarms();
	}

	@Test
	void testFindNonEmailedResolvedAlarms_delegatesToRepository() {
		List<Alarm> expectedAlarms = List.of(createTestAlarm());
		when(mockRepository.findNonEmailedResolvedAlarms()).thenReturn(expectedAlarms);

		List<Alarm> result = alarmComponent.findNonEmailedResolvedAlarms();

		assertEquals(expectedAlarms, result);
		verify(mockRepository).findNonEmailedResolvedAlarms();
	}

	@Test
	void testGetAlarms_delegatesToRepository() {
		List<Alarm> expectedAlarms = List.of(createTestAlarm());
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
		List<Alarm> expectedAlarms = List.of(createTestAlarm());
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
		List<Alarm> expectedAlarms = List.of(createTestAlarm());
		when(mockRepository.findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID)).thenReturn(expectedAlarms);

		List<Alarm> result = alarmComponent.filterAlarms(CUSTOMER_ID, SITE_ID, DEVICE_ID);

		assertEquals(expectedAlarms, result);
		verify(mockRepository).findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID);
		verify(mockRepository, never()).findAlarmsBySite(anyString(), anyString());
	}

	@Test
	void testFilterAlarms_withSiteIdOnly_returnsSiteAlarms() {
		List<Alarm> expectedAlarms = List.of(createTestAlarm());
		when(mockRepository.findAlarmsBySite(CUSTOMER_ID, SITE_ID)).thenReturn(expectedAlarms);

		List<Alarm> result = alarmComponent.filterAlarms(CUSTOMER_ID, SITE_ID, "");

		assertEquals(expectedAlarms, result);
		verify(mockRepository).findAlarmsBySite(CUSTOMER_ID, SITE_ID);
		verify(mockRepository, never()).findAlarmsByDevice(anyString(), anyString());
	}

	@Test
	void testFilterAlarms_withOnlyCustomerId_returnsAllCustomerAlarms() {
		List<Alarm> expectedAlarms = List.of(createTestAlarm());
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

	@Test
	void testResolveActiveAlarms_withOldData_doesNotResolveAlarm() {
		DeviceData oldData = new DeviceData();
		oldData.setDate(new Date(System.currentTimeMillis() - 2 * 60 * 60 * 1000));
		oldData.setDeviceId(DEVICE_ID);

		alarmComponent.resolveActiveAlarms(oldData);

		verify(mockRepository, never()).findMostRecentAlarm(anyString());
		verify(mockRepository, never()).update(any(Alarm.class));
	}

	@Test
	void testResolveActiveAlarms_withNullDate_doesNotResolveAlarm() {
		DeviceData deviceData = new DeviceData();
		deviceData.setDate(null);
		deviceData.setDeviceId(DEVICE_ID);

		alarmComponent.resolveActiveAlarms(deviceData);

		verify(mockRepository, never()).findMostRecentAlarm(anyString());
	}

	@Test
	void testResolveActiveAlarms_withNoActiveAlarm_doesNothing() {
		DeviceData deviceData = createValidDeviceData();
		when(mockRepository.findMostRecentAlarm(DEVICE_ID)).thenReturn(Optional.empty());

		alarmComponent.resolveActiveAlarms(deviceData);

		verify(mockRepository).findMostRecentAlarm(DEVICE_ID);
		verify(mockRepository, never()).update(any(Alarm.class));
	}

	@Test
	void testResolveActiveAlarms_withResolvedAlarm_doesNothing() {
		DeviceData deviceData = createValidDeviceData();
		Alarm resolvedAlarm = createTestAlarm();
		resolvedAlarm.setState(RESOLVED);
		when(mockRepository.findMostRecentAlarm(DEVICE_ID)).thenReturn(Optional.of(resolvedAlarm));

		alarmComponent.resolveActiveAlarms(deviceData);

		verify(mockRepository).findMostRecentAlarm(DEVICE_ID);
		verify(mockRepository, never()).update(any(Alarm.class));
	}

	@Test
	void testResolveActiveAlarms_notDaylightAndNotNoDataAlarm_doesNotResolve() {
		DeviceData deviceData = createValidDeviceData();
		deviceData.setDaylight(false);
		Alarm activeAlarm = createTestAlarm();
		activeAlarm.setMessage("Some other error");
		when(mockRepository.findMostRecentAlarm(DEVICE_ID)).thenReturn(Optional.of(activeAlarm));

		alarmComponent.resolveActiveAlarms(deviceData);

		verify(mockRepository, never()).update(any(Alarm.class));
	}

	@Test
	void testResolveActiveAlarms_noPowerAndNotNoDataAlarm_doesNotResolve() {
		DeviceData deviceData = createValidDeviceData();
		deviceData.setTotalRealPower(0.0f);
		Alarm activeAlarm = createTestAlarm();
		activeAlarm.setMessage("Some other error");
		when(mockRepository.findMostRecentAlarm(DEVICE_ID)).thenReturn(Optional.of(activeAlarm));

		alarmComponent.resolveActiveAlarms(deviceData);

		verify(mockRepository, never()).update(any(Alarm.class));
	}

	@Test
	void testResolveActiveAlarms_withNoDataAlarm_resolvesAlarm() {
		DeviceData deviceData = createValidDeviceData();
		Alarm activeAlarm = createTestAlarm();
		activeAlarm.setMessage(NO_DATA_RECENTLY + "12345");
		activeAlarm.setEmailed(NEEDS_EMAIL);
		when(mockRepository.findMostRecentAlarm(DEVICE_ID)).thenReturn(Optional.of(activeAlarm));
		when(mockRepository.update(any(Alarm.class))).thenReturn(Optional.of(activeAlarm));

		alarmComponent.resolveActiveAlarms(deviceData);

		verify(mockRepository)
				.update(argThat(alarm -> alarm.getState() == RESOLVED
						&& alarm.getEmailed() == RESOLVED_NOT_EMAILED
						&& alarm.getEndDate() > 0));
	}

	@Test
	void testResolveActiveAlarms_withEmailedAlarm_setsResolveEmailFlag() {
		DeviceData deviceData = createValidDeviceData();
		Alarm activeAlarm = createTestAlarm();
		activeAlarm.setEmailed(System.currentTimeMillis());
		when(mockRepository.findMostRecentAlarm(DEVICE_ID)).thenReturn(Optional.of(activeAlarm));
		when(mockRepository.update(any(Alarm.class))).thenReturn(Optional.of(activeAlarm));

		alarmComponent.resolveActiveAlarms(deviceData);

		verify(mockRepository).update(argThat(alarm -> alarm.getResolveEmailed() == NEEDS_EMAIL));
	}

	@Test
	void testFaultDetected_withNoExistingAlarm_createsNewFaultAlarm() {
		String content = "Device fault detected";
		when(mockRepository.findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID)).thenReturn(Collections.emptyList());
		when(mockRepository.update(any(Alarm.class))).thenAnswer(invocation -> Optional.of(invocation.getArgument(0)));

		Optional<Alarm> result = alarmComponent.faultDetected(CUSTOMER_ID, DEVICE_ID, SITE_ID, content);

		assertTrue(result.isPresent());
		verify(mockRepository)
				.update(argThat(alarm -> alarm.getEmailed() == DONT_EMAIL
						&& alarm.getState() == ACTIVE
						&& alarm.getMessage().equals(content)));
	}

	@Test
	void testFaultDetected_withExistingActiveAlarm_updatesAlarm() {
		String content = "Device fault detected";
		Alarm existingAlarm = createTestAlarm();
		existingAlarm.setEmailed(DONT_EMAIL);
		when(mockRepository.findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID)).thenReturn(List.of(existingAlarm));
		when(mockRepository.update(any(Alarm.class))).thenAnswer(invocation -> Optional.of(invocation.getArgument(0)));

		Optional<Alarm> result = alarmComponent.faultDetected(CUSTOMER_ID, DEVICE_ID, SITE_ID, content);

		assertTrue(result.isPresent());
		verify(mockRepository)
				.update(argThat(alarm -> alarm.getAlarmId().equals(ALARM_ID) && alarm.getLastUpdate() > 0));
	}

	@Test
	void testAlarmConditionDetected_withNoExistingAlarm_createsNewAlarm() {
		String content = "Alarm condition detected";
		when(mockRepository.findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID)).thenReturn(Collections.emptyList());
		when(mockRepository.update(any(Alarm.class))).thenAnswer(invocation -> Optional.of(invocation.getArgument(0)));

		Optional<Alarm> result = alarmComponent.alarmConditionDetected(CUSTOMER_ID, DEVICE_ID, SITE_ID, content);

		assertTrue(result.isPresent());
		verify(mockRepository)
				.update(argThat(alarm -> alarm.getState() == ACTIVE
						&& alarm.getMessage().equals(content)
						&& alarm.getEmailed() == NEEDS_EMAIL));
	}

	@Test
	void testAlarmConditionDetected_withExistingFaultAlarm_upgradesToEmailAlarm() {
		String content = "Alarm condition detected";
		Alarm existingFault = createTestAlarm();
		existingFault.setEmailed(DONT_EMAIL);
		existingFault.setMessage("");
		when(mockRepository.findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID)).thenReturn(List.of(existingFault));
		when(mockRepository.update(any(Alarm.class))).thenAnswer(invocation -> Optional.of(invocation.getArgument(0)));

		Optional<Alarm> result = alarmComponent.alarmConditionDetected(CUSTOMER_ID, DEVICE_ID, SITE_ID, content);

		assertTrue(result.isPresent());
		verify(mockRepository)
				.update(argThat(alarm ->
						alarm.getEmailed() == NEEDS_EMAIL && alarm.getMessage().equals(content)));
	}

	@Test
	void testGetNewAlarm_createsAlarmWithCorrectProperties() {
		String content = "Test alarm content";

		Alarm result = alarmComponent.getNewAlarm(CUSTOMER_ID, DEVICE_ID, SITE_ID, content);

		assertNotNull(result);
		assertEquals(CUSTOMER_ID, result.getCustomerId());
		assertEquals(DEVICE_ID, result.getDeviceId());
		assertEquals(SITE_ID, result.getSiteId());
		assertEquals(content, result.getMessage());
		assertEquals(ACTIVE, result.getState());
		assertEquals(NEEDS_EMAIL, result.getEmailed());
		assertTrue(result.getStartDate() > 0);
		assertNotNull(result.getAlarmId());
	}

	@Test
	void testCleanupOldAlarms_deletesOldResolvedAndActiveAlarms() {
		Alarm oldResolvedAlarm = createTestAlarm();
		oldResolvedAlarm.setState(RESOLVED);
		Alarm oldActiveAlarm = createTestAlarm();
		oldActiveAlarm.setAlarmId("alarm-456");
		oldActiveAlarm.setState(ACTIVE);

		when(mockRepository.findAlarmsByStateAndDateLessThan(eq(RESOLVED), anyLong()))
				.thenReturn(List.of(oldResolvedAlarm));
		when(mockRepository.findAlarmsByStateAndDateLessThan(eq(ACTIVE), anyLong()))
				.thenReturn(List.of(oldActiveAlarm));

		alarmComponent.cleanupOldAlarms();

		verify(mockRepository).findAlarmsByStateAndDateLessThan(eq(RESOLVED), anyLong());
		verify(mockRepository).findAlarmsByStateAndDateLessThan(eq(ACTIVE), anyLong());
		verify(mockRepository, times(2)).delete(any(Alarm.class));
	}

	@Test
	void testCleanupOldAlarms_withNoOldAlarms_doesNotDelete() {
		when(mockRepository.findAlarmsByStateAndDateLessThan(anyInt(), anyLong()))
				.thenReturn(Collections.emptyList());

		alarmComponent.cleanupOldAlarms();

		verify(mockRepository, never()).delete(any(Alarm.class));
	}

	@Test
	void testIsLinkedDeviceErrored_withNullDeviceData_returnsEmpty() {
		Optional<LinkedDevice> result = alarmComponent.isLinkedDeviceErrored(null, null);

		assertFalse(result.isPresent());
	}

	private DeviceData createValidDeviceData() {
		DeviceData deviceData = new DeviceData();
		deviceData.setDeviceId(DEVICE_ID);
		deviceData.setCustomerId(CUSTOMER_ID);
		deviceData.setSiteId(SITE_ID);
		deviceData.setDate(new Date());
		deviceData.setDaylight(true);
		deviceData.setTotalRealPower(5.0f);
		return deviceData;
	}

	private Alarm createTestAlarm() {
		Alarm alarm = new Alarm(ALARM_ID, CUSTOMER_ID, DEVICE_ID, SITE_ID);
		alarm.setMessage(MESSAGE);
		alarm.setState(ACTIVE);
		alarm.setStartDate(System.currentTimeMillis());
		return alarm;
	}
}
