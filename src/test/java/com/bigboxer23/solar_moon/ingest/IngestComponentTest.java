package com.bigboxer23.solar_moon.ingest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.alarm.AlarmComponent;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.device.DeviceComponent;
import com.bigboxer23.solar_moon.device.LinkedDeviceComponent;
import com.bigboxer23.solar_moon.device.VirtualDeviceComponent;
import com.bigboxer23.solar_moon.location.LocationComponent;
import com.bigboxer23.solar_moon.search.OpenSearchComponent;
import com.bigboxer23.solar_moon.weather.PirateWeatherComponent;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IngestComponentTest {

	private IngestComponent component;
	private DeviceComponent mockDeviceComponent;
	private DeviceComponent originalDeviceComponent;
	private OpenSearchComponent mockOSComponent;
	private OpenSearchComponent originalOSComponent;
	private LocationComponent mockLocationComponent;
	private LocationComponent originalLocationComponent;
	private PirateWeatherComponent mockWeatherComponent;
	private PirateWeatherComponent originalWeatherComponent;
	private AlarmComponent mockAlarmComponent;
	private AlarmComponent originalAlarmComponent;
	private LinkedDeviceComponent mockLinkedDeviceComponent;
	private LinkedDeviceComponent originalLinkedDeviceComponent;
	private VirtualDeviceComponent mockVirtualDeviceComponent;
	private VirtualDeviceComponent originalVirtualDeviceComponent;

	@BeforeEach
	void setUp() throws Exception {
		originalDeviceComponent = IComponentRegistry.deviceComponent;
		originalOSComponent = IComponentRegistry.OSComponent;
		originalLocationComponent = IComponentRegistry.locationComponent;
		originalWeatherComponent = IComponentRegistry.weatherComponent;
		originalAlarmComponent = IComponentRegistry.alarmComponent;
		originalLinkedDeviceComponent = IComponentRegistry.linkedDeviceComponent;
		originalVirtualDeviceComponent = IComponentRegistry.virtualDeviceComponent;

		mockDeviceComponent = mock(DeviceComponent.class);
		mockOSComponent = mock(OpenSearchComponent.class);
		mockLocationComponent = mock(LocationComponent.class);
		mockWeatherComponent = mock(PirateWeatherComponent.class);
		mockAlarmComponent = mock(AlarmComponent.class);
		mockLinkedDeviceComponent = mock(LinkedDeviceComponent.class);
		mockVirtualDeviceComponent = mock(VirtualDeviceComponent.class);

		setFinalStatic(IComponentRegistry.class.getField("deviceComponent"), mockDeviceComponent);
		setFinalStatic(IComponentRegistry.class.getField("OSComponent"), mockOSComponent);
		setFinalStatic(IComponentRegistry.class.getField("locationComponent"), mockLocationComponent);
		setFinalStatic(IComponentRegistry.class.getField("weatherComponent"), mockWeatherComponent);
		setFinalStatic(IComponentRegistry.class.getField("alarmComponent"), mockAlarmComponent);
		setFinalStatic(IComponentRegistry.class.getField("linkedDeviceComponent"), mockLinkedDeviceComponent);
		setFinalStatic(IComponentRegistry.class.getField("virtualDeviceComponent"), mockVirtualDeviceComponent);

		component = new IngestComponent();
	}

	@AfterEach
	void tearDown() throws Exception {
		setFinalStatic(IComponentRegistry.class.getField("deviceComponent"), originalDeviceComponent);
		setFinalStatic(IComponentRegistry.class.getField("OSComponent"), originalOSComponent);
		setFinalStatic(IComponentRegistry.class.getField("locationComponent"), originalLocationComponent);
		setFinalStatic(IComponentRegistry.class.getField("weatherComponent"), originalWeatherComponent);
		setFinalStatic(IComponentRegistry.class.getField("alarmComponent"), originalAlarmComponent);
		setFinalStatic(IComponentRegistry.class.getField("linkedDeviceComponent"), originalLinkedDeviceComponent);
		setFinalStatic(IComponentRegistry.class.getField("virtualDeviceComponent"), originalVirtualDeviceComponent);
	}

	private void setFinalStatic(Field field, Object newValue) throws Exception {
		field.setAccessible(true);

		java.lang.reflect.Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
		unsafeField.setAccessible(true);
		sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);

		Object staticFieldBase = unsafe.staticFieldBase(field);
		long staticFieldOffset = unsafe.staticFieldOffset(field);
		unsafe.putObject(staticFieldBase, staticFieldOffset, newValue);
	}

	@Test
	void testHandleDevice_nullDeviceData() throws Exception {
		Device device = createTestDevice();

		DeviceData result = component.handleDevice(device, null);

		assertNull(result);
		verify(mockOSComponent, never()).logData(any(), any());
	}

	@Test
	void testHandleDevice_nullDevice() throws Exception {
		DeviceData deviceData = createTestDeviceData();

		DeviceData result = component.handleDevice(null, deviceData);

		assertNull(result);
		verify(mockOSComponent, never()).logData(any(), any());
	}

	@Test
	void testHandleDevice_validDeviceAndData() throws Exception {
		Device device = createTestDevice();
		device.setSiteId("site-123");
		DeviceData deviceData = createTestDeviceData();
		deviceData.setTotalEnergyConsumed(1000f);

		Device site = createTestDevice();
		site.setId("site-123");
		when(mockDeviceComponent.findDeviceById("site-123", "customer-1")).thenReturn(Optional.of(site));
		when(mockOSComponent.getTotalEnergyConsumed("device-1")).thenReturn(900f);

		DeviceData result = component.handleDevice(device, deviceData);

		assertNotNull(result);
		assertEquals(100f, result.getEnergyConsumed());
		verify(mockLocationComponent).addLocationData(eq(deviceData), eq(site));
		verify(mockWeatherComponent).addWeatherData(eq(deviceData), eq(site));
		verify(mockAlarmComponent).resolveActiveAlarms(deviceData);
		verify(mockLinkedDeviceComponent).addLinkedDeviceData(device, deviceData);
		verify(mockOSComponent).logData(any(Date.class), eq(Collections.singletonList(deviceData)));
		verify(mockVirtualDeviceComponent).handleVirtualDevice(deviceData);
	}

	@Test
	void testHandleDevice_deviceIsSite() throws Exception {
		Device device = createTestDevice();
		device.setIsSite("1");
		device.setSiteId(device.getId());
		DeviceData deviceData = createTestDeviceData();
		deviceData.setTotalEnergyConsumed(1000f);

		when(mockDeviceComponent.findDeviceById(device.getId(), "customer-1")).thenReturn(Optional.of(device));
		when(mockOSComponent.getTotalEnergyConsumed("device-1")).thenReturn(null);

		DeviceData result = component.handleDevice(device, deviceData);

		assertNotNull(result);
		assertTrue(result.isSite());
	}

	@Test
	void testMaybeCorrectForRollover_noRollover() {
		float result = component.maybeCorrectForRollover(100f, 200f);
		assertEquals(200f, result);
	}

	@Test
	void testMaybeCorrectForRollover_withRollover() {
		float prev = 9999500f;
		float newTotal = 100f;

		float result = component.maybeCorrectForRollover(prev, newTotal);

		assertEquals(10000100f, result);
	}

	@Test
	void testMaybeCorrectForRollover_justBelowRollover() {
		float prev = 9998000f;
		float newTotal = 9999000f;

		float result = component.maybeCorrectForRollover(prev, newTotal);

		assertEquals(9999000f, result);
	}

	@Test
	void testMaybeCorrectForRollover_newTotalAboveMargin() {
		float prev = 9999500f;
		float newTotal = 2000f;

		float result = component.maybeCorrectForRollover(prev, newTotal);

		assertEquals(2000f, result);
	}

	@Test
	void testFindDeviceFromDeviceName_exactMatch() {
		String customerId = "customer-1";
		String deviceName = "TestDevice";
		Device device = createTestDevice();
		device.setDeviceName(deviceName);

		when(mockDeviceComponent.findDeviceByDeviceName(customerId, deviceName)).thenReturn(Optional.of(device));

		Device result = component.findDeviceFromDeviceName(customerId, deviceName);

		assertNotNull(result);
		assertEquals(deviceName, result.getDeviceName());
		verify(mockDeviceComponent).findDeviceByDeviceName(customerId, deviceName);
	}

	@Test
	void testFindDeviceFromDeviceName_nullCustomerId() {
		Device result = component.findDeviceFromDeviceName(null, "deviceName");

		assertNull(result);
		verify(mockDeviceComponent, never()).findDeviceByDeviceName(any(), any());
	}

	@Test
	void testFindDeviceFromDeviceName_emptyDeviceName() {
		Device result = component.findDeviceFromDeviceName("customer-1", "");

		assertNull(result);
		verify(mockDeviceComponent, never()).findDeviceByDeviceName(any(), any());
	}

	@Test
	void testFindDeviceFromDeviceName_createNewDevice() {
		String customerId = "customer-1";
		String deviceName = "NewDevice";
		Device newDevice = createTestDevice();
		newDevice.setDeviceName(deviceName);

		when(mockDeviceComponent.findDeviceByDeviceName(customerId, deviceName)).thenReturn(Optional.empty());
		when(mockDeviceComponent.addDevice(any(Device.class))).thenReturn(newDevice);

		Device result = component.findDeviceFromDeviceName(customerId, deviceName);

		assertNotNull(result);
		verify(mockDeviceComponent).addDevice(any(Device.class));
	}

	@Test
	void testFindDeviceFromDeviceNameFuzzy_exactMatch() {
		String customerId = "customer-1";
		String deviceName = "TestDevice";
		Device device = createTestDevice();
		device.setDeviceName(deviceName);

		when(mockDeviceComponent.findDeviceByDeviceName(customerId, deviceName)).thenReturn(Optional.of(device));

		Device result = component.findDeviceFromDeviceNameFuzzy(customerId, deviceName);

		assertNotNull(result);
		assertEquals(deviceName, result.getDeviceName());
	}

	@Test
	void testFindDeviceFromDeviceNameFuzzy_fuzzyMatch() {
		String customerId = "customer-1";
		String deviceName = "TestDevice";
		String fuzzyDeviceName = "TestDeviceOld";
		Device device = createTestDevice();
		device.setDeviceName(fuzzyDeviceName);
		device.setDisabled(false);

		when(mockDeviceComponent.findDeviceByDeviceName(customerId, deviceName)).thenReturn(Optional.empty());
		when(mockDeviceComponent.getDevicesForCustomerId(customerId)).thenReturn(List.of(device));
		when(mockDeviceComponent.updateDevice(any(Device.class))).thenReturn(Optional.of(device));

		Device result = component.findDeviceFromDeviceNameFuzzy(customerId, deviceName);

		assertNotNull(result);
		assertEquals(deviceName, result.getDeviceName());
		verify(mockDeviceComponent).updateDevice(any(Device.class));
	}

	@Test
	void testFindDeviceFromDeviceNameFuzzy_noFuzzyMatch() {
		String customerId = "customer-1";
		String deviceName = "TestDevice";
		Device otherDevice = createTestDevice();
		otherDevice.setDeviceName("OtherDevice");
		otherDevice.setDisabled(false);

		Device newDevice = createTestDevice();
		newDevice.setDeviceName(deviceName);

		when(mockDeviceComponent.findDeviceByDeviceName(customerId, deviceName)).thenReturn(Optional.empty());
		when(mockDeviceComponent.getDevicesForCustomerId(customerId)).thenReturn(List.of(otherDevice));
		when(mockDeviceComponent.addDevice(any(Device.class))).thenReturn(newDevice);

		Device result = component.findDeviceFromDeviceNameFuzzy(customerId, deviceName);

		assertNotNull(result);
		verify(mockDeviceComponent).addDevice(any(Device.class));
	}

	@Test
	void testFindDeviceFromDeviceNameFuzzy_skipsDisabledDevices() {
		String customerId = "customer-1";
		String deviceName = "TestDevice";
		Device disabledDevice = createTestDevice();
		disabledDevice.setDeviceName("TestDeviceOld");
		disabledDevice.setDisabled(true);

		Device newDevice = createTestDevice();
		newDevice.setDeviceName(deviceName);

		when(mockDeviceComponent.findDeviceByDeviceName(customerId, deviceName)).thenReturn(Optional.empty());
		when(mockDeviceComponent.getDevicesForCustomerId(customerId)).thenReturn(List.of(disabledDevice));
		when(mockDeviceComponent.addDevice(any(Device.class))).thenReturn(newDevice);

		Device result = component.findDeviceFromDeviceNameFuzzy(customerId, deviceName);

		assertNotNull(result);
		verify(mockDeviceComponent).addDevice(any(Device.class));
		verify(mockDeviceComponent, never()).updateDevice(any(Device.class));
	}

	@Test
	void testHandleDevice_calculatesEnergyConsumed() throws Exception {
		Device device = createTestDevice();
		DeviceData deviceData = createTestDeviceData();
		deviceData.setTotalEnergyConsumed(1500f);

		when(mockDeviceComponent.findDeviceById(device.getSiteId(), "customer-1"))
				.thenReturn(Optional.empty());
		when(mockOSComponent.getTotalEnergyConsumed("device-1")).thenReturn(1000f);

		DeviceData result = component.handleDevice(device, deviceData);

		assertNotNull(result);
		assertEquals(500f, result.getEnergyConsumed());
	}

	@Test
	void testHandleDevice_zerosBadEnergyValues() throws Exception {
		Device device = createTestDevice();
		DeviceData deviceData = createTestDeviceData();
		deviceData.setTotalEnergyConsumed(1000f);

		when(mockDeviceComponent.findDeviceById(device.getSiteId(), "customer-1"))
				.thenReturn(Optional.empty());
		when(mockOSComponent.getTotalEnergyConsumed("device-1")).thenReturn(2000f);

		DeviceData result = component.handleDevice(device, deviceData);

		assertNotNull(result);
		assertEquals(0f, result.getEnergyConsumed());
	}

	@Test
	void testHandleDevice_zerosExcessiveEnergyValues() throws Exception {
		Device device = createTestDevice();
		DeviceData deviceData = createTestDeviceData();
		deviceData.setTotalEnergyConsumed(2000f);

		when(mockDeviceComponent.findDeviceById(device.getSiteId(), "customer-1"))
				.thenReturn(Optional.empty());
		when(mockOSComponent.getTotalEnergyConsumed("device-1")).thenReturn(100f);

		DeviceData result = component.handleDevice(device, deviceData);

		assertNotNull(result);
		assertEquals(0f, result.getEnergyConsumed());
	}

	@Test
	void testHandleDevice_noPreviousEnergyConsumed() throws Exception {
		Device device = createTestDevice();
		DeviceData deviceData = createTestDeviceData();
		deviceData.setTotalEnergyConsumed(1000f);

		when(mockDeviceComponent.findDeviceById(device.getSiteId(), "customer-1"))
				.thenReturn(Optional.empty());
		when(mockOSComponent.getTotalEnergyConsumed("device-1")).thenReturn(null);

		DeviceData result = component.handleDevice(device, deviceData);

		assertNotNull(result);
		assertEquals(0f, result.getEnergyConsumed());
	}

	private Device createTestDevice() {
		Device device = new Device();
		device.setId("device-1");
		device.setClientId("customer-1");
		device.setDeviceName("TestDevice");
		device.setSiteId("site-1");
		device.setSite("Test Site");
		return device;
	}

	private DeviceData createTestDeviceData() {
		DeviceData deviceData = new DeviceData();
		deviceData.setDeviceId("device-1");
		deviceData.setCustomerId("customer-1");
		deviceData.setSiteId("site-1");
		deviceData.setDate(new Date());
		return deviceData;
	}
}
