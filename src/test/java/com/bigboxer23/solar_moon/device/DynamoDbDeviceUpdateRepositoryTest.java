package com.bigboxer23.solar_moon.device;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.data.DeviceUpdateData;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

@ExtendWith(MockitoExtension.class)
public class DynamoDbDeviceUpdateRepositoryTest {

	@Mock
	private DynamoDbTable<DeviceUpdateData> mockTable;

	@Mock
	private DynamoDbIndex<DeviceUpdateData> mockIndex;

	@Mock
	private PageIterable<DeviceUpdateData> mockPageIterable;

	@Mock
	private SdkIterable<DeviceUpdateData> mockSdkIterable;

	@Mock
	private Page<DeviceUpdateData> mockPage;

	private DynamoDbDeviceUpdateRepository repository;

	private static final String DEVICE_ID = "device-123";
	private static final long LAST_UPDATE = System.currentTimeMillis();

	private static class TestableDynamoDbDeviceUpdateRepository extends DynamoDbDeviceUpdateRepository {
		private final DynamoDbTable<DeviceUpdateData> table;

		public TestableDynamoDbDeviceUpdateRepository(DynamoDbTable<DeviceUpdateData> table) {
			this.table = table;
		}

		@Override
		protected DynamoDbTable<DeviceUpdateData> getTable() {
			return table;
		}
	}

	@BeforeEach
	void setUp() {
		repository = new TestableDynamoDbDeviceUpdateRepository(mockTable);
	}

	@Test
	void testUpdate_withValidDeviceUpdate_updatesSuccessfully() {
		DeviceUpdateData deviceUpdate = createDeviceUpdate();
		when(mockTable.updateItem(any(Consumer.class))).thenReturn(deviceUpdate);

		DeviceUpdateData result = repository.update(deviceUpdate);

		assertEquals(deviceUpdate, result);
		verify(mockTable).updateItem(any(Consumer.class));
	}

	@Test
	void testDelete_withValidDeviceId_deletesSuccessfully() {
		DeviceUpdateData deviceUpdate1 = createDeviceUpdate();
		DeviceUpdateData deviceUpdate2 = createDeviceUpdate();
		when(mockTable.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.items()).thenReturn(mockSdkIterable);
		doAnswer(invocation -> {
					Consumer<DeviceUpdateData> action = invocation.getArgument(0);
					action.accept(deviceUpdate1);
					action.accept(deviceUpdate2);
					return null;
				})
				.when(mockSdkIterable)
				.forEach(any(Consumer.class));

		repository.delete(DEVICE_ID);

		verify(mockTable).query(any(QueryConditional.class));
		verify(mockTable, times(2)).deleteItem(any(Consumer.class));
	}

	@Test
	void testDelete_withNoResults_doesNotDelete() {
		when(mockTable.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.items()).thenReturn(mockSdkIterable);
		doAnswer(invocation -> null).when(mockSdkIterable).forEach(any(Consumer.class));

		repository.delete(DEVICE_ID);

		verify(mockTable).query(any(QueryConditional.class));
		verify(mockTable, never()).deleteItem(any(Consumer.class));
	}

	@Test
	void testFindLastUpdateByDeviceId_withValidDeviceId_returnsLastUpdate() {
		DeviceUpdateData deviceUpdate = createDeviceUpdate();
		when(mockTable.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.items()).thenReturn(mockSdkIterable);
		when(mockSdkIterable.stream()).thenReturn(Stream.of(deviceUpdate));

		Optional<Long> result = repository.findLastUpdateByDeviceId(DEVICE_ID);

		assertTrue(result.isPresent());
		assertEquals(LAST_UPDATE, result.get());
		verify(mockTable).query(any(QueryConditional.class));
	}

	@Test
	void testFindLastUpdateByDeviceId_withNoResults_returnsEmpty() {
		when(mockTable.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.items()).thenReturn(mockSdkIterable);
		when(mockSdkIterable.stream()).thenReturn(Stream.<DeviceUpdateData>empty());

		Optional<Long> result = repository.findLastUpdateByDeviceId(DEVICE_ID);

		assertFalse(result.isPresent());
		verify(mockTable).query(any(QueryConditional.class));
	}

	@Test
	void testFindAll_returnsAllDeviceUpdates() {
		DeviceUpdateData deviceUpdate1 = createDeviceUpdate();
		DeviceUpdateData deviceUpdate2 = createDeviceUpdate();
		when(mockTable.scan()).thenReturn(mockPageIterable);
		when(mockPageIterable.items()).thenReturn(mockSdkIterable);

		Iterable<DeviceUpdateData> result = repository.findAll();

		assertEquals(mockSdkIterable, result);
		verify(mockTable).scan();
	}

	@Test
	void testFindByTimeRangeLessThan_withValidTime_returnsFilteredResults() {
		long olderThan = System.currentTimeMillis() - 10000;
		DeviceUpdateData deviceUpdate1 = createDeviceUpdate();
		DeviceUpdateData deviceUpdate2 = createDeviceUpdate();
		when(mockTable.index(DeviceUpdateData.IDENTITY_UPDATE_INDEX)).thenReturn(mockIndex);
		when(mockIndex.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(Stream.of(mockPage));
		when(mockPage.items()).thenReturn(Arrays.asList(deviceUpdate1, deviceUpdate2));

		Iterable<DeviceUpdateData> result = repository.findByTimeRangeLessThan(olderThan);

		assertNotNull(result);
		verify(mockTable).index(DeviceUpdateData.IDENTITY_UPDATE_INDEX);
		verify(mockIndex).query(any(QueryConditional.class));
	}

	@Test
	void testFindByTimeRangeLessThan_withNoResults_returnsEmptyList() {
		long olderThan = System.currentTimeMillis() - 10000;
		when(mockTable.index(DeviceUpdateData.IDENTITY_UPDATE_INDEX)).thenReturn(mockIndex);
		when(mockIndex.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(java.util.stream.Stream.empty());

		Iterable<DeviceUpdateData> result = repository.findByTimeRangeLessThan(olderThan);

		assertNotNull(result);
		verify(mockTable).index(DeviceUpdateData.IDENTITY_UPDATE_INDEX);
		verify(mockIndex).query(any(QueryConditional.class));
	}

	@Test
	void testGetTableName_returnsCorrectTableName() {
		assertEquals("device_updates", repository.getTableName());
	}

	@Test
	void testGetObjectClass_returnsCorrectClass() {
		assertEquals(DeviceUpdateData.class, repository.getObjectClass());
	}

	private DeviceUpdateData createDeviceUpdate() {
		DeviceUpdateData deviceUpdate = new DeviceUpdateData();
		deviceUpdate.setDeviceId(DEVICE_ID);
		deviceUpdate.setLastUpdate(LAST_UPDATE);
		return deviceUpdate;
	}
}
