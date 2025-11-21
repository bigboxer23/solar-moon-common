package com.bigboxer23.solar_moon.alarm;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.data.Alarm;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

@ExtendWith(MockitoExtension.class)
public class DynamoDbAlarmRepositoryTest implements IAlarmConstants {

	@Mock
	private DynamoDbTable<Alarm> mockTable;

	@Mock
	private DynamoDbIndex<Alarm> mockIndex;

	@Mock
	private PageIterable<Alarm> mockPageIterable;

	@Mock
	private Page<Alarm> mockPage;

	private TestableDynamoDbAlarmRepository repository;

	private static final String ALARM_ID = "alarm-123";
	private static final String CUSTOMER_ID = "customer-123";
	private static final String DEVICE_ID = "device-123";
	private static final String SITE_ID = "site-123";

	private static class TestableDynamoDbAlarmRepository extends DynamoDbAlarmRepository {
		private final DynamoDbTable<Alarm> table;

		public TestableDynamoDbAlarmRepository(DynamoDbTable<Alarm> table) {
			this.table = table;
		}

		@Override
		protected DynamoDbTable<Alarm> getTable() {
			return table;
		}
	}

	@BeforeEach
	void setUp() {
		repository = new TestableDynamoDbAlarmRepository(mockTable);
	}

	@Test
	void testFindMostRecentAlarm_withExistingAlarm_returnsAlarm() {
		Alarm expectedAlarm = createTestAlarm();
		when(mockTable.index(Alarm.DEVICEID_STARTDATE_INDEX)).thenReturn(mockIndex);
		when(mockIndex.query(any(Consumer.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(java.util.stream.Stream.of(mockPage));
		when(mockPage.items()).thenReturn(List.of(expectedAlarm));

		Optional<Alarm> result = repository.findMostRecentAlarm(DEVICE_ID);

		assertTrue(result.isPresent());
		assertEquals(expectedAlarm, result.get());
		verify(mockTable).index(Alarm.DEVICEID_STARTDATE_INDEX);
	}

	@Test
	void testFindMostRecentAlarm_withNoAlarms_returnsEmpty() {
		when(mockTable.index(Alarm.DEVICEID_STARTDATE_INDEX)).thenReturn(mockIndex);
		when(mockIndex.query(any(Consumer.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(java.util.stream.Stream.empty());

		Optional<Alarm> result = repository.findMostRecentAlarm(DEVICE_ID);

		assertFalse(result.isPresent());
	}

	@Test
	void testFindMostRecentAlarm_withEmptyItemsList_returnsEmpty() {
		when(mockTable.index(Alarm.DEVICEID_STARTDATE_INDEX)).thenReturn(mockIndex);
		when(mockIndex.query(any(Consumer.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(java.util.stream.Stream.of(mockPage));
		when(mockPage.items()).thenReturn(Collections.emptyList());

		Optional<Alarm> result = repository.findMostRecentAlarm(DEVICE_ID);

		assertFalse(result.isPresent());
	}

	@Test
	void testFindAlarmsByDevice_returnsDeviceAlarms() {
		List<Alarm> expectedAlarms = List.of(createTestAlarm());
		when(mockTable.index(Alarm.DEVICE_CUSTOMER_INDEX)).thenReturn(mockIndex);
		when(mockIndex.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(java.util.stream.Stream.of(mockPage));
		when(mockPage.items()).thenReturn(expectedAlarms);

		List<Alarm> result = repository.findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID);

		assertEquals(1, result.size());
		assertEquals(expectedAlarms.getFirst(), result.getFirst());
		verify(mockTable).index(Alarm.DEVICE_CUSTOMER_INDEX);
	}

	@Test
	void testFindAlarmsByDevice_withNoAlarms_returnsEmptyList() {
		when(mockTable.index(Alarm.DEVICE_CUSTOMER_INDEX)).thenReturn(mockIndex);
		when(mockIndex.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(java.util.stream.Stream.empty());

		List<Alarm> result = repository.findAlarmsByDevice(CUSTOMER_ID, DEVICE_ID);

		assertTrue(result.isEmpty());
	}

	@Test
	void testFindAlarmsBySite_returnsSiteAlarms() {
		List<Alarm> expectedAlarms = List.of(createTestAlarm());
		when(mockTable.index(Alarm.SITE_CUSTOMER_INDEX)).thenReturn(mockIndex);
		when(mockIndex.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(java.util.stream.Stream.of(mockPage));
		when(mockPage.items()).thenReturn(expectedAlarms);

		List<Alarm> result = repository.findAlarmsBySite(CUSTOMER_ID, SITE_ID);

		assertEquals(1, result.size());
		assertEquals(expectedAlarms.getFirst(), result.getFirst());
		verify(mockTable).index(Alarm.SITE_CUSTOMER_INDEX);
	}

	@Test
	void testFindAlarmsBySite_withNoAlarms_returnsEmptyList() {
		when(mockTable.index(Alarm.SITE_CUSTOMER_INDEX)).thenReturn(mockIndex);
		when(mockIndex.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(java.util.stream.Stream.empty());

		List<Alarm> result = repository.findAlarmsBySite(CUSTOMER_ID, SITE_ID);

		assertTrue(result.isEmpty());
	}

	@Test
	void testFindNonEmailedAlarms_returnsAlarmsNeedingEmail() {
		List<Alarm> expectedAlarms = List.of(createTestAlarm());
		when(mockTable.index(Alarm.EMAILED_CUSTOMER_INDEX)).thenReturn(mockIndex);
		when(mockIndex.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(java.util.stream.Stream.of(mockPage));
		when(mockPage.items()).thenReturn(expectedAlarms);

		List<Alarm> result = repository.findNonEmailedAlarms(CUSTOMER_ID);

		assertEquals(1, result.size());
		assertEquals(expectedAlarms.getFirst(), result.getFirst());
		verify(mockTable).index(Alarm.EMAILED_CUSTOMER_INDEX);
	}

	@Test
	void testFindNonEmailedAlarms_withMultiplePages_returnsAllAlarms() {
		Alarm alarm1 = createTestAlarm();
		Alarm alarm2 = createTestAlarm();
		alarm2.setAlarmId("alarm-456");

		Page<Alarm> page1 = mock(Page.class);
		Page<Alarm> page2 = mock(Page.class);
		when(page1.items()).thenReturn(List.of(alarm1));
		when(page2.items()).thenReturn(List.of(alarm2));

		when(mockTable.index(Alarm.EMAILED_CUSTOMER_INDEX)).thenReturn(mockIndex);
		when(mockIndex.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(java.util.stream.Stream.of(page1, page2));

		List<Alarm> result = repository.findNonEmailedAlarms(CUSTOMER_ID);

		assertEquals(2, result.size());
	}

	@Test
	void testFindNonEmailedActiveAlarms_returnsActiveAlarms() {
		List<Alarm> expectedAlarms = List.of(createTestAlarm());
		when(mockTable.index(Alarm.EMAILED_CUSTOMER_INDEX)).thenReturn(mockIndex);
		when(mockIndex.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(java.util.stream.Stream.of(mockPage));
		when(mockPage.items()).thenReturn(expectedAlarms);

		List<Alarm> result = repository.findNonEmailedActiveAlarms();

		assertEquals(1, result.size());
		verify(mockTable).index(Alarm.EMAILED_CUSTOMER_INDEX);
	}

	@Test
	void testFindNonEmailedResolvedAlarms_returnsResolvedAlarms() {
		List<Alarm> expectedAlarms = List.of(createTestAlarm());
		when(mockTable.index(Alarm.RESOLVED_EMAILED_INDEX)).thenReturn(mockIndex);
		when(mockIndex.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(java.util.stream.Stream.of(mockPage));
		when(mockPage.items()).thenReturn(expectedAlarms);

		List<Alarm> result = repository.findNonEmailedResolvedAlarms();

		assertEquals(1, result.size());
		verify(mockTable).index(Alarm.RESOLVED_EMAILED_INDEX);
	}

	@Test
	void testFindAlarms_withValidCustomerId_returnsAlarms() {
		List<Alarm> expectedAlarms = List.of(createTestAlarm());
		when(mockTable.index(Alarm.CUSTOMER_INDEX)).thenReturn(mockIndex);
		when(mockIndex.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(java.util.stream.Stream.of(mockPage));
		when(mockPage.items()).thenReturn(expectedAlarms);

		List<Alarm> result = repository.findAlarms(CUSTOMER_ID);

		assertEquals(1, result.size());
		assertEquals(expectedAlarms.getFirst(), result.getFirst());
		verify(mockTable).index(Alarm.CUSTOMER_INDEX);
	}

	@Test
	void testFindAlarms_withBlankCustomerId_returnsEmptyList() {
		List<Alarm> result = repository.findAlarms("");

		assertTrue(result.isEmpty());
		verify(mockTable, never()).index(anyString());
	}

	@Test
	void testFindAlarms_withNullCustomerId_returnsEmptyList() {
		List<Alarm> result = repository.findAlarms(null);

		assertTrue(result.isEmpty());
		verify(mockTable, never()).index(anyString());
	}

	@Test
	void testFindAlarmByAlarmId_withValidIds_returnsAlarm() {
		Alarm expectedAlarm = createTestAlarm();
		when(mockTable.getItem(any(Alarm.class))).thenReturn(expectedAlarm);

		Optional<Alarm> result = repository.findAlarmByAlarmId(ALARM_ID, CUSTOMER_ID);

		assertTrue(result.isPresent());
		assertEquals(expectedAlarm, result.get());
		verify(mockTable).getItem(any(Alarm.class));
	}

	@Test
	void testFindAlarmByAlarmId_withBlankAlarmId_returnsEmpty() {
		Optional<Alarm> result = repository.findAlarmByAlarmId("", CUSTOMER_ID);

		assertFalse(result.isPresent());
		verify(mockTable, never()).getItem(any(Alarm.class));
	}

	@Test
	void testFindAlarmByAlarmId_withBlankCustomerId_returnsEmpty() {
		Optional<Alarm> result = repository.findAlarmByAlarmId(ALARM_ID, "");

		assertFalse(result.isPresent());
		verify(mockTable, never()).getItem(any(Alarm.class));
	}

	@Test
	void testFindAlarmByAlarmId_withNullAlarmId_returnsEmpty() {
		Optional<Alarm> result = repository.findAlarmByAlarmId(null, CUSTOMER_ID);

		assertFalse(result.isPresent());
		verify(mockTable, never()).getItem(any(Alarm.class));
	}

	@Test
	void testFindAlarmByAlarmId_withNullCustomerId_returnsEmpty() {
		Optional<Alarm> result = repository.findAlarmByAlarmId(ALARM_ID, null);

		assertFalse(result.isPresent());
		verify(mockTable, never()).getItem(any(Alarm.class));
	}

	@Test
	void testFindAlarmByAlarmId_whenNotFound_returnsEmpty() {
		when(mockTable.getItem(any(Alarm.class))).thenReturn(null);

		Optional<Alarm> result = repository.findAlarmByAlarmId(ALARM_ID, CUSTOMER_ID);

		assertFalse(result.isPresent());
	}

	@Test
	void testFindActiveAlarms_returnsActiveAlarms() {
		List<Alarm> expectedAlarms = List.of(createTestAlarm());
		when(mockTable.index(Alarm.STATE_CUSTOMER_INDEX)).thenReturn(mockIndex);
		when(mockIndex.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(java.util.stream.Stream.of(mockPage));
		when(mockPage.items()).thenReturn(expectedAlarms);

		List<Alarm> result = repository.findActiveAlarms();

		assertEquals(1, result.size());
		verify(mockTable).index(Alarm.STATE_CUSTOMER_INDEX);
	}

	@Test
	void testFindActiveAlarms_withNoActiveAlarms_returnsEmptyList() {
		when(mockTable.index(Alarm.STATE_CUSTOMER_INDEX)).thenReturn(mockIndex);
		when(mockIndex.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(java.util.stream.Stream.empty());

		List<Alarm> result = repository.findActiveAlarms();

		assertTrue(result.isEmpty());
	}

	@Test
	void testFindAlarmsByStateAndDateLessThan_returnsMatchingAlarms() {
		List<Alarm> expectedAlarms = List.of(createTestAlarm());
		long timestamp = System.currentTimeMillis();
		when(mockTable.index(Alarm.STATE_STARTDATE_INDEX)).thenReturn(mockIndex);
		when(mockIndex.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(java.util.stream.Stream.of(mockPage));
		when(mockPage.items()).thenReturn(expectedAlarms);

		List<Alarm> result = repository.findAlarmsByStateAndDateLessThan(ACTIVE, timestamp);

		assertEquals(1, result.size());
		verify(mockTable).index(Alarm.STATE_STARTDATE_INDEX);
	}

	@Test
	void testFindAlarmsByStateAndDateLessThan_withNoMatches_returnsEmptyList() {
		long timestamp = System.currentTimeMillis();
		when(mockTable.index(Alarm.STATE_STARTDATE_INDEX)).thenReturn(mockIndex);
		when(mockIndex.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(java.util.stream.Stream.empty());

		List<Alarm> result = repository.findAlarmsByStateAndDateLessThan(RESOLVED, timestamp);

		assertTrue(result.isEmpty());
	}

	@Test
	void testDelete_callsTableDeleteItem() {
		Alarm alarm = createTestAlarm();

		repository.delete(alarm);

		verify(mockTable).deleteItem(alarm);
	}

	@Test
	void testGetTableName_returnsCorrectTableName() {
		assertEquals("alarms", repository.getTableName());
	}

	@Test
	void testGetObjectClass_returnsCorrectClass() {
		assertEquals(Alarm.class, repository.getObjectClass());
	}

	private Alarm createTestAlarm() {
		Alarm alarm = new Alarm(ALARM_ID, CUSTOMER_ID, DEVICE_ID, SITE_ID);
		alarm.setMessage("Test alarm message");
		alarm.setState(ACTIVE);
		alarm.setStartDate(System.currentTimeMillis());
		alarm.setEmailed(NEEDS_EMAIL);
		return alarm;
	}
}
