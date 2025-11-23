package com.bigboxer23.solar_moon.alarm;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.data.Alarm;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.data.LinkedDevice;
import com.bigboxer23.solar_moon.device.DeviceComponent;
import com.bigboxer23.solar_moon.device.DeviceUpdateComponent;
import com.bigboxer23.solar_moon.maintenance.MaintenanceComponent;
import com.bigboxer23.solar_moon.util.TimeConstants;
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

	@Mock
	private DeviceComponent mockDeviceComponent;

	@Mock
	private DeviceUpdateComponent mockDeviceUpdateComponent;

	@Mock
	private MaintenanceComponent mockMaintenanceComponent;

	@Mock
	private com.bigboxer23.solar_moon.device.LinkedDeviceComponent mockLinkedDeviceComponent;

	@Mock
	private com.bigboxer23.solar_moon.location.LocationComponent mockLocationComponent;

	@Mock
	private com.bigboxer23.solar_moon.search.OpenSearchComponent mockOpenSearchComponent;

	@Mock
	private com.bigboxer23.solar_moon.search.status.OpenSearchStatusComponent mockOpenSearchStatusComponent;

	@Mock
	private com.bigboxer23.solar_moon.notifications.NotificationComponent mockNotificationComponent;

	private TestableAlarmComponent alarmComponent;

	private static final String ALARM_ID = "alarm-123";
	private static final String CUSTOMER_ID = "customer-123";
	private static final String DEVICE_ID = "device-123";
	private static final String SITE_ID = "site-123";
	private static final String MESSAGE = "Test alarm message";

	private static class TestableAlarmComponent extends AlarmComponent {
		private final AlarmRepository repository;
		private final DeviceComponent deviceComponent;
		private final DeviceUpdateComponent deviceUpdateComponent;
		private final MaintenanceComponent maintenanceComponent;
		private final com.bigboxer23.solar_moon.device.LinkedDeviceComponent linkedDeviceComponent;
		private final com.bigboxer23.solar_moon.location.LocationComponent locationComponent;
		private final com.bigboxer23.solar_moon.search.OpenSearchComponent openSearchComponent;
		private final com.bigboxer23.solar_moon.search.status.OpenSearchStatusComponent openSearchStatusComponent;
		private final com.bigboxer23.solar_moon.notifications.NotificationComponent notificationComponent;
		private Optional<LinkedDevice> linkedDeviceErrorResult = Optional.empty();
		private boolean useRealLinkedDeviceErrored = false;

		public TestableAlarmComponent(
				AlarmRepository repository,
				DeviceComponent deviceComponent,
				DeviceUpdateComponent deviceUpdateComponent,
				MaintenanceComponent maintenanceComponent,
				com.bigboxer23.solar_moon.device.LinkedDeviceComponent linkedDeviceComponent,
				com.bigboxer23.solar_moon.location.LocationComponent locationComponent,
				com.bigboxer23.solar_moon.search.OpenSearchComponent openSearchComponent,
				com.bigboxer23.solar_moon.search.status.OpenSearchStatusComponent openSearchStatusComponent,
				com.bigboxer23.solar_moon.notifications.NotificationComponent notificationComponent) {
			this.repository = repository;
			this.deviceComponent = deviceComponent;
			this.deviceUpdateComponent = deviceUpdateComponent;
			this.maintenanceComponent = maintenanceComponent;
			this.linkedDeviceComponent = linkedDeviceComponent;
			this.locationComponent = locationComponent;
			this.openSearchComponent = openSearchComponent;
			this.openSearchStatusComponent = openSearchStatusComponent;
			this.notificationComponent = notificationComponent;
		}

		@Override
		protected AlarmRepository getRepository() {
			return repository;
		}

		@Override
		protected DeviceComponent getDeviceComponent() {
			return deviceComponent;
		}

		@Override
		protected DeviceUpdateComponent getDeviceUpdateComponent() {
			return deviceUpdateComponent;
		}

		@Override
		protected MaintenanceComponent getMaintenanceComponent() {
			return maintenanceComponent;
		}

		@Override
		protected com.bigboxer23.solar_moon.device.LinkedDeviceComponent getLinkedDeviceComponent() {
			return linkedDeviceComponent;
		}

		@Override
		protected com.bigboxer23.solar_moon.location.LocationComponent getLocationComponent() {
			return locationComponent;
		}

		@Override
		protected com.bigboxer23.solar_moon.search.OpenSearchComponent getOpenSearchComponent() {
			return openSearchComponent;
		}

		@Override
		protected com.bigboxer23.solar_moon.search.status.OpenSearchStatusComponent getOpenSearchStatusComponent() {
			return openSearchStatusComponent;
		}

		@Override
		protected com.bigboxer23.solar_moon.notifications.NotificationComponent getNotificationComponent() {
			return notificationComponent;
		}

		@Override
		protected Optional<LinkedDevice> isLinkedDeviceErrored(DeviceData deviceData, Device device) {
			if (useRealLinkedDeviceErrored) {
				return super.isLinkedDeviceErrored(deviceData, device);
			}
			return linkedDeviceErrorResult;
		}

		public void setLinkedDeviceErrorResult(Optional<LinkedDevice> result) {
			this.linkedDeviceErrorResult = result;
		}

		public void setUseRealLinkedDeviceErrored(boolean use) {
			this.useRealLinkedDeviceErrored = use;
		}
	}

	@BeforeEach
	void setUp() {
		alarmComponent = new TestableAlarmComponent(
				mockRepository,
				mockDeviceComponent,
				mockDeviceUpdateComponent,
				mockMaintenanceComponent,
				mockLinkedDeviceComponent,
				mockLocationComponent,
				mockOpenSearchComponent,
				mockOpenSearchStatusComponent,
				mockNotificationComponent);
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

		verify(mockDeviceUpdateComponent).update(DEVICE_ID);
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

		verify(mockDeviceUpdateComponent).update(DEVICE_ID);
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

		verify(mockDeviceUpdateComponent).update(DEVICE_ID);
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

		verify(mockDeviceUpdateComponent).update(DEVICE_ID);
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

		verify(mockDeviceUpdateComponent).update(DEVICE_ID);
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

		verify(mockDeviceUpdateComponent).update(DEVICE_ID);
		verify(mockRepository).update(argThat(alarm -> alarm.getResolveEmailed() == NEEDS_EMAIL));
	}

	@Test
	void testFaultDetected_withNoExistingAlarm_createsNewFaultAlarm() {
		String content = "Device fault detected";
		when(mockRepository.findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID)).thenReturn(Collections.emptyList());
		when(mockRepository.update(any(Alarm.class))).thenAnswer(invocation -> Optional.of(invocation.getArgument(0)));
		when(mockDeviceComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.empty());

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

	@Test
	void testAlarmConditionDetected_duringMaintenanceMode_returnsEmpty() {
		String content = "Alarm condition detected";
		when(mockMaintenanceComponent.isInMaintenanceMode()).thenReturn(true);

		Optional<Alarm> result = alarmComponent.alarmConditionDetected(CUSTOMER_ID, DEVICE_ID, SITE_ID, content);

		assertFalse(result.isPresent());
		verify(mockRepository, never()).update(any(Alarm.class));
		verify(mockMaintenanceComponent).isInMaintenanceMode();
	}

	@Test
	void testAlarmConditionDetected_withDeviceNotificationsDisabled_doesNotSetEmailFlag() {
		String content = "Alarm condition detected";
		Alarm existingFault = createTestAlarm();
		existingFault.setEmailed(DONT_EMAIL);
		existingFault.setMessage("");
		Device device = new Device();
		device.setNotificationsDisabled(true);

		when(mockRepository.findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID)).thenReturn(List.of(existingFault));
		when(mockDeviceComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.of(device));
		when(mockRepository.update(any(Alarm.class))).thenAnswer(invocation -> Optional.of(invocation.getArgument(0)));

		Optional<Alarm> result = alarmComponent.alarmConditionDetected(CUSTOMER_ID, DEVICE_ID, SITE_ID, content);

		assertTrue(result.isPresent());
		verify(mockRepository)
				.update(argThat(alarm ->
						alarm.getEmailed() == DONT_EMAIL && alarm.getMessage().equals(content)));
	}

	@Test
	void testGetNewAlarm_withNotificationsDisabled_doesNotSetEmailFlag() {
		String content = "Test alarm content";
		Device device = new Device();
		device.setNotificationsDisabled(true);
		when(mockDeviceComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.of(device));

		Alarm result = alarmComponent.getNewAlarm(CUSTOMER_ID, DEVICE_ID, SITE_ID, content);

		assertNotNull(result);
		assertEquals(CUSTOMER_ID, result.getCustomerId());
		assertEquals(DEVICE_ID, result.getDeviceId());
		assertEquals(SITE_ID, result.getSiteId());
		assertEquals(content, result.getMessage());
		assertEquals(ACTIVE, result.getState());
		assertNotEquals(NEEDS_EMAIL, result.getEmailed());
	}

	@Test
	void testFaultDetected_withDeviceNotificationsDisabled_keepsEmailFlagAsDontEmail() {
		String content = "Device fault detected";
		Device device = new Device();
		device.setNotificationsDisabled(true);
		when(mockRepository.findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID)).thenReturn(Collections.emptyList());
		when(mockDeviceComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.of(device));
		when(mockRepository.update(any(Alarm.class))).thenAnswer(invocation -> Optional.of(invocation.getArgument(0)));

		Optional<Alarm> result = alarmComponent.faultDetected(CUSTOMER_ID, DEVICE_ID, SITE_ID, content);

		assertTrue(result.isPresent());
		verify(mockRepository).update(argThat(alarm -> alarm.getEmailed() == DONT_EMAIL && alarm.getState() == ACTIVE));
	}

	@Test
	void testResolveActiveAlarms_withActiveAlarmButLowPower_doesNotResolve() {
		DeviceData deviceData = createValidDeviceData();
		deviceData.setTotalRealPower(0.05f);
		Alarm activeAlarm = createTestAlarm();
		activeAlarm.setMessage("Low power error");
		when(mockRepository.findMostRecentAlarm(DEVICE_ID)).thenReturn(Optional.of(activeAlarm));

		alarmComponent.resolveActiveAlarms(deviceData);

		verify(mockDeviceUpdateComponent).update(DEVICE_ID);
		verify(mockRepository, never()).update(any(Alarm.class));
	}

	@Test
	void testResolveActiveAlarms_withValidDataAndActiveAlarm_resolvesAlarm() {
		DeviceData deviceData = createValidDeviceData();
		Alarm activeAlarm = createTestAlarm();
		activeAlarm.setEmailed(System.currentTimeMillis());
		when(mockRepository.findMostRecentAlarm(DEVICE_ID)).thenReturn(Optional.of(activeAlarm));
		when(mockRepository.update(any(Alarm.class))).thenReturn(Optional.of(activeAlarm));

		alarmComponent.resolveActiveAlarms(deviceData);

		verify(mockDeviceUpdateComponent).update(DEVICE_ID);
		verify(mockRepository)
				.update(argThat(alarm -> alarm.getState() == RESOLVED
						&& alarm.getResolveEmailed() == NEEDS_EMAIL
						&& alarm.getEndDate() > 0));
	}

	@Test
	void testUpdateAlarm_withEmptyCustomerId_doesNotUpdate() {
		Alarm alarm = createTestAlarm();
		alarm.setCustomerId("");

		Optional<Alarm> result = alarmComponent.updateAlarm(alarm);

		assertFalse(result.isPresent());
		verify(mockRepository, never()).update(any(Alarm.class));
	}

	@Test
	void testUpdateAlarm_withEmptyAlarmId_doesNotUpdate() {
		Alarm alarm = createTestAlarm();
		alarm.setAlarmId("");

		Optional<Alarm> result = alarmComponent.updateAlarm(alarm);

		assertFalse(result.isPresent());
		verify(mockRepository, never()).update(any(Alarm.class));
	}

	@Test
	void testAlarmConditionDetected_withExistingAlarmWithMessage_doesNotOverwriteMessage() {
		String newContent = "New alarm content";
		Alarm existingAlarm = createTestAlarm();
		existingAlarm.setMessage("Existing message");
		existingAlarm.setEmailed(NEEDS_EMAIL);

		when(mockRepository.findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID)).thenReturn(List.of(existingAlarm));
		when(mockRepository.update(any(Alarm.class))).thenAnswer(invocation -> Optional.of(invocation.getArgument(0)));

		Optional<Alarm> result = alarmComponent.alarmConditionDetected(CUSTOMER_ID, DEVICE_ID, SITE_ID, newContent);

		assertTrue(result.isPresent());
		verify(mockRepository)
				.update(argThat(alarm -> alarm.getMessage().equals("Existing message") && alarm.getLastUpdate() > 0));
	}

	@Test
	void testFilterAlarms_withNullCustomerId_returnsEmptyList() {
		List<Alarm> result = alarmComponent.filterAlarms(null, SITE_ID, DEVICE_ID);

		assertTrue(result.isEmpty());
		verify(mockRepository, never()).findAlarmsByDevice(anyString(), anyString());
	}

	@Test
	void testResolveActiveAlarms_notDaylightButIsNoDataAlarm_resolvesAlarm() {
		DeviceData deviceData = createValidDeviceData();
		deviceData.setDaylight(false);
		Alarm activeAlarm = createTestAlarm();
		activeAlarm.setMessage(NO_DATA_RECENTLY + "12345");
		activeAlarm.setEmailed(System.currentTimeMillis());
		when(mockRepository.findMostRecentAlarm(DEVICE_ID)).thenReturn(Optional.of(activeAlarm));
		when(mockRepository.update(any(Alarm.class))).thenReturn(Optional.of(activeAlarm));

		alarmComponent.resolveActiveAlarms(deviceData);

		verify(mockDeviceUpdateComponent).update(DEVICE_ID);
		verify(mockRepository)
				.update(argThat(alarm -> alarm.getState() == RESOLVED
						&& alarm.getResolveEmailed() == NEEDS_EMAIL
						&& alarm.getEndDate() > 0));
	}

	@Test
	void testResolveActiveAlarms_noPowerButIsNoDataAlarm_resolvesAlarm() {
		DeviceData deviceData = createValidDeviceData();
		deviceData.setTotalRealPower(0.0f);
		Alarm activeAlarm = createTestAlarm();
		activeAlarm.setMessage(NO_DATA_RECENTLY + "12345");
		activeAlarm.setEmailed(System.currentTimeMillis());
		when(mockRepository.findMostRecentAlarm(DEVICE_ID)).thenReturn(Optional.of(activeAlarm));
		when(mockRepository.update(any(Alarm.class))).thenReturn(Optional.of(activeAlarm));

		alarmComponent.resolveActiveAlarms(deviceData);

		verify(mockDeviceUpdateComponent).update(DEVICE_ID);
		verify(mockRepository)
				.update(argThat(alarm -> alarm.getState() == RESOLVED
						&& alarm.getResolveEmailed() == NEEDS_EMAIL
						&& alarm.getEndDate() > 0));
	}

	@Test
	void testResolveActiveAlarms_withLinkedDeviceError_doesNotResolve() {
		DeviceData deviceData = createValidDeviceData();
		Alarm activeAlarm = createTestAlarm();
		activeAlarm.setMessage("Device error");
		LinkedDevice erroredLinkedDevice = new LinkedDevice();
		erroredLinkedDevice.setCriticalAlarm(1);

		alarmComponent.setLinkedDeviceErrorResult(Optional.of(erroredLinkedDevice));
		when(mockRepository.findMostRecentAlarm(DEVICE_ID)).thenReturn(Optional.of(activeAlarm));

		alarmComponent.resolveActiveAlarms(deviceData);

		verify(mockDeviceUpdateComponent).update(DEVICE_ID);
		verify(mockRepository, never()).update(any(Alarm.class));
	}

	@Test
	void testResolveActiveAlarms_withLinkedDeviceErrorButNoDataAlarm_resolvesAlarm() {
		DeviceData deviceData = createValidDeviceData();
		Alarm activeAlarm = createTestAlarm();
		activeAlarm.setMessage(NO_DATA_RECENTLY + "12345");
		activeAlarm.setEmailed(System.currentTimeMillis());
		LinkedDevice erroredLinkedDevice = new LinkedDevice();
		erroredLinkedDevice.setCriticalAlarm(1);

		alarmComponent.setLinkedDeviceErrorResult(Optional.of(erroredLinkedDevice));
		when(mockRepository.findMostRecentAlarm(DEVICE_ID)).thenReturn(Optional.of(activeAlarm));
		when(mockRepository.update(any(Alarm.class))).thenReturn(Optional.of(activeAlarm));

		alarmComponent.resolveActiveAlarms(deviceData);

		verify(mockDeviceUpdateComponent).update(DEVICE_ID);
		verify(mockRepository)
				.update(argThat(alarm -> alarm.getState() == RESOLVED
						&& alarm.getResolveEmailed() == NEEDS_EMAIL
						&& alarm.getEndDate() > 0));
	}

	@Test
	void testGetNewAlarm_withDeviceNotFound_setsEmailFlag() {
		String content = "Test alarm content";
		when(mockDeviceComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.empty());

		Alarm result = alarmComponent.getNewAlarm(CUSTOMER_ID, DEVICE_ID, SITE_ID, content);

		assertNotNull(result);
		assertEquals(NEEDS_EMAIL, result.getEmailed());
	}

	@Test
	void testAlarmConditionDetected_withExistingAlarmWithEmptyMessage_setsMessage() {
		String newContent = "New alarm content";
		Alarm existingAlarm = createTestAlarm();
		existingAlarm.setMessage("");
		existingAlarm.setEmailed(NEEDS_EMAIL);

		when(mockRepository.findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID)).thenReturn(List.of(existingAlarm));
		when(mockRepository.update(any(Alarm.class))).thenAnswer(invocation -> Optional.of(invocation.getArgument(0)));

		Optional<Alarm> result = alarmComponent.alarmConditionDetected(CUSTOMER_ID, DEVICE_ID, SITE_ID, newContent);

		assertTrue(result.isPresent());
		verify(mockRepository)
				.update(argThat(alarm -> alarm.getMessage().equals(newContent) && alarm.getLastUpdate() > 0));
	}

	@Test
	void testFaultDetected_withResolvedExistingAlarm_createsNewFault() {
		String content = "Device fault detected";
		Alarm resolvedAlarm = createTestAlarm();
		resolvedAlarm.setState(RESOLVED);
		when(mockRepository.findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID)).thenReturn(List.of(resolvedAlarm));
		when(mockRepository.update(any(Alarm.class))).thenAnswer(invocation -> Optional.of(invocation.getArgument(0)));
		when(mockDeviceComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.empty());

		Optional<Alarm> result = alarmComponent.faultDetected(CUSTOMER_ID, DEVICE_ID, SITE_ID, content);

		assertTrue(result.isPresent());
		verify(mockRepository)
				.update(argThat(alarm -> alarm.getEmailed() == DONT_EMAIL
						&& alarm.getState() == ACTIVE
						&& !alarm.getAlarmId().equals(ALARM_ID)));
	}

	@Test
	void testAlarmConditionDetected_withResolvedExistingAlarm_createsNewAlarm() {
		String content = "Alarm condition detected";
		Alarm resolvedAlarm = createTestAlarm();
		resolvedAlarm.setState(RESOLVED);
		when(mockRepository.findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID)).thenReturn(List.of(resolvedAlarm));
		when(mockRepository.update(any(Alarm.class))).thenAnswer(invocation -> Optional.of(invocation.getArgument(0)));

		Optional<Alarm> result = alarmComponent.alarmConditionDetected(CUSTOMER_ID, DEVICE_ID, SITE_ID, content);

		assertTrue(result.isPresent());
		verify(mockRepository)
				.update(argThat(alarm -> alarm.getState() == ACTIVE
						&& alarm.getMessage().equals(content)
						&& !alarm.getAlarmId().equals(ALARM_ID)));
	}

	@Test
	void testFilterAlarms_withBlankSiteId_andBlankDeviceId_returnsAllCustomerAlarms() {
		List<Alarm> expectedAlarms = List.of(createTestAlarm());
		when(mockRepository.findAlarms(CUSTOMER_ID)).thenReturn(expectedAlarms);

		List<Alarm> result = alarmComponent.filterAlarms(CUSTOMER_ID, null, null);

		assertEquals(expectedAlarms, result);
		verify(mockRepository).findAlarms(CUSTOMER_ID);
	}

	@Test
	void testGetMostRecentAlarm_returnsEmpty() {
		when(mockRepository.findMostRecentAlarm(DEVICE_ID)).thenReturn(Optional.empty());

		Optional<Alarm> result = alarmComponent.getMostRecentAlarm(DEVICE_ID);

		assertFalse(result.isPresent());
	}

	@Test
	void testCleanupOldAlarms_deletesOnlyOldAlarms() {
		Alarm recentResolvedAlarm = createTestAlarm();
		recentResolvedAlarm.setState(RESOLVED);
		recentResolvedAlarm.setStartDate(System.currentTimeMillis());

		when(mockRepository.findAlarmsByStateAndDateLessThan(eq(RESOLVED), anyLong()))
				.thenReturn(Collections.emptyList());
		when(mockRepository.findAlarmsByStateAndDateLessThan(eq(ACTIVE), anyLong()))
				.thenReturn(Collections.emptyList());

		alarmComponent.cleanupOldAlarms();

		verify(mockRepository).findAlarmsByStateAndDateLessThan(eq(RESOLVED), anyLong());
		verify(mockRepository).findAlarmsByStateAndDateLessThan(eq(ACTIVE), anyLong());
		verify(mockRepository, never()).delete(any(Alarm.class));
	}

	@Test
	void testResolveActiveAlarms_withBothNoPowerAndNotDaylight_doesNotResolveNonNoDataAlarm() {
		DeviceData deviceData = createValidDeviceData();
		deviceData.setDaylight(false);
		deviceData.setTotalRealPower(0.0f);
		Alarm activeAlarm = createTestAlarm();
		activeAlarm.setMessage("Some other error");
		when(mockRepository.findMostRecentAlarm(DEVICE_ID)).thenReturn(Optional.of(activeAlarm));

		alarmComponent.resolveActiveAlarms(deviceData);

		verify(mockDeviceUpdateComponent).update(DEVICE_ID);
		verify(mockRepository, never()).update(any(Alarm.class));
	}

	@Test
	void testUpdateAlarm_withValidAlarmButRepositoryReturnsEmpty_returnsEmpty() {
		Alarm alarm = createTestAlarm();
		when(mockRepository.update(alarm)).thenReturn(Optional.empty());

		Optional<Alarm> result = alarmComponent.updateAlarm(alarm);

		assertFalse(result.isPresent());
		verify(mockRepository).update(alarm);
	}

	@Test
	void testFindAlarmsByDevice_withNoAlarms_returnsEmptyList() {
		when(mockRepository.findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID)).thenReturn(Collections.emptyList());

		List<Alarm> result = alarmComponent.findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID);

		assertTrue(result.isEmpty());
		verify(mockRepository).findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID);
	}

	@Test
	void testFindAlarmsBySite_withNoAlarms_returnsEmptyList() {
		when(mockRepository.findAlarmsBySite(CUSTOMER_ID, SITE_ID)).thenReturn(Collections.emptyList());

		List<Alarm> result = alarmComponent.findAlarmsBySite(CUSTOMER_ID, SITE_ID);

		assertTrue(result.isEmpty());
		verify(mockRepository).findAlarmsBySite(CUSTOMER_ID, SITE_ID);
	}

	@Test
	void testGetAlarms_withNoAlarms_returnsEmptyList() {
		when(mockRepository.findAlarms(CUSTOMER_ID)).thenReturn(Collections.emptyList());

		List<Alarm> result = alarmComponent.getAlarms(CUSTOMER_ID);

		assertTrue(result.isEmpty());
		verify(mockRepository).findAlarms(CUSTOMER_ID);
	}

	@Test
	void testFindNonEmailedAlarms_withNoAlarms_returnsEmptyList() {
		when(mockRepository.findNonEmailedAlarms(CUSTOMER_ID)).thenReturn(Collections.emptyList());

		List<Alarm> result = alarmComponent.findNonEmailedAlarms(CUSTOMER_ID);

		assertTrue(result.isEmpty());
		verify(mockRepository).findNonEmailedAlarms(CUSTOMER_ID);
	}

	@Test
	void testFindNonEmailedActiveAlarms_withNoAlarms_returnsEmptyList() {
		when(mockRepository.findNonEmailedActiveAlarms()).thenReturn(Collections.emptyList());

		List<Alarm> result = alarmComponent.findNonEmailedActiveAlarms();

		assertTrue(result.isEmpty());
		verify(mockRepository).findNonEmailedActiveAlarms();
	}

	@Test
	void testFindNonEmailedResolvedAlarms_withNoAlarms_returnsEmptyList() {
		when(mockRepository.findNonEmailedResolvedAlarms()).thenReturn(Collections.emptyList());

		List<Alarm> result = alarmComponent.findNonEmailedResolvedAlarms();

		assertTrue(result.isEmpty());
		verify(mockRepository).findNonEmailedResolvedAlarms();
	}

	@Test
	void testGetActiveAlarms_withNoAlarms_returnsEmptyList() {
		when(mockRepository.findActiveAlarms()).thenReturn(Collections.emptyList());

		List<Alarm> result = alarmComponent.getActiveAlarms();

		assertTrue(result.isEmpty());
		verify(mockRepository).findActiveAlarms();
	}

	@Test
	void testClearDisabledResolvedAlarms_resolvesAlarmsForDisabledDevices() {
		Alarm activeAlarm = createTestAlarm();
		activeAlarm.setEmailed(System.currentTimeMillis());
		Device disabledDevice = new Device();
		disabledDevice.setId(DEVICE_ID);
		disabledDevice.setSiteId(SITE_ID);
		disabledDevice.setDisabled(true);

		when(mockRepository.findActiveAlarms()).thenReturn(List.of(activeAlarm));
		when(mockDeviceComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.of(disabledDevice));
		when(mockRepository.update(any(Alarm.class))).thenReturn(Optional.of(activeAlarm));

		alarmComponent.clearDisabledResolvedAlarms();

		verify(mockRepository).findActiveAlarms();
		verify(mockDeviceComponent).findDeviceById(DEVICE_ID, CUSTOMER_ID);
		verify(mockRepository)
				.update(argThat(alarm -> alarm.getState() == RESOLVED
						&& alarm.getEmailed() == RESOLVED_NOT_EMAILED
						&& alarm.getEndDate() > 0));
	}

	@Test
	void testClearDisabledResolvedAlarms_skipsEnabledDevices() {
		Alarm activeAlarm = createTestAlarm();
		Device enabledDevice = new Device();
		enabledDevice.setId(DEVICE_ID);
		enabledDevice.setDisabled(false);

		when(mockRepository.findActiveAlarms()).thenReturn(List.of(activeAlarm));
		when(mockDeviceComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.of(enabledDevice));

		alarmComponent.clearDisabledResolvedAlarms();

		verify(mockRepository).findActiveAlarms();
		verify(mockRepository, never()).update(any(Alarm.class));
	}

	@Test
	void testIsLinkedDeviceErrored_withDeviceNotFound_returnsEmpty() {
		DeviceData deviceData = createValidDeviceData();
		alarmComponent.setUseRealLinkedDeviceErrored(true);

		when(mockDeviceComponent.findDeviceById(DEVICE_ID, CUSTOMER_ID)).thenReturn(Optional.empty());

		Optional<LinkedDevice> result = alarmComponent.isLinkedDeviceErrored(deviceData, null);

		assertFalse(result.isPresent());
		verify(mockDeviceComponent).findDeviceById(DEVICE_ID, CUSTOMER_ID);
	}

	@Test
	void testIsLinkedDeviceErrored_withNoSerialNumber_returnsEmpty() {
		DeviceData deviceData = createValidDeviceData();
		Device device = new Device();
		device.setSerialNumber("");
		alarmComponent.setUseRealLinkedDeviceErrored(true);

		Optional<LinkedDevice> result = alarmComponent.isLinkedDeviceErrored(deviceData, device);

		assertFalse(result.isPresent());
		verify(mockLinkedDeviceComponent, never()).queryBySerialNumber(anyString(), anyString());
	}

	@Test
	void testIsLinkedDeviceErrored_withNoLinkedDevice_returnsEmpty() {
		DeviceData deviceData = createValidDeviceData();
		Device device = new Device();
		device.setSerialNumber("serial-123");
		alarmComponent.setUseRealLinkedDeviceErrored(true);

		when(mockLinkedDeviceComponent.queryBySerialNumber("serial-123", CUSTOMER_ID))
				.thenReturn(Optional.empty());

		Optional<LinkedDevice> result = alarmComponent.isLinkedDeviceErrored(deviceData, device);

		assertFalse(result.isPresent());
		verify(mockLinkedDeviceComponent).queryBySerialNumber("serial-123", CUSTOMER_ID);
	}

	@Test
	void testIsLinkedDeviceErrored_withNominalCriticalAlarm_returnsEmpty() {
		DeviceData deviceData = createValidDeviceData();
		Device device = new Device();
		device.setSerialNumber("serial-123");
		LinkedDevice linkedDevice = new LinkedDevice();
		linkedDevice.setCriticalAlarm(ISolectriaConstants.NOMINAL);
		linkedDevice.setInformativeAlarm(ISolectriaConstants.NOMINAL);
		alarmComponent.setUseRealLinkedDeviceErrored(true);

		when(mockLinkedDeviceComponent.queryBySerialNumber("serial-123", CUSTOMER_ID))
				.thenReturn(Optional.of(linkedDevice));

		Optional<LinkedDevice> result = alarmComponent.isLinkedDeviceErrored(deviceData, device);

		assertFalse(result.isPresent());
	}

	@Test
	void testIsLinkedDeviceErrored_withCriticalAlarm_returnsLinkedDevice() {
		DeviceData deviceData = createValidDeviceData();
		Device device = new Device();
		device.setSerialNumber("serial-123");
		LinkedDevice linkedDevice = new LinkedDevice();
		linkedDevice.setCriticalAlarm(5);
		linkedDevice.setInformativeAlarm(0);
		alarmComponent.setUseRealLinkedDeviceErrored(true);

		when(mockLinkedDeviceComponent.queryBySerialNumber("serial-123", CUSTOMER_ID))
				.thenReturn(Optional.of(linkedDevice));

		Optional<LinkedDevice> result = alarmComponent.isLinkedDeviceErrored(deviceData, device);

		assertTrue(result.isPresent());
		assertEquals(linkedDevice, result.get());
	}

	@Test
	void testCheckDevice_withNullDevice_returnsEmpty() {
		Optional<Alarm> result = alarmComponent.checkDevice(null);

		assertFalse(result.isPresent());
	}

	@Test
	void testCheckDevice_withDisabledDevice_returnsEmpty() {
		Device device = new Device();
		device.setId(DEVICE_ID);
		device.setClientId(CUSTOMER_ID);
		device.setSiteId(SITE_ID);
		device.setDisabled(true);

		when(mockOpenSearchComponent.getLastDeviceEntry(eq(DEVICE_ID), any())).thenReturn(new DeviceData());

		Optional<Alarm> result = alarmComponent.checkDevice(device);

		assertFalse(result.isPresent());
	}

	@Test
	void testCheckDevice_withNoDeviceData_returnsEmpty() {
		Device device = new Device();
		device.setId(DEVICE_ID);
		device.setClientId(CUSTOMER_ID);
		device.setSiteId(SITE_ID);
		device.setDisabled(false);

		when(mockOpenSearchComponent.getLastDeviceEntry(eq(DEVICE_ID), any())).thenReturn(null);

		Optional<Alarm> result = alarmComponent.checkDevice(device);

		assertFalse(result.isPresent());
	}

	@Test
	void testSendPendingNotifications_withNoAlarmsToSend_doesNothing() {
		when(mockRepository.findNonEmailedActiveAlarms()).thenReturn(Collections.emptyList());
		when(mockRepository.findNonEmailedResolvedAlarms()).thenReturn(Collections.emptyList());

		alarmComponent.sendPendingNotifications();

		verify(mockNotificationComponent, never()).sendNotification(anyString(), anyString(), any());
	}

	@Test
	void testSendPendingNotifications_withNoAlarms_completesSuccessfully() {
		when(mockRepository.findNonEmailedActiveAlarms()).thenReturn(Collections.emptyList());
		when(mockRepository.findNonEmailedResolvedAlarms()).thenReturn(Collections.emptyList());

		alarmComponent.sendPendingNotifications();

		verify(mockNotificationComponent, never()).sendNotification(anyString(), anyString(), any());
		verify(mockRepository, never()).update(any(Alarm.class));
	}

	@Test
	void testSendPendingNotifications_callsFindNonEmailedAlarms() {
		when(mockRepository.findNonEmailedActiveAlarms()).thenReturn(Collections.emptyList());
		when(mockRepository.findNonEmailedResolvedAlarms()).thenReturn(Collections.emptyList());

		alarmComponent.sendPendingNotifications();

		verify(mockRepository).findNonEmailedActiveAlarms();
		verify(mockRepository).findNonEmailedResolvedAlarms();
	}

	@Test
	void testSendPendingNotifications_doesNotThrowException() {
		when(mockRepository.findNonEmailedActiveAlarms()).thenReturn(Collections.emptyList());
		when(mockRepository.findNonEmailedResolvedAlarms()).thenReturn(Collections.emptyList());

		assertDoesNotThrow(() -> alarmComponent.sendPendingNotifications());
	}

	@Test
	void testCheckDevice_withStaleDataAndHealthyOpenSearch_createsNoDataAlarm() {
		Device device = new Device();
		device.setId(DEVICE_ID);
		device.setClientId(CUSTOMER_ID);
		device.setSiteId(SITE_ID);
		device.setDisabled(false);

		DeviceData staleData = createValidDeviceData();
		staleData.setDate(new Date(System.currentTimeMillis() - TimeConstants.HOUR * 2));

		when(mockOpenSearchComponent.getLastDeviceEntry(eq(DEVICE_ID), any())).thenReturn(staleData);
		when(mockOpenSearchStatusComponent.hasFailureWithinLastThirtyMinutes()).thenReturn(false);
		when(mockRepository.findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID)).thenReturn(Collections.emptyList());
		when(mockRepository.update(any(Alarm.class))).thenAnswer(invocation -> Optional.of(invocation.getArgument(0)));

		Optional<Alarm> result = alarmComponent.checkDevice(device);

		assertTrue(result.isPresent());
		verify(mockRepository).update(argThat(alarm -> alarm.getMessage().contains(NO_DATA_RECENTLY)));
	}

	@Test
	void testCheckDevice_withStaleDataButUnhealthyOpenSearch_returnsEmpty() {
		Device device = new Device();
		device.setId(DEVICE_ID);
		device.setClientId(CUSTOMER_ID);
		device.setSiteId(SITE_ID);
		device.setDisabled(false);

		DeviceData staleData = createValidDeviceData();
		staleData.setDate(new Date(System.currentTimeMillis() - TimeConstants.HOUR * 2));

		when(mockOpenSearchComponent.getLastDeviceEntry(eq(DEVICE_ID), any())).thenReturn(staleData);
		when(mockOpenSearchStatusComponent.hasFailureWithinLastThirtyMinutes()).thenReturn(true);

		Optional<Alarm> result = alarmComponent.checkDevice(device);

		assertFalse(result.isPresent());
		verify(mockRepository, never()).update(any(Alarm.class));
	}

	@Test
	void testQuickCheckDevices_withNoDeviceUpdates_returnsEmptyList() {
		when(mockDeviceUpdateComponent.queryByTimeRange(anyLong())).thenReturn(Collections.emptyList());

		List<Alarm> result = alarmComponent.quickCheckDevices();

		assertTrue(result.isEmpty());
	}

	@Test
	void testQuickCheckDevices_withDisabledDevice_skipsDevice() {
		com.bigboxer23.solar_moon.data.DeviceUpdateData updateData =
				new com.bigboxer23.solar_moon.data.DeviceUpdateData();
		updateData.setDeviceId(DEVICE_ID);
		updateData.setLastUpdate(System.currentTimeMillis());

		Device device = new Device();
		device.setId(DEVICE_ID);
		device.setClientId(CUSTOMER_ID);
		device.setSiteId(SITE_ID);
		device.setDisabled(true);

		when(mockDeviceUpdateComponent.queryByTimeRange(anyLong())).thenReturn(List.of(updateData));
		when(mockDeviceComponent.findDeviceById(DEVICE_ID)).thenReturn(Optional.of(device));

		List<Alarm> result = alarmComponent.quickCheckDevices();

		assertTrue(result.isEmpty());
		verify(mockRepository, never()).findAlarmsByDevice(anyString(), anyString());
	}

	@Test
	void testQuickCheckDevices_withDeviceNotFound_skipsDevice() {
		com.bigboxer23.solar_moon.data.DeviceUpdateData updateData =
				new com.bigboxer23.solar_moon.data.DeviceUpdateData();
		updateData.setDeviceId(DEVICE_ID);
		updateData.setLastUpdate(System.currentTimeMillis());

		when(mockDeviceUpdateComponent.queryByTimeRange(anyLong())).thenReturn(List.of(updateData));
		when(mockDeviceComponent.findDeviceById(DEVICE_ID)).thenReturn(Optional.empty());

		List<Alarm> result = alarmComponent.quickCheckDevices();

		assertTrue(result.isEmpty());
	}

	@Test
	void testQuickCheckDevices_atNight_skipsDevice() {
		com.bigboxer23.solar_moon.data.DeviceUpdateData updateData =
				new com.bigboxer23.solar_moon.data.DeviceUpdateData();
		updateData.setDeviceId(DEVICE_ID);
		updateData.setLastUpdate(System.currentTimeMillis());

		Device device = new Device();
		device.setId(DEVICE_ID);
		device.setClientId(CUSTOMER_ID);
		device.setSiteId(SITE_ID);
		device.setDisabled(false);

		Device siteDevice = new Device();
		siteDevice.setId(SITE_ID);
		siteDevice.setClientId(CUSTOMER_ID);
		siteDevice.setLatitude(40.0);
		siteDevice.setLongitude(-75.0);

		when(mockDeviceUpdateComponent.queryByTimeRange(anyLong())).thenReturn(List.of(updateData));
		when(mockDeviceComponent.findDeviceById(DEVICE_ID)).thenReturn(Optional.of(device));
		when(mockDeviceComponent.findDeviceById(SITE_ID, CUSTOMER_ID)).thenReturn(Optional.of(siteDevice));
		when(mockLocationComponent.isDay(any(Date.class), anyDouble(), anyDouble()))
				.thenReturn(Optional.of(false));

		List<Alarm> result = alarmComponent.quickCheckDevices();

		assertTrue(result.isEmpty());
	}

	@Test
	void testQuickCheckDevices_duringDayWithNoUpdates_createsAlarm() {
		com.bigboxer23.solar_moon.data.DeviceUpdateData updateData =
				new com.bigboxer23.solar_moon.data.DeviceUpdateData();
		updateData.setDeviceId(DEVICE_ID);
		updateData.setLastUpdate(System.currentTimeMillis());

		Device device = new Device();
		device.setId(DEVICE_ID);
		device.setClientId(CUSTOMER_ID);
		device.setSiteId(SITE_ID);
		device.setDisabled(false);

		Device siteDevice = new Device();
		siteDevice.setId(SITE_ID);
		siteDevice.setClientId(CUSTOMER_ID);
		siteDevice.setLatitude(40.0);
		siteDevice.setLongitude(-75.0);

		Alarm alarm = createTestAlarm();

		when(mockDeviceUpdateComponent.queryByTimeRange(anyLong())).thenReturn(List.of(updateData));
		when(mockDeviceComponent.findDeviceById(DEVICE_ID)).thenReturn(Optional.of(device));
		when(mockDeviceComponent.findDeviceById(SITE_ID, CUSTOMER_ID)).thenReturn(Optional.of(siteDevice));
		when(mockLocationComponent.isDay(any(Date.class), anyDouble(), anyDouble()))
				.thenReturn(Optional.of(true));
		when(mockRepository.findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID)).thenReturn(Collections.emptyList());
		when(mockRepository.update(any(Alarm.class))).thenReturn(Optional.of(alarm));

		List<Alarm> result = alarmComponent.quickCheckDevices();

		assertFalse(result.isEmpty());
		verify(mockRepository).update(argThat(a -> a.getMessage().contains(NO_DATA_RECENTLY)));
	}

	@Test
	void testIsDeviceOK_withNotDaylight_returnsTrue() {
		Device device = new Device();
		DeviceData deviceData = createValidDeviceData();
		deviceData.setDaylight(false);

		boolean result = alarmComponent.isDeviceOK(device, deviceData, true);

		assertTrue(result);
	}

	@Test
	void testIsDeviceOK_withGoodPower_returnsTrue() {
		Device device = new Device();
		DeviceData deviceData = createValidDeviceData();
		deviceData.setTotalRealPower(5.0f);

		boolean result = alarmComponent.isDeviceOK(device, deviceData, true);

		assertTrue(result);
	}

	@Test
	void testIsDeviceOK_withUnhealthyOpenSearch_returnsTrue() {
		Device device = new Device();
		DeviceData deviceData = createValidDeviceData();
		deviceData.setTotalRealPower(0.0f);

		boolean result = alarmComponent.isDeviceOK(device, deviceData, false);

		assertTrue(result);
	}

	@Test
	void testIsDeviceOK_withSiteDevice_returnsTrue() {
		Device device = new Device();
		device.setIsSite("1");
		DeviceData deviceData = createValidDeviceData();
		deviceData.setTotalRealPower(0.0f);

		boolean result = alarmComponent.isDeviceOK(device, deviceData, true);

		assertTrue(result);
	}

	@Test
	void testResolveActiveAlarms_withOldData_doesNotResolve() {
		DeviceData deviceData = createValidDeviceData();
		deviceData.setDate(new Date(System.currentTimeMillis() - TimeConstants.HOUR * 2));

		alarmComponent.resolveActiveAlarms(deviceData);

		verify(mockDeviceUpdateComponent, never()).update(anyString());
		verify(mockRepository, never()).findMostRecentAlarm(anyString());
	}

	@Test
	void testResolveActiveAlarms_withNullDate_doesNotResolve() {
		DeviceData deviceData = createValidDeviceData();
		deviceData.setDate(null);

		alarmComponent.resolveActiveAlarms(deviceData);

		verify(mockDeviceUpdateComponent, never()).update(anyString());
		verify(mockRepository, never()).findMostRecentAlarm(anyString());
	}

	@Test
	void testResolveActiveAlarms_withNoActiveAlarm_updatesDeviceButDoesNotResolve() {
		DeviceData deviceData = createValidDeviceData();
		when(mockRepository.findMostRecentAlarm(DEVICE_ID)).thenReturn(Optional.empty());

		alarmComponent.resolveActiveAlarms(deviceData);

		verify(mockDeviceUpdateComponent).update(DEVICE_ID);
		verify(mockRepository, never()).update(any(Alarm.class));
	}

	@Test
	void testResolveActiveAlarms_withResolvedAlarm_doesNotResolveAgain() {
		DeviceData deviceData = createValidDeviceData();
		Alarm resolvedAlarm = createTestAlarm();
		resolvedAlarm.setState(RESOLVED);
		when(mockRepository.findMostRecentAlarm(DEVICE_ID)).thenReturn(Optional.of(resolvedAlarm));

		alarmComponent.resolveActiveAlarms(deviceData);

		verify(mockDeviceUpdateComponent).update(DEVICE_ID);
		verify(mockRepository, never()).update(any(Alarm.class));
	}

	@Test
	void testFaultDetected_withExistingActiveFault_updatesExistingAlarm() {
		String content = "Device fault detected";
		Alarm existingFault = createTestAlarm();
		existingFault.setMessage(content);
		existingFault.setEmailed(DONT_EMAIL);
		when(mockRepository.findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID)).thenReturn(List.of(existingFault));
		when(mockRepository.update(any(Alarm.class))).thenAnswer(invocation -> Optional.of(invocation.getArgument(0)));

		Optional<Alarm> result = alarmComponent.faultDetected(CUSTOMER_ID, DEVICE_ID, SITE_ID, content);

		assertTrue(result.isPresent());
		verify(mockRepository)
				.update(argThat(alarm -> alarm.getAlarmId().equals(ALARM_ID) && alarm.getLastUpdate() > 0));
	}

	@Test
	void testAlarmConditionDetected_withBlankContent_createsAlarmWithBlankMessage() {
		when(mockRepository.findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID)).thenReturn(Collections.emptyList());
		when(mockRepository.update(any(Alarm.class))).thenAnswer(invocation -> Optional.of(invocation.getArgument(0)));

		Optional<Alarm> result = alarmComponent.alarmConditionDetected(CUSTOMER_ID, DEVICE_ID, SITE_ID, "");

		assertTrue(result.isPresent());
		verify(mockRepository).update(argThat(alarm -> alarm.getMessage().isEmpty()));
	}

	@Test
	void testAlarmConditionDetected_withNullContent_createsAlarmWithNullMessage() {
		when(mockRepository.findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID)).thenReturn(Collections.emptyList());
		when(mockRepository.update(any(Alarm.class))).thenAnswer(invocation -> Optional.of(invocation.getArgument(0)));

		Optional<Alarm> result = alarmComponent.alarmConditionDetected(CUSTOMER_ID, DEVICE_ID, SITE_ID, null);

		assertTrue(result.isPresent());
		verify(mockRepository).update(any(Alarm.class));
	}

	@Test
	void testFaultDetected_withBlankContent_createsAlarmWithBlankMessage() {
		when(mockRepository.findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID)).thenReturn(Collections.emptyList());
		when(mockRepository.update(any(Alarm.class))).thenAnswer(invocation -> Optional.of(invocation.getArgument(0)));

		Optional<Alarm> result = alarmComponent.faultDetected(CUSTOMER_ID, DEVICE_ID, SITE_ID, "");

		assertTrue(result.isPresent());
		verify(mockRepository).update(argThat(alarm -> alarm.getMessage().isEmpty()));
	}

	@Test
	void testFilterAlarms_withOnlyDeviceId_returnsDeviceAlarms() {
		List<Alarm> expectedAlarms = List.of(createTestAlarm());
		when(mockRepository.findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID)).thenReturn(expectedAlarms);

		List<Alarm> result = alarmComponent.filterAlarms(CUSTOMER_ID, null, DEVICE_ID);

		assertEquals(expectedAlarms, result);
		verify(mockRepository).findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID);
	}

	@Test
	void testFilterAlarms_withOnlySiteId_returnsSiteAlarms() {
		List<Alarm> expectedAlarms = List.of(createTestAlarm());
		when(mockRepository.findAlarmsBySite(CUSTOMER_ID, SITE_ID)).thenReturn(expectedAlarms);

		List<Alarm> result = alarmComponent.filterAlarms(CUSTOMER_ID, SITE_ID, null);

		assertEquals(expectedAlarms, result);
		verify(mockRepository).findAlarmsBySite(CUSTOMER_ID, SITE_ID);
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
