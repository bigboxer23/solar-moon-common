package com.bigboxer23.solar_moon.device;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.alarm.ISolectriaConstants;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.data.LinkedDevice;
import com.bigboxer23.solar_moon.util.TimeConstants;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

@ExtendWith(MockitoExtension.class)
public class LinkedDeviceComponentTest {

	@Mock
	private DynamoDbTable<LinkedDevice> mockTable;

	@Mock
	private DeviceComponent mockDeviceComponent;

	@Mock
	private PageIterable<LinkedDevice> mockPageIterable;

	@Mock
	private Page<LinkedDevice> mockPage;

	private TestableLinkedDeviceComponent linkedDeviceComponent;

	private static final String CUSTOMER_ID = "test-customer-123";
	private static final String DEVICE_ID = "device-123";
	private static final String SERIAL_NUMBER = "serial-123";

	private static class TestableLinkedDeviceComponent extends LinkedDeviceComponent {
		private final DynamoDbTable<LinkedDevice> table;
		private final DeviceComponent deviceComponent;

		public TestableLinkedDeviceComponent(DynamoDbTable<LinkedDevice> table, DeviceComponent deviceComponent) {
			this.table = table;
			this.deviceComponent = deviceComponent;
		}

		@Override
		protected DynamoDbTable<LinkedDevice> getTable() {
			return table;
		}
	}

	@BeforeEach
	void setUp() {
		linkedDeviceComponent = new TestableLinkedDeviceComponent(mockTable, mockDeviceComponent);
	}

	@Test
	void testUpdate_withValidDevice_updatesSuccessfully() {
		LinkedDevice device = createLinkedDevice();
		when(mockTable.updateItem(any(java.util.function.Consumer.class))).thenReturn(device);

		linkedDeviceComponent.update(device);

		verify(mockTable).updateItem(any(java.util.function.Consumer.class));
	}

	@Test
	void testUpdate_withNullDevice_doesNotUpdate() {
		linkedDeviceComponent.update(null);

		verify(mockTable, never()).updateItem(any(java.util.function.Consumer.class));
	}

	@Test
	void testUpdate_withBlankId_doesNotUpdate() {
		LinkedDevice device = createLinkedDevice();
		device.setId("");

		linkedDeviceComponent.update(device);

		verify(mockTable, never()).updateItem(any(java.util.function.Consumer.class));
	}

	@Test
	void testUpdate_withNullId_doesNotUpdate() {
		LinkedDevice device = createLinkedDevice();
		device.setId(null);

		linkedDeviceComponent.update(device);

		verify(mockTable, never()).updateItem(any(java.util.function.Consumer.class));
	}

	@Test
	void testUpdate_withBlankCustomerId_doesNotUpdate() {
		LinkedDevice device = createLinkedDevice();
		device.setCustomerId("");

		linkedDeviceComponent.update(device);

		verify(mockTable, never()).updateItem(any(java.util.function.Consumer.class));
	}

	@Test
	void testUpdate_withNullCustomerId_doesNotUpdate() {
		LinkedDevice device = createLinkedDevice();
		device.setCustomerId(null);

		linkedDeviceComponent.update(device);

		verify(mockTable, never()).updateItem(any(java.util.function.Consumer.class));
	}

	@Test
	void testDelete_withValidParameters_deletesSuccessfully() {
		linkedDeviceComponent.delete(SERIAL_NUMBER, CUSTOMER_ID);

		verify(mockTable).deleteItem(any(java.util.function.Consumer.class));
	}

	@Test
	void testDelete_withBlankSerialNumber_doesNotDelete() {
		linkedDeviceComponent.delete("", CUSTOMER_ID);

		verify(mockTable, never()).deleteItem(any(java.util.function.Consumer.class));
	}

	@Test
	void testDelete_withNullSerialNumber_doesNotDelete() {
		linkedDeviceComponent.delete(null, CUSTOMER_ID);

		verify(mockTable, never()).deleteItem(any(java.util.function.Consumer.class));
	}

	@Test
	void testDelete_withBlankCustomerId_doesNotDelete() {
		linkedDeviceComponent.delete(SERIAL_NUMBER, "");

		verify(mockTable, never()).deleteItem(any(java.util.function.Consumer.class));
	}

	@Test
	void testDelete_withNullCustomerId_doesNotDelete() {
		linkedDeviceComponent.delete(SERIAL_NUMBER, null);

		verify(mockTable, never()).deleteItem(any(java.util.function.Consumer.class));
	}

	@Test
	void testQueryBySerialNumber_withValidParameters_returnsDevice() {
		LinkedDevice expectedDevice = createLinkedDevice();
		when(mockTable.query((QueryConditional) any())).thenReturn(mockPageIterable);
		when(mockPageIterable.items())
				.thenReturn(() -> Collections.singletonList(expectedDevice).iterator());

		Optional<LinkedDevice> result = linkedDeviceComponent.queryBySerialNumber(SERIAL_NUMBER, CUSTOMER_ID);

		assertTrue(result.isPresent());
		assertEquals(expectedDevice, result.get());
		verify(mockTable).query((QueryConditional) any());
	}

	@Test
	void testQueryBySerialNumber_withBlankSerialNumber_returnsEmpty() {
		Optional<LinkedDevice> result = linkedDeviceComponent.queryBySerialNumber("", CUSTOMER_ID);

		assertFalse(result.isPresent());
		verify(mockTable, never()).query((QueryConditional) any());
	}

	@Test
	void testQueryBySerialNumber_withNullSerialNumber_returnsEmpty() {
		Optional<LinkedDevice> result = linkedDeviceComponent.queryBySerialNumber(null, CUSTOMER_ID);

		assertFalse(result.isPresent());
		verify(mockTable, never()).query((QueryConditional) any());
	}

	@Test
	void testQueryBySerialNumber_withBlankCustomerId_returnsEmpty() {
		Optional<LinkedDevice> result = linkedDeviceComponent.queryBySerialNumber(SERIAL_NUMBER, "");

		assertFalse(result.isPresent());
		verify(mockTable, never()).query((QueryConditional) any());
	}

	@Test
	void testQueryBySerialNumber_withNullCustomerId_returnsEmpty() {
		Optional<LinkedDevice> result = linkedDeviceComponent.queryBySerialNumber(SERIAL_NUMBER, null);

		assertFalse(result.isPresent());
		verify(mockTable, never()).query((QueryConditional) any());
	}

	@Test
	void testAddLinkedDeviceDataVirtual_withNullVirtualDevice_returnsNull() {
		List<DeviceData> childDevices = List.of(createDeviceData());

		DeviceData result = linkedDeviceComponent.addLinkedDeviceDataVirtual(null, childDevices);

		assertNull(result);
	}

	@Test
	void testAddLinkedDeviceDataVirtual_withNullChildDevices_returnsVirtualDevice() {
		DeviceData virtualDevice = createDeviceData();

		DeviceData result = linkedDeviceComponent.addLinkedDeviceDataVirtual(virtualDevice, null);

		assertEquals(virtualDevice, result);
	}

	@Test
	void testAddLinkedDeviceDataVirtual_withEmptyChildDevices_returnsVirtualDevice() {
		DeviceData virtualDevice = createDeviceData();

		DeviceData result = linkedDeviceComponent.addLinkedDeviceDataVirtual(virtualDevice, Collections.emptyList());

		assertEquals(virtualDevice, result);
	}

	@Test
	void testAddLinkedDeviceDataVirtual_withInformationalErrors_aggregatesCorrectly() {
		DeviceData virtualDevice = createDeviceData();
		DeviceData child1 = createDeviceData();
		child1.setInformationalError(ISolectriaConstants.Fan_Life_Reached);
		DeviceData child2 = createDeviceData();
		child2.setInformationalError(ISolectriaConstants.AC_Voltage_High);

		DeviceData result =
				linkedDeviceComponent.addLinkedDeviceDataVirtual(virtualDevice, Arrays.asList(child1, child2));

		assertNotNull(result);
		assertTrue(result.getInformationalError() > -1);
		assertNotNull(result.getInformationalErrorString());
	}

	@Test
	void testAddLinkedDeviceDataVirtual_withCriticalErrors_aggregatesCorrectly() {
		DeviceData virtualDevice = createDeviceData();
		DeviceData child1 = createDeviceData();
		child1.setCriticalError(ISolectriaConstants.Power_Stage_Over_Temperature);
		DeviceData child2 = createDeviceData();
		child2.setCriticalError(ISolectriaConstants.AC_Contactor_Opened);

		DeviceData result =
				linkedDeviceComponent.addLinkedDeviceDataVirtual(virtualDevice, Arrays.asList(child1, child2));

		assertNotNull(result);
		assertTrue(result.getCriticalError() > -1);
		assertNotNull(result.getCriticalErrorString());
	}

	@Test
	void testAddLinkedDeviceDataVirtual_withMixedErrors_aggregatesCorrectly() {
		DeviceData virtualDevice = createDeviceData();
		DeviceData child1 = createDeviceData();
		child1.setInformationalError(ISolectriaConstants.Fan_Life_Reached);
		child1.setCriticalError(ISolectriaConstants.Power_Stage_Over_Temperature);
		DeviceData child2 = createDeviceData();
		child2.setInformationalError(ISolectriaConstants.AC_Voltage_High);

		DeviceData result =
				linkedDeviceComponent.addLinkedDeviceDataVirtual(virtualDevice, Arrays.asList(child1, child2));

		assertNotNull(result);
		assertTrue(result.getInformationalError() > -1);
		assertTrue(result.getCriticalError() > -1);
	}

	@Test
	void testAddLinkedDeviceData_withNullDevice_doesNotUpdate() {
		DeviceData deviceData = createDeviceData();

		linkedDeviceComponent.addLinkedDeviceData(null, deviceData);

		verify(mockTable, never()).query((QueryConditional) any());
	}

	@Test
	void testAddLinkedDeviceData_withNullDeviceData_doesNotUpdate() {
		Device device = createDevice();

		linkedDeviceComponent.addLinkedDeviceData(device, null);

		verify(mockTable, never()).query((QueryConditional) any());
	}

	@Test
	void testAddLinkedDeviceData_withBlankSerialNumber_doesNotUpdate() {
		Device device = createDevice();
		device.setSerialNumber("");
		DeviceData deviceData = createDeviceData();

		linkedDeviceComponent.addLinkedDeviceData(device, deviceData);

		verify(mockTable, never()).query((QueryConditional) any());
	}

	@Test
	void testAddLinkedDeviceData_withNullSerialNumber_doesNotUpdate() {
		Device device = createDevice();
		device.setSerialNumber(null);
		DeviceData deviceData = createDeviceData();

		linkedDeviceComponent.addLinkedDeviceData(device, deviceData);

		verify(mockTable, never()).query((QueryConditional) any());
	}

	@Test
	void testAddLinkedDeviceData_withNoLinkedDevice_doesNotUpdate() {
		Device device = createDevice();
		DeviceData deviceData = createDeviceData();
		when(mockTable.query((QueryConditional) any())).thenReturn(mockPageIterable);
		when(mockPageIterable.items()).thenReturn(Collections::emptyIterator);

		linkedDeviceComponent.addLinkedDeviceData(device, deviceData);

		assertEquals(-1, deviceData.getCriticalError());
		assertEquals(-1, deviceData.getInformationalError());
	}

	@Test
	void testAddLinkedDeviceData_withOldLinkedDevice_doesNotUpdate() {
		Device device = createDevice();
		DeviceData deviceData = createDeviceData();
		LinkedDevice linkedDevice = createLinkedDevice();
		linkedDevice.setDate(System.currentTimeMillis() - TimeConstants.HOUR - 1000);
		when(mockTable.query((QueryConditional) any())).thenReturn(mockPageIterable);
		when(mockPageIterable.items())
				.thenReturn(() -> Collections.singletonList(linkedDevice).iterator());

		linkedDeviceComponent.addLinkedDeviceData(device, deviceData);

		assertEquals(-1, deviceData.getCriticalError());
		assertEquals(-1, deviceData.getInformationalError());
	}

	@Test
	void testAddLinkedDeviceData_withRecentLinkedDevice_updatesDeviceData() {
		Device device = createDevice();
		DeviceData deviceData = createDeviceData();
		LinkedDevice linkedDevice = createLinkedDevice();
		linkedDevice.setDate(System.currentTimeMillis());
		linkedDevice.setCriticalAlarm(ISolectriaConstants.Power_Stage_Over_Temperature);
		linkedDevice.setInformativeAlarm(ISolectriaConstants.Fan_Life_Reached);
		when(mockTable.query((QueryConditional) any())).thenReturn(mockPageIterable);
		when(mockPageIterable.items())
				.thenReturn(() -> Collections.singletonList(linkedDevice).iterator());

		linkedDeviceComponent.addLinkedDeviceData(device, deviceData);

		assertEquals(ISolectriaConstants.Power_Stage_Over_Temperature, deviceData.getCriticalError());
		assertEquals(ISolectriaConstants.Fan_Life_Reached, deviceData.getInformationalError());
		assertNotNull(deviceData.getCriticalErrorString());
		assertNotNull(deviceData.getInformationalErrorString());
	}

	private LinkedDevice createLinkedDevice() {
		return new LinkedDevice(
				SERIAL_NUMBER,
				CUSTOMER_ID,
				ISolectriaConstants.NOMINAL,
				ISolectriaConstants.NOMINAL,
				System.currentTimeMillis());
	}

	private Device createDevice() {
		Device device = new Device(DEVICE_ID, CUSTOMER_ID, "Test Device");
		device.setSerialNumber(SERIAL_NUMBER);
		return device;
	}

	private DeviceData createDeviceData() {
		DeviceData data = new DeviceData("site-123", CUSTOMER_ID, DEVICE_ID);
		data.setInformationalError(-1);
		data.setCriticalError(-1);
		return data;
	}
}
