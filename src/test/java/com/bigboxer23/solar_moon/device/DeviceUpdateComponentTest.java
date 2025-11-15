package com.bigboxer23.solar_moon.device;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceUpdateData;
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
public class DeviceUpdateComponentTest {

	@Mock
	private DeviceUpdateRepository mockRepository;

	@Mock
	private DeviceComponent mockDeviceComponent;

	private TestableDeviceUpdateComponent deviceUpdateComponent;

	private static final String DEVICE_ID = "device-123";
	private static final String CUSTOMER_ID = "customer-123";
	private static final long TEST_TIME = System.currentTimeMillis();

	private static class TestableDeviceUpdateComponent extends DeviceUpdateComponent {
		private final DeviceUpdateRepository repository;

		public TestableDeviceUpdateComponent(DeviceUpdateRepository repository) {
			this.repository = repository;
		}

		@Override
		protected DeviceUpdateRepository getRepository() {
			return repository;
		}
	}

	@BeforeEach
	void setUp() {
		deviceUpdateComponent = new TestableDeviceUpdateComponent(mockRepository);
	}

	@Test
	void testUpdate_withDeviceId_updatesWithCurrentTime() {
		when(mockRepository.update(any(DeviceUpdateData.class))).thenAnswer(invocation -> invocation.getArgument(0));

		deviceUpdateComponent.update(DEVICE_ID);

		verify(mockRepository).update(any(DeviceUpdateData.class));
	}

	@Test
	void testUpdate_withBlankDeviceId_doesNotUpdate() {
		deviceUpdateComponent.update("");

		verify(mockRepository, never()).update(any(DeviceUpdateData.class));
	}

	@Test
	void testUpdate_withNullDeviceId_doesNotUpdate() {
		deviceUpdateComponent.update(null);

		verify(mockRepository, never()).update(any(DeviceUpdateData.class));
	}

	@Test
	void testUpdate_withDeviceIdAndTime_updatesWithSpecificTime() {
		when(mockRepository.update(any(DeviceUpdateData.class))).thenAnswer(invocation -> invocation.getArgument(0));

		deviceUpdateComponent.update(DEVICE_ID, TEST_TIME);

		verify(mockRepository).update(any(DeviceUpdateData.class));
	}

	@Test
	void testUpdate_withBlankDeviceIdAndTime_doesNotUpdate() {
		deviceUpdateComponent.update("", TEST_TIME);

		verify(mockRepository, never()).update(any(DeviceUpdateData.class));
	}

	@Test
	void testDelete_withValidDeviceId_deletesSuccessfully() {
		deviceUpdateComponent.delete(DEVICE_ID);

		verify(mockRepository).delete(DEVICE_ID);
	}

	@Test
	void testQueryByDeviceId_withValidDeviceId_returnsLastUpdate() {
		when(mockRepository.findLastUpdateByDeviceId(DEVICE_ID)).thenReturn(Optional.of(TEST_TIME));

		long result = deviceUpdateComponent.queryByDeviceId(DEVICE_ID);

		assertEquals(TEST_TIME, result);
		verify(mockRepository).findLastUpdateByDeviceId(DEVICE_ID);
	}

	@Test
	void testQueryByDeviceId_withNoRecords_returnsMinusOne() {
		when(mockRepository.findLastUpdateByDeviceId(DEVICE_ID)).thenReturn(Optional.empty());

		long result = deviceUpdateComponent.queryByDeviceId(DEVICE_ID);

		assertEquals(-1L, result);
		verify(mockRepository).findLastUpdateByDeviceId(DEVICE_ID);
	}

	@Test
	void testGetDevices_returnsAllDevices() {
		List<DeviceUpdateData> expectedDevices = Arrays.asList(createDeviceUpdateData(), createDeviceUpdateData());
		when(mockRepository.findAll()).thenReturn(expectedDevices);

		Iterable<DeviceUpdateData> result = deviceUpdateComponent.getDevices();

		assertNotNull(result);
		verify(mockRepository).findAll();
	}

	@Test
	void testQueryByTimeRange_withValidTimeRange_returnsMatchingDevices() {
		long olderThan = System.currentTimeMillis() - 10000;
		DeviceUpdateData oldDevice = new DeviceUpdateData(DEVICE_ID, olderThan - 5000);
		when(mockRepository.findByTimeRangeLessThan(olderThan)).thenReturn(Collections.singletonList(oldDevice));

		Iterable<DeviceUpdateData> result = deviceUpdateComponent.queryByTimeRange(olderThan);

		assertNotNull(result);
		verify(mockRepository).findByTimeRangeLessThan(olderThan);
	}

	@Test
	void testQueryByTimeRange_withNoMatchingDevices_returnsEmpty() {
		long olderThan = System.currentTimeMillis() - 10000;
		when(mockRepository.findByTimeRangeLessThan(olderThan)).thenReturn(Collections.emptyList());

		Iterable<DeviceUpdateData> result = deviceUpdateComponent.queryByTimeRange(olderThan);

		assertNotNull(result);
		verify(mockRepository).findByTimeRangeLessThan(olderThan);
	}

	@Test
	void testQueryByTimeRange_withZeroTime_queriesCorrectly() {
		when(mockRepository.findByTimeRangeLessThan(0)).thenReturn(Collections.emptyList());

		Iterable<DeviceUpdateData> result = deviceUpdateComponent.queryByTimeRange(0);

		assertNotNull(result);
		verify(mockRepository).findByTimeRangeLessThan(0);
	}

	@Test
	void testQueryByTimeRange_withFutureTime_queriesCorrectly() {
		long futureTime = System.currentTimeMillis() + 100000;
		when(mockRepository.findByTimeRangeLessThan(futureTime)).thenReturn(Collections.emptyList());

		Iterable<DeviceUpdateData> result = deviceUpdateComponent.queryByTimeRange(futureTime);

		assertNotNull(result);
		verify(mockRepository).findByTimeRangeLessThan(futureTime);
	}

	private DeviceUpdateData createDeviceUpdateData() {
		return new DeviceUpdateData(DEVICE_ID, TEST_TIME);
	}

	private Device createDevice(String deviceId) {
		return new Device(deviceId, CUSTOMER_ID, "Test Device " + deviceId);
	}
}
