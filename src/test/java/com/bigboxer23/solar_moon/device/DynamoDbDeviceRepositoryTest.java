package com.bigboxer23.solar_moon.device;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.data.Device;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
public class DynamoDbDeviceRepositoryTest {

	@Mock
	private DynamoDbTable<Device> mockTable;

	@Mock
	private DynamoDbIndex<Device> mockIndex;

	@Mock
	private PageIterable<Device> mockPageIterable;

	@Mock
	private SdkIterable<Device> mockSdkIterable;

	@Mock
	private Page<Device> mockPage;

	private DynamoDbDeviceRepository repository;

	private static final String CUSTOMER_ID = "customer-123";
	private static final String DEVICE_ID = "device-123";
	private static final String DEVICE_NAME = "Test Device";
	private static final String DEVICE_KEY = "device-key-123";
	private static final String SITE_ID = "site-123";

	private static class TestableDynamoDbDeviceRepository extends DynamoDbDeviceRepository {
		private final DynamoDbTable<Device> table;

		public TestableDynamoDbDeviceRepository(DynamoDbTable<Device> table) {
			this.table = table;
		}

		@Override
		protected DynamoDbTable<Device> getTable() {
			return table;
		}
	}

	@BeforeEach
	void setUp() {
		repository = new TestableDynamoDbDeviceRepository(mockTable);
	}

	@Test
	void testFindDeviceByDeviceName_withValidParameters_returnsDevice() {
		Device expectedDevice = createDevice();
		when(mockTable.index(Device.DEVICE_NAME_INDEX)).thenReturn(mockIndex);
		when(mockIndex.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(Stream.of(mockPage));
		when(mockPage.items()).thenReturn(Collections.singletonList(expectedDevice));

		Optional<Device> result = repository.findDeviceByDeviceName(CUSTOMER_ID, DEVICE_NAME);

		assertTrue(result.isPresent());
		assertEquals(expectedDevice, result.get());
		verify(mockTable).index(Device.DEVICE_NAME_INDEX);
	}

	@Test
	void testFindDeviceByDeviceName_withBlankDeviceName_returnsEmpty() {
		Optional<Device> result = repository.findDeviceByDeviceName(CUSTOMER_ID, "");

		assertFalse(result.isPresent());
		verify(mockTable, never()).index(anyString());
	}

	@Test
	void testFindDeviceByDeviceName_withNullDeviceName_returnsEmpty() {
		Optional<Device> result = repository.findDeviceByDeviceName(CUSTOMER_ID, null);

		assertFalse(result.isPresent());
		verify(mockTable, never()).index(anyString());
	}

	@Test
	void testFindDeviceByDeviceName_withBlankCustomerId_returnsEmpty() {
		Optional<Device> result = repository.findDeviceByDeviceName("", DEVICE_NAME);

		assertFalse(result.isPresent());
		verify(mockTable, never()).index(anyString());
	}

	@Test
	void testFindDeviceByDeviceName_withNullCustomerId_returnsEmpty() {
		Optional<Device> result = repository.findDeviceByDeviceName(null, DEVICE_NAME);

		assertFalse(result.isPresent());
		verify(mockTable, never()).index(anyString());
	}

	@Test
	void testFindDeviceByName_withValidParameters_returnsDevice() {
		Device expectedDevice = createDevice();
		when(mockTable.index(Device.NAME_INDEX)).thenReturn(mockIndex);
		when(mockIndex.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(Stream.of(mockPage));
		when(mockPage.items()).thenReturn(Collections.singletonList(expectedDevice));

		Optional<Device> result = repository.findDeviceByName(CUSTOMER_ID, DEVICE_NAME);

		assertTrue(result.isPresent());
		assertEquals(expectedDevice, result.get());
		verify(mockTable).index(Device.NAME_INDEX);
	}

	@Test
	void testFindDeviceByName_withBlankName_returnsEmpty() {
		Optional<Device> result = repository.findDeviceByName(CUSTOMER_ID, "");

		assertFalse(result.isPresent());
		verify(mockTable, never()).index(anyString());
	}

	@Test
	void testFindDeviceByName_withNullName_returnsEmpty() {
		Optional<Device> result = repository.findDeviceByName(CUSTOMER_ID, null);

		assertFalse(result.isPresent());
		verify(mockTable, never()).index(anyString());
	}

	@Test
	void testFindDeviceByName_withBlankCustomerId_returnsEmpty() {
		Optional<Device> result = repository.findDeviceByName("", DEVICE_NAME);

		assertFalse(result.isPresent());
		verify(mockTable, never()).index(anyString());
	}

	@Test
	void testFindDeviceByName_withNullCustomerId_returnsEmpty() {
		Optional<Device> result = repository.findDeviceByName(null, DEVICE_NAME);

		assertFalse(result.isPresent());
		verify(mockTable, never()).index(anyString());
	}

	@Test
	void testFindDeviceByDeviceKey_withValidDeviceKey_returnsDevice() {
		Device expectedDevice = createDevice();
		when(mockTable.index(Device.DEVICE_KEY_INDEX)).thenReturn(mockIndex);
		when(mockIndex.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(Stream.of(mockPage));
		when(mockPage.items()).thenReturn(Collections.singletonList(expectedDevice));

		Optional<Device> result = repository.findDeviceByDeviceKey(DEVICE_KEY);

		assertTrue(result.isPresent());
		assertEquals(expectedDevice, result.get());
		verify(mockTable).index(Device.DEVICE_KEY_INDEX);
	}

	@Test
	void testFindDeviceByDeviceKey_withBlankDeviceKey_returnsEmpty() {
		Optional<Device> result = repository.findDeviceByDeviceKey("");

		assertFalse(result.isPresent());
		verify(mockTable, never()).index(anyString());
	}

	@Test
	void testFindDeviceByDeviceKey_withNullDeviceKey_returnsEmpty() {
		Optional<Device> result = repository.findDeviceByDeviceKey(null);

		assertFalse(result.isPresent());
		verify(mockTable, never()).index(anyString());
	}

	@Test
	void testGetDevicesBySiteId_withValidParameters_returnsDevices() {
		Device device1 = createDevice();
		Device device2 = createDevice();
		when(mockTable.index(Device.SITEID_INDEX)).thenReturn(mockIndex);
		when(mockIndex.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(Stream.of(mockPage));
		when(mockPage.items()).thenReturn(Arrays.asList(device1, device2));

		List<Device> result = repository.getDevicesBySiteId(CUSTOMER_ID, SITE_ID);

		assertEquals(2, result.size());
		verify(mockTable).index(Device.SITEID_INDEX);
	}

	@Test
	void testGetDevicesBySiteId_withNullCustomerId_returnsEmptyList() {
		List<Device> result = repository.getDevicesBySiteId(null, SITE_ID);

		assertTrue(result.isEmpty());
		verify(mockTable, never()).index(anyString());
	}

	@Test
	void testGetDevicesBySiteId_withBlankCustomerId_returnsEmptyList() {
		List<Device> result = repository.getDevicesBySiteId("", SITE_ID);

		assertTrue(result.isEmpty());
		verify(mockTable, never()).index(anyString());
	}

	@Test
	void testGetDevicesBySiteId_withNullSiteId_returnsEmptyList() {
		List<Device> result = repository.getDevicesBySiteId(CUSTOMER_ID, null);

		assertTrue(result.isEmpty());
		verify(mockTable, never()).index(anyString());
	}

	@Test
	void testGetDevicesBySiteId_withBlankSiteId_returnsEmptyList() {
		List<Device> result = repository.getDevicesBySiteId(CUSTOMER_ID, "");

		assertTrue(result.isEmpty());
		verify(mockTable, never()).index(anyString());
	}

	@Test
	void testGetDevicesBySite_withValidParameters_returnsDevices() {
		Device device1 = createDevice();
		Device device2 = createDevice();
		when(mockTable.index(Device.SITE_INDEX)).thenReturn(mockIndex);
		when(mockIndex.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(Stream.of(mockPage));
		when(mockPage.items()).thenReturn(Arrays.asList(device1, device2));

		List<Device> result = repository.getDevicesBySite(CUSTOMER_ID, "site-name");

		assertEquals(2, result.size());
		verify(mockTable).index(Device.SITE_INDEX);
	}

	@Test
	void testGetDevicesBySite_withNullCustomerId_returnsEmptyList() {
		List<Device> result = repository.getDevicesBySite(null, "site-name");

		assertTrue(result.isEmpty());
		verify(mockTable, never()).index(anyString());
	}

	@Test
	void testGetDevicesBySite_withBlankCustomerId_returnsEmptyList() {
		List<Device> result = repository.getDevicesBySite("", "site-name");

		assertTrue(result.isEmpty());
		verify(mockTable, never()).index(anyString());
	}

	@Test
	void testGetDevicesBySite_withNullSite_returnsEmptyList() {
		List<Device> result = repository.getDevicesBySite(CUSTOMER_ID, null);

		assertTrue(result.isEmpty());
		verify(mockTable, never()).index(anyString());
	}

	@Test
	void testGetDevicesBySite_withBlankSite_returnsEmptyList() {
		List<Device> result = repository.getDevicesBySite(CUSTOMER_ID, "");

		assertTrue(result.isEmpty());
		verify(mockTable, never()).index(anyString());
	}

	@Test
	void testGetDevices_withVirtualTrue_returnsVirtualDevices() {
		Device device1 = createDevice();
		Device device2 = createDevice();
		when(mockTable.index(Device.VIRTUAL_INDEX)).thenReturn(mockIndex);
		when(mockIndex.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(Stream.of(mockPage));
		when(mockPage.items()).thenReturn(Arrays.asList(device1, device2));

		List<Device> result = repository.getDevices(true);

		assertEquals(2, result.size());
		verify(mockTable).index(Device.VIRTUAL_INDEX);
	}

	@Test
	void testGetDevices_withVirtualFalse_returnsPhysicalDevices() {
		Device device1 = createDevice();
		Device device2 = createDevice();
		when(mockTable.index(Device.VIRTUAL_INDEX)).thenReturn(mockIndex);
		when(mockIndex.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(Stream.of(mockPage));
		when(mockPage.items()).thenReturn(Arrays.asList(device1, device2));

		List<Device> result = repository.getDevices(false);

		assertEquals(2, result.size());
		verify(mockTable).index(Device.VIRTUAL_INDEX);
	}

	@Test
	void testGetSites_returnsSiteDevices() {
		Device device1 = createDevice();
		Device device2 = createDevice();
		when(mockTable.index(Device.IS_SITE_INDEX)).thenReturn(mockIndex);
		when(mockIndex.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(Stream.of(mockPage));
		when(mockPage.items()).thenReturn(Arrays.asList(device1, device2));

		List<Device> result = repository.getSites();

		assertEquals(2, result.size());
		verify(mockTable).index(Device.IS_SITE_INDEX);
	}

	@Test
	void testGetDevicesForCustomerId_withValidCustomerId_returnsDevices() {
		Device device1 = createDevice();
		Device device2 = createDevice();
		when(mockTable.index(Device.CLIENT_INDEX)).thenReturn(mockIndex);
		when(mockIndex.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(Stream.of(mockPage));
		when(mockPage.items()).thenReturn(Arrays.asList(device1, device2));

		List<Device> result = repository.getDevicesForCustomerId(CUSTOMER_ID);

		assertEquals(2, result.size());
		verify(mockTable).index(Device.CLIENT_INDEX);
	}

	@Test
	void testGetDevicesForCustomerId_withBlankCustomerId_returnsEmptyList() {
		List<Device> result = repository.getDevicesForCustomerId("");

		assertTrue(result.isEmpty());
		verify(mockTable, never()).index(anyString());
	}

	@Test
	void testGetDevicesForCustomerId_withNullCustomerId_returnsEmptyList() {
		List<Device> result = repository.getDevicesForCustomerId(null);

		assertTrue(result.isEmpty());
		verify(mockTable, never()).index(anyString());
	}

	@Test
	void testFindDeviceById_withValidId_returnsDevice() {
		Device expectedDevice = createDevice();
		when(mockTable.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.items()).thenReturn(mockSdkIterable);
		when(mockSdkIterable.stream()).thenReturn(Stream.of(expectedDevice));

		Optional<Device> result = repository.findDeviceById(DEVICE_ID);

		assertTrue(result.isPresent());
		assertEquals(expectedDevice, result.get());
		verify(mockTable).query(any(QueryConditional.class));
	}

	@Test
	void testFindDeviceById_withBlankId_returnsEmpty() {
		Optional<Device> result = repository.findDeviceById("");

		assertFalse(result.isPresent());
		verify(mockTable, never()).query(any(QueryConditional.class));
	}

	@Test
	void testFindDeviceById_withNullId_returnsEmpty() {
		Optional<Device> result = repository.findDeviceById(null);

		assertFalse(result.isPresent());
		verify(mockTable, never()).query(any(QueryConditional.class));
	}

	@Test
	void testFindDeviceByIdAndCustomerId_withValidParameters_returnsDevice() {
		Device expectedDevice = createDevice();
		when(mockTable.getItem(any(Device.class))).thenReturn(expectedDevice);

		Optional<Device> result = repository.findDeviceById(DEVICE_ID, CUSTOMER_ID);

		assertTrue(result.isPresent());
		assertEquals(expectedDevice, result.get());
		verify(mockTable).getItem(any(Device.class));
	}

	@Test
	void testFindDeviceByIdAndCustomerId_withBlankId_returnsEmpty() {
		Optional<Device> result = repository.findDeviceById("", CUSTOMER_ID);

		assertFalse(result.isPresent());
		verify(mockTable, never()).getItem(any(Device.class));
	}

	@Test
	void testFindDeviceByIdAndCustomerId_withNullId_returnsEmpty() {
		Optional<Device> result = repository.findDeviceById(null, CUSTOMER_ID);

		assertFalse(result.isPresent());
		verify(mockTable, never()).getItem(any(Device.class));
	}

	@Test
	void testFindDeviceByIdAndCustomerId_withBlankCustomerId_returnsEmpty() {
		Optional<Device> result = repository.findDeviceById(DEVICE_ID, "");

		assertFalse(result.isPresent());
		verify(mockTable, never()).getItem(any(Device.class));
	}

	@Test
	void testFindDeviceByIdAndCustomerId_withNullCustomerId_returnsEmpty() {
		Optional<Device> result = repository.findDeviceById(DEVICE_ID, null);

		assertFalse(result.isPresent());
		verify(mockTable, never()).getItem(any(Device.class));
	}

	@Test
	void testDelete_withValidDevice_deletesSuccessfully() {
		Device device = createDevice();

		repository.delete(device);

		verify(mockTable).deleteItem(device);
	}

	@Test
	void testDelete_withNullDevice_doesNotDelete() {
		repository.delete(null);

		verify(mockTable, never()).deleteItem(any(Device.class));
	}

	@Test
	void testGetTableName_returnsCorrectTableName() {
		assertEquals("devices", repository.getTableName());
	}

	@Test
	void testGetObjectClass_returnsCorrectClass() {
		assertEquals(Device.class, repository.getObjectClass());
	}

	private Device createDevice() {
		return new Device(DEVICE_ID, CUSTOMER_ID, DEVICE_NAME);
	}
}
