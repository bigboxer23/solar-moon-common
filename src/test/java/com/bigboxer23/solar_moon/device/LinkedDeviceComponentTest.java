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

@ExtendWith(MockitoExtension.class)
public class LinkedDeviceComponentTest {

	@Mock
	private LinkedDeviceRepository mockRepository;

	private LinkedDeviceComponent linkedDeviceComponent;

	private static final String CUSTOMER_ID = "test-customer-123";
	private static final String DEVICE_ID = "device-123";
	private static final String SERIAL_NUMBER = "serial-123";

	private static class TestableLinkedDeviceComponent extends LinkedDeviceComponent {
		private final LinkedDeviceRepository repository;

		public TestableLinkedDeviceComponent(LinkedDeviceRepository repository) {
			this.repository = repository;
		}

		@Override
		protected LinkedDeviceRepository getRepository() {
			return repository;
		}
	}

	@BeforeEach
	void setUp() {
		linkedDeviceComponent = new TestableLinkedDeviceComponent(mockRepository);
	}

	@Test
	void testUpdate_withValidDevice_updatesSuccessfully() {
		LinkedDevice device = createLinkedDevice();
		when(mockRepository.update(device)).thenReturn(Optional.of(device));

		linkedDeviceComponent.update(device);

		verify(mockRepository).update(device);
	}

	@Test
	void testUpdate_withNullDevice_doesNotUpdate() {
		linkedDeviceComponent.update(null);

		verify(mockRepository, never()).update(any());
	}

	@Test
	void testUpdate_withBlankId_doesNotUpdate() {
		LinkedDevice device = createLinkedDevice();
		device.setId("");

		linkedDeviceComponent.update(device);

		verify(mockRepository, never()).update(any());
	}

	@Test
	void testUpdate_withNullId_doesNotUpdate() {
		LinkedDevice device = createLinkedDevice();
		device.setId(null);

		linkedDeviceComponent.update(device);

		verify(mockRepository, never()).update(any());
	}

	@Test
	void testUpdate_withBlankCustomerId_doesNotUpdate() {
		LinkedDevice device = createLinkedDevice();
		device.setCustomerId("");

		linkedDeviceComponent.update(device);

		verify(mockRepository, never()).update(any());
	}

	@Test
	void testUpdate_withNullCustomerId_doesNotUpdate() {
		LinkedDevice device = createLinkedDevice();
		device.setCustomerId(null);

		linkedDeviceComponent.update(device);

		verify(mockRepository, never()).update(any());
	}

	@Test
	void testDelete_withValidParameters_deletesSuccessfully() {
		linkedDeviceComponent.delete(SERIAL_NUMBER, CUSTOMER_ID);

		verify(mockRepository).delete(SERIAL_NUMBER, CUSTOMER_ID);
	}

	@Test
	void testDelete_withBlankSerialNumber_doesNotDelete() {
		linkedDeviceComponent.delete("", CUSTOMER_ID);

		verify(mockRepository).delete("", CUSTOMER_ID);
	}

	@Test
	void testDelete_withNullSerialNumber_doesNotDelete() {
		linkedDeviceComponent.delete(null, CUSTOMER_ID);

		verify(mockRepository).delete(null, CUSTOMER_ID);
	}

	@Test
	void testDelete_withBlankCustomerId_doesNotDelete() {
		linkedDeviceComponent.delete(SERIAL_NUMBER, "");

		verify(mockRepository).delete(SERIAL_NUMBER, "");
	}

	@Test
	void testDelete_withNullCustomerId_doesNotDelete() {
		linkedDeviceComponent.delete(SERIAL_NUMBER, null);

		verify(mockRepository).delete(SERIAL_NUMBER, null);
	}

	@Test
	void testDeleteByCustomerId_withValidCustomerId_deletesSuccessfully() {
		linkedDeviceComponent.deleteByCustomerId(CUSTOMER_ID);

		verify(mockRepository).deleteByCustomerId(CUSTOMER_ID);
	}

	@Test
	void testDeleteByCustomerId_withNullCustomerId_callsRepository() {
		linkedDeviceComponent.deleteByCustomerId(null);

		verify(mockRepository).deleteByCustomerId(null);
	}

	@Test
	void testDeleteByCustomerId_withBlankCustomerId_callsRepository() {
		linkedDeviceComponent.deleteByCustomerId("");

		verify(mockRepository).deleteByCustomerId("");
	}

	@Test
	void testQueryBySerialNumber_withValidParameters_returnsDevice() {
		LinkedDevice expectedDevice = createLinkedDevice();
		when(mockRepository.findBySerialNumber(SERIAL_NUMBER, CUSTOMER_ID)).thenReturn(Optional.of(expectedDevice));

		Optional<LinkedDevice> result = linkedDeviceComponent.queryBySerialNumber(SERIAL_NUMBER, CUSTOMER_ID);

		assertTrue(result.isPresent());
		assertEquals(expectedDevice, result.get());
		verify(mockRepository).findBySerialNumber(SERIAL_NUMBER, CUSTOMER_ID);
	}

	@Test
	void testQueryBySerialNumber_withBlankSerialNumber_returnsEmpty() {
		when(mockRepository.findBySerialNumber("", CUSTOMER_ID)).thenReturn(Optional.empty());

		Optional<LinkedDevice> result = linkedDeviceComponent.queryBySerialNumber("", CUSTOMER_ID);

		assertFalse(result.isPresent());
		verify(mockRepository).findBySerialNumber("", CUSTOMER_ID);
	}

	@Test
	void testQueryBySerialNumber_withNullSerialNumber_returnsEmpty() {
		when(mockRepository.findBySerialNumber(null, CUSTOMER_ID)).thenReturn(Optional.empty());

		Optional<LinkedDevice> result = linkedDeviceComponent.queryBySerialNumber(null, CUSTOMER_ID);

		assertFalse(result.isPresent());
		verify(mockRepository).findBySerialNumber(null, CUSTOMER_ID);
	}

	@Test
	void testQueryBySerialNumber_withBlankCustomerId_returnsEmpty() {
		when(mockRepository.findBySerialNumber(SERIAL_NUMBER, "")).thenReturn(Optional.empty());

		Optional<LinkedDevice> result = linkedDeviceComponent.queryBySerialNumber(SERIAL_NUMBER, "");

		assertFalse(result.isPresent());
		verify(mockRepository).findBySerialNumber(SERIAL_NUMBER, "");
	}

	@Test
	void testQueryBySerialNumber_withNullCustomerId_returnsEmpty() {
		when(mockRepository.findBySerialNumber(SERIAL_NUMBER, null)).thenReturn(Optional.empty());

		Optional<LinkedDevice> result = linkedDeviceComponent.queryBySerialNumber(SERIAL_NUMBER, null);

		assertFalse(result.isPresent());
		verify(mockRepository).findBySerialNumber(SERIAL_NUMBER, null);
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

		verify(mockRepository, never()).findBySerialNumber(any(), any());
	}

	@Test
	void testAddLinkedDeviceData_withNullDeviceData_doesNotUpdate() {
		Device device = createDevice();

		linkedDeviceComponent.addLinkedDeviceData(device, null);

		verify(mockRepository, never()).findBySerialNumber(any(), any());
	}

	@Test
	void testAddLinkedDeviceData_withBlankSerialNumber_doesNotUpdate() {
		Device device = createDevice();
		device.setSerialNumber("");
		DeviceData deviceData = createDeviceData();

		linkedDeviceComponent.addLinkedDeviceData(device, deviceData);

		verify(mockRepository, never()).findBySerialNumber(any(), any());
	}

	@Test
	void testAddLinkedDeviceData_withNullSerialNumber_doesNotUpdate() {
		Device device = createDevice();
		device.setSerialNumber(null);
		DeviceData deviceData = createDeviceData();

		linkedDeviceComponent.addLinkedDeviceData(device, deviceData);

		verify(mockRepository, never()).findBySerialNumber(any(), any());
	}

	@Test
	void testAddLinkedDeviceData_withNoLinkedDevice_doesNotUpdate() {
		Device device = createDevice();
		DeviceData deviceData = createDeviceData();
		when(mockRepository.findBySerialNumber(SERIAL_NUMBER, CUSTOMER_ID)).thenReturn(Optional.empty());

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
		when(mockRepository.findBySerialNumber(SERIAL_NUMBER, CUSTOMER_ID)).thenReturn(Optional.of(linkedDevice));

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
		when(mockRepository.findBySerialNumber(SERIAL_NUMBER, CUSTOMER_ID)).thenReturn(Optional.of(linkedDevice));

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
