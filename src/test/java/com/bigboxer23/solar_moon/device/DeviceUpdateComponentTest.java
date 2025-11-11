package com.bigboxer23.solar_moon.device;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceUpdateData;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
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
public class DeviceUpdateComponentTest {

	@Mock
	private DynamoDbTable<DeviceUpdateData> mockTable;

	@Mock
	private DynamoDbIndex<DeviceUpdateData> mockIndex;

	@Mock
	private DeviceComponent mockDeviceComponent;

	@Mock
	private PageIterable<DeviceUpdateData> mockPageIterable;

	@Mock
	private Page<DeviceUpdateData> mockPage;

	private TestableDeviceUpdateComponent deviceUpdateComponent;

	private static final String DEVICE_ID = "device-123";
	private static final String CUSTOMER_ID = "customer-123";
	private static final long TEST_TIME = System.currentTimeMillis();

	private static class TestableDeviceUpdateComponent extends DeviceUpdateComponent {
		private final DynamoDbTable<DeviceUpdateData> table;
		private final DeviceComponent deviceComponent;

		public TestableDeviceUpdateComponent(DynamoDbTable<DeviceUpdateData> table, DeviceComponent deviceComponent) {
			this.table = table;
			this.deviceComponent = deviceComponent;
		}

		@Override
		protected DynamoDbTable<DeviceUpdateData> getTable() {
			return table;
		}
	}

	@BeforeEach
	void setUp() {
		deviceUpdateComponent = new TestableDeviceUpdateComponent(mockTable, mockDeviceComponent);
	}

	@Test
	void testUpdate_withDeviceId_updatesWithCurrentTime() {
		when(mockTable.updateItem(any(java.util.function.Consumer.class)))
				.thenAnswer(invocation -> new DeviceUpdateData(DEVICE_ID, System.currentTimeMillis()));

		deviceUpdateComponent.update(DEVICE_ID);

		verify(mockTable).updateItem(any(java.util.function.Consumer.class));
	}

	@Test
	void testUpdate_withBlankDeviceId_doesNotUpdate() {
		deviceUpdateComponent.update("");

		verify(mockTable, never()).updateItem(any(java.util.function.Consumer.class));
	}

	@Test
	void testUpdate_withNullDeviceId_doesNotUpdate() {
		deviceUpdateComponent.update(null);

		verify(mockTable, never()).updateItem(any(java.util.function.Consumer.class));
	}

	@Test
	void testUpdate_withDeviceIdAndTime_updatesWithSpecificTime() {
		when(mockTable.updateItem(any(java.util.function.Consumer.class)))
				.thenAnswer(invocation -> new DeviceUpdateData(DEVICE_ID, TEST_TIME));

		deviceUpdateComponent.update(DEVICE_ID, TEST_TIME);

		verify(mockTable).updateItem(any(java.util.function.Consumer.class));
	}

	@Test
	void testUpdate_withBlankDeviceIdAndTime_doesNotUpdate() {
		deviceUpdateComponent.update("", TEST_TIME);

		verify(mockTable, never()).updateItem(any(java.util.function.Consumer.class));
	}

	@Test
	void testDelete_withValidDeviceId_deletesSuccessfully() {
		DeviceUpdateData updateData = createDeviceUpdateData();
		when(mockTable.query((QueryConditional) any())).thenReturn(mockPageIterable);
		when(mockPageIterable.items())
				.thenReturn(() -> Collections.singletonList(updateData).iterator());

		deviceUpdateComponent.delete(DEVICE_ID);

		verify(mockTable).query((QueryConditional) any());
		verify(mockTable).deleteItem(any(java.util.function.Consumer.class));
	}

	@Test
	void testDelete_withMultipleRecords_deletesAll() {
		DeviceUpdateData updateData1 = createDeviceUpdateData();
		DeviceUpdateData updateData2 = createDeviceUpdateData();
		when(mockTable.query((QueryConditional) any())).thenReturn(mockPageIterable);
		when(mockPageIterable.items())
				.thenReturn(() -> Arrays.asList(updateData1, updateData2).iterator());

		deviceUpdateComponent.delete(DEVICE_ID);

		verify(mockTable).query((QueryConditional) any());
		verify(mockTable, times(2)).deleteItem(any(java.util.function.Consumer.class));
	}

	@Test
	void testDelete_withNoRecords_doesNotDelete() {
		when(mockTable.query((QueryConditional) any())).thenReturn(mockPageIterable);
		when(mockPageIterable.items()).thenReturn(Collections::emptyIterator);

		deviceUpdateComponent.delete(DEVICE_ID);

		verify(mockTable).query((QueryConditional) any());
		verify(mockTable, never()).deleteItem(any(java.util.function.Consumer.class));
	}

	@Test
	void testQueryByDeviceId_withValidDeviceId_returnsLastUpdate() {
		DeviceUpdateData updateData = createDeviceUpdateData();
		when(mockTable.query((QueryConditional) any())).thenReturn(mockPageIterable);
		when(mockPageIterable.items())
				.thenReturn(() -> Collections.singletonList(updateData).iterator());

		long result = deviceUpdateComponent.queryByDeviceId(DEVICE_ID);

		assertEquals(TEST_TIME, result);
		verify(mockTable).query((QueryConditional) any());
	}

	@Test
	void testQueryByDeviceId_withNoRecords_returnsMinusOne() {
		when(mockTable.query((QueryConditional) any())).thenReturn(mockPageIterable);
		when(mockPageIterable.items()).thenReturn(Collections::emptyIterator);

		long result = deviceUpdateComponent.queryByDeviceId(DEVICE_ID);

		assertEquals(-1L, result);
		verify(mockTable).query((QueryConditional) any());
	}

	@Test
	void testGetDevices_returnsAllDevices() {
		List<DeviceUpdateData> expectedDevices = Arrays.asList(createDeviceUpdateData(), createDeviceUpdateData());
		when(mockTable.scan()).thenReturn(mockPageIterable);
		when(mockPageIterable.items()).thenReturn(expectedDevices::iterator);

		Iterable<DeviceUpdateData> result = deviceUpdateComponent.getDevices();

		assertNotNull(result);
	}

	@Test
	void testQueryByTimeRange_withValidTimeRange_returnsMatchingDevices() {
		long olderThan = System.currentTimeMillis() - 10000;
		DeviceUpdateData oldDevice = new DeviceUpdateData(DEVICE_ID, olderThan - 5000);
		when(mockTable.index(DeviceUpdateData.IDENTITY_UPDATE_INDEX)).thenReturn(mockIndex);
		when(mockIndex.query((QueryConditional) any())).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(Stream.of(mockPage));
		when(mockPage.items()).thenReturn(Collections.singletonList(oldDevice));

		Iterable<DeviceUpdateData> result = deviceUpdateComponent.queryByTimeRange(olderThan);

		assertNotNull(result);
		verify(mockTable).index(DeviceUpdateData.IDENTITY_UPDATE_INDEX);
		verify(mockIndex).query((QueryConditional) any());
	}

	@Test
	void testQueryByTimeRange_withNoMatchingDevices_returnsEmpty() {
		long olderThan = System.currentTimeMillis() - 10000;
		when(mockTable.index(DeviceUpdateData.IDENTITY_UPDATE_INDEX)).thenReturn(mockIndex);
		when(mockIndex.query((QueryConditional) any())).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(Stream.empty());

		Iterable<DeviceUpdateData> result = deviceUpdateComponent.queryByTimeRange(olderThan);

		assertNotNull(result);
		verify(mockTable).index(DeviceUpdateData.IDENTITY_UPDATE_INDEX);
	}

	@Test
	void testQueryByTimeRange_withZeroTime_queriesCorrectly() {
		when(mockTable.index(DeviceUpdateData.IDENTITY_UPDATE_INDEX)).thenReturn(mockIndex);
		when(mockIndex.query((QueryConditional) any())).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(Stream.empty());

		Iterable<DeviceUpdateData> result = deviceUpdateComponent.queryByTimeRange(0);

		assertNotNull(result);
		verify(mockTable).index(DeviceUpdateData.IDENTITY_UPDATE_INDEX);
	}

	@Test
	void testQueryByTimeRange_withFutureTime_queriesCorrectly() {
		long futureTime = System.currentTimeMillis() + 100000;
		when(mockTable.index(DeviceUpdateData.IDENTITY_UPDATE_INDEX)).thenReturn(mockIndex);
		when(mockIndex.query((QueryConditional) any())).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(Stream.empty());

		Iterable<DeviceUpdateData> result = deviceUpdateComponent.queryByTimeRange(futureTime);

		assertNotNull(result);
		verify(mockTable).index(DeviceUpdateData.IDENTITY_UPDATE_INDEX);
	}

	private DeviceUpdateData createDeviceUpdateData() {
		return new DeviceUpdateData(DEVICE_ID, TEST_TIME);
	}

	private Device createDevice(String deviceId) {
		return new Device(deviceId, CUSTOMER_ID, "Test Device " + deviceId);
	}
}
