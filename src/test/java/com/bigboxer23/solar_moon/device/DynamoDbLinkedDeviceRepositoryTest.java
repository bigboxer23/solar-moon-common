package com.bigboxer23.solar_moon.device;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.LinkedDevice;
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
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

@ExtendWith(MockitoExtension.class)
public class DynamoDbLinkedDeviceRepositoryTest {

	@Mock
	private DynamoDbTable<LinkedDevice> mockTable;

	@Mock
	private DeviceComponent mockDeviceComponent;

	@Mock
	private PageIterable<LinkedDevice> mockPageIterable;

	@Mock
	private SdkIterable<LinkedDevice> mockSdkIterable;

	@Mock
	private Page<LinkedDevice> mockPage;

	private DynamoDbLinkedDeviceRepository repository;

	private static final String SERIAL_NUMBER = "serial-123";
	private static final String CUSTOMER_ID = "customer-123";

	private static class TestableDynamoDbLinkedDeviceRepository extends DynamoDbLinkedDeviceRepository {
		private final DynamoDbTable<LinkedDevice> table;
		private final DeviceComponent deviceComponent;

		public TestableDynamoDbLinkedDeviceRepository(
				DynamoDbTable<LinkedDevice> table, DeviceComponent deviceComponent) {
			this.table = table;
			this.deviceComponent = deviceComponent;
		}

		@Override
		protected DynamoDbTable<LinkedDevice> getTable() {
			return table;
		}

		@Override
		protected DeviceComponent getDeviceComponent() {
			return deviceComponent;
		}
	}

	@BeforeEach
	void setUp() {
		repository = new TestableDynamoDbLinkedDeviceRepository(mockTable, mockDeviceComponent);
	}

	@Test
	void testDelete_withValidParameters_deletesSuccessfully() {
		repository.delete(SERIAL_NUMBER, CUSTOMER_ID);

		verify(mockTable).deleteItem(any(Consumer.class));
	}

	@Test
	void testDelete_withBlankSerialNumber_doesNotDelete() {
		repository.delete("", CUSTOMER_ID);

		verify(mockTable, never()).deleteItem(any(Consumer.class));
	}

	@Test
	void testDelete_withNullSerialNumber_doesNotDelete() {
		repository.delete(null, CUSTOMER_ID);

		verify(mockTable, never()).deleteItem(any(Consumer.class));
	}

	@Test
	void testDelete_withBlankCustomerId_doesNotDelete() {
		repository.delete(SERIAL_NUMBER, "");

		verify(mockTable, never()).deleteItem(any(Consumer.class));
	}

	@Test
	void testDelete_withNullCustomerId_doesNotDelete() {
		repository.delete(SERIAL_NUMBER, null);

		verify(mockTable, never()).deleteItem(any(Consumer.class));
	}

	@Test
	void testDeleteByCustomerId_withValidCustomerId_deletesAllDevices() {
		Device device1 = createDevice("device-1", "serial-1");
		Device device2 = createDevice("device-2", "serial-2");
		when(mockDeviceComponent.getDevicesForCustomerId(CUSTOMER_ID)).thenReturn(Arrays.asList(device1, device2));

		repository.deleteByCustomerId(CUSTOMER_ID);

		verify(mockDeviceComponent).getDevicesForCustomerId(CUSTOMER_ID);
		verify(mockTable, times(2)).deleteItem(any(Consumer.class));
	}

	@Test
	void testDeleteByCustomerId_withNoDevices_doesNotDelete() {
		when(mockDeviceComponent.getDevicesForCustomerId(CUSTOMER_ID)).thenReturn(Collections.emptyList());

		repository.deleteByCustomerId(CUSTOMER_ID);

		verify(mockDeviceComponent).getDevicesForCustomerId(CUSTOMER_ID);
		verify(mockTable, never()).deleteItem(any(Consumer.class));
	}

	@Test
	void testDeleteByCustomerId_withDevicesWithoutSerialNumber_skipsThoseDevices() {
		Device device1 = createDevice("device-1", "serial-1");
		Device device2 = createDevice("device-2", null);
		Device device3 = createDevice("device-3", "");
		when(mockDeviceComponent.getDevicesForCustomerId(CUSTOMER_ID))
				.thenReturn(Arrays.asList(device1, device2, device3));

		repository.deleteByCustomerId(CUSTOMER_ID);

		verify(mockDeviceComponent).getDevicesForCustomerId(CUSTOMER_ID);
		verify(mockTable, times(1)).deleteItem(any(Consumer.class));
	}

	@Test
	void testFindBySerialNumber_withValidParameters_returnsDevice() {
		LinkedDevice expectedDevice = createLinkedDevice();
		when(mockTable.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.items()).thenReturn(mockSdkIterable);
		when(mockSdkIterable.stream()).thenReturn(Stream.of(expectedDevice));

		Optional<LinkedDevice> result = repository.findBySerialNumber(SERIAL_NUMBER, CUSTOMER_ID);

		assertTrue(result.isPresent());
		assertEquals(expectedDevice, result.get());
		verify(mockTable).query(any(QueryConditional.class));
	}

	@Test
	void testFindBySerialNumber_withNoResults_returnsEmpty() {
		when(mockTable.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.items()).thenReturn(mockSdkIterable);
		when(mockSdkIterable.stream()).thenReturn(Stream.empty());

		Optional<LinkedDevice> result = repository.findBySerialNumber(SERIAL_NUMBER, CUSTOMER_ID);

		assertFalse(result.isPresent());
		verify(mockTable).query(any(QueryConditional.class));
	}

	@Test
	void testFindBySerialNumber_withBlankSerialNumber_returnsEmpty() {
		Optional<LinkedDevice> result = repository.findBySerialNumber("", CUSTOMER_ID);

		assertFalse(result.isPresent());
		verify(mockTable, never()).query(any(QueryConditional.class));
	}

	@Test
	void testFindBySerialNumber_withNullSerialNumber_returnsEmpty() {
		Optional<LinkedDevice> result = repository.findBySerialNumber(null, CUSTOMER_ID);

		assertFalse(result.isPresent());
		verify(mockTable, never()).query(any(QueryConditional.class));
	}

	@Test
	void testFindBySerialNumber_withBlankCustomerId_returnsEmpty() {
		Optional<LinkedDevice> result = repository.findBySerialNumber(SERIAL_NUMBER, "");

		assertFalse(result.isPresent());
		verify(mockTable, never()).query(any(QueryConditional.class));
	}

	@Test
	void testFindBySerialNumber_withNullCustomerId_returnsEmpty() {
		Optional<LinkedDevice> result = repository.findBySerialNumber(SERIAL_NUMBER, null);

		assertFalse(result.isPresent());
		verify(mockTable, never()).query(any(QueryConditional.class));
	}

	@Test
	void testGetTableName_returnsCorrectTableName() {
		assertEquals("linked_devices", repository.getTableName());
	}

	@Test
	void testGetObjectClass_returnsCorrectClass() {
		assertEquals(LinkedDevice.class, repository.getObjectClass());
	}

	private LinkedDevice createLinkedDevice() {
		return new LinkedDevice(SERIAL_NUMBER, CUSTOMER_ID, 0, 0, System.currentTimeMillis());
	}

	private Device createDevice(String deviceId, String serialNumber) {
		Device device = new Device(deviceId, CUSTOMER_ID, "Test Device");
		device.setSerialNumber(serialNumber);
		return device;
	}
}
