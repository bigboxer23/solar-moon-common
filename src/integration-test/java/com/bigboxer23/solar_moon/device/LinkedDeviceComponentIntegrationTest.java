package com.bigboxer23.solar_moon.device;

import static com.bigboxer23.solar_moon.TestConstants.CUSTOMER_ID;
import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.TestUtils;
import com.bigboxer23.solar_moon.alarm.ISolectriaConstants;
import com.bigboxer23.solar_moon.alarm.SolectriaErrorOracle;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.data.LinkedDevice;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.*;

/** */
public class LinkedDeviceComponentIntegrationTest implements IComponentRegistry, ISolectriaConstants {

	@BeforeAll
	public static void before() {
		TestUtils.setupSite();
	}

	@BeforeEach
	public void beforeEach() {
		assertFalse(linkedDeviceComponent
				.queryBySerialNumber(TestUtils.getDevice().getSerialNumber(), CUSTOMER_ID)
				.isPresent());
		LinkedDevice linkedDevice = new LinkedDevice(
				TestUtils.getDevice().getSerialNumber(),
				CUSTOMER_ID,
				ISolectriaConstants.NOMINAL,
				ISolectriaConstants.NOMINAL,
				System.currentTimeMillis());
		linkedDeviceComponent.update(linkedDevice);
	}

	@AfterAll
	public static void afterAll() {
		TestUtils.nukeCustomerId(CUSTOMER_ID);
	}

	@AfterEach
	public void afterEach() {
		linkedDeviceComponent.delete(TestUtils.getDevice().getSerialNumber(), CUSTOMER_ID);
	}

	@Test
	public void addLinkedDeviceDataVirtual() {
		assertNull(linkedDeviceComponent.addLinkedDeviceDataVirtual(null, null));
		assertNull(linkedDeviceComponent.addLinkedDeviceDataVirtual(null, Collections.emptyList()));
		DeviceData virtualData = new DeviceData();
		assertNotNull(linkedDeviceComponent.addLinkedDeviceDataVirtual(virtualData, Collections.emptyList()));
		assertNotNull(linkedDeviceComponent.addLinkedDeviceDataVirtual(virtualData, null));
		assertEquals(
				-1,
				linkedDeviceComponent
						.addLinkedDeviceDataVirtual(virtualData, Collections.emptyList())
						.getInformationalError());
		List<DeviceData> childDevices = new ArrayList<>();
		DeviceData childData = new DeviceData();
		childData.setInformationalError(NOMINAL);
		childDevices.add(childData);
		assertEquals(
				NOMINAL,
				linkedDeviceComponent
						.addLinkedDeviceDataVirtual(virtualData, childDevices)
						.getInformationalError());
		childData = new DeviceData();
		childData.setInformationalError(DC_Voltage_High);
		childDevices.add(childData);
		assertEquals(
				DC_Voltage_High,
				linkedDeviceComponent
						.addLinkedDeviceDataVirtual(virtualData, childDevices)
						.getInformationalError());
		childDevices.clear();
		childDevices.add(childData);
		assertEquals(
				DC_Voltage_High,
				linkedDeviceComponent
						.addLinkedDeviceDataVirtual(virtualData, childDevices)
						.getInformationalError());
		childData = new DeviceData();
		childData.setInformationalError(Fan_Life_Reached);
		childDevices.add(childData);
		assertEquals(
				DC_Voltage_High + Fan_Life_Reached,
				linkedDeviceComponent
						.addLinkedDeviceDataVirtual(virtualData, childDevices)
						.getInformationalError());
		childData = new DeviceData();
		childData.setInformationalError(UL_Islanding_Fault);
		childDevices.add(childData);
		assertEquals(
				DC_Voltage_High + Fan_Life_Reached + UL_Islanding_Fault,
				linkedDeviceComponent
						.addLinkedDeviceDataVirtual(virtualData, childDevices)
						.getInformationalError());
		String errorString = linkedDeviceComponent
				.addLinkedDeviceDataVirtual(virtualData, childDevices)
				.getInformationalErrorString();
		assertNotNull(errorString);
		assertTrue(errorString.contains(SolectriaErrorOracle.translateError(DC_Voltage_High, false)));
		assertTrue(errorString.contains(SolectriaErrorOracle.translateError(Fan_Life_Reached, false)));
		assertTrue(errorString.contains(SolectriaErrorOracle.translateError(UL_Islanding_Fault, false)));
	}

	@Test
	public void queryBySerialNumber() {
		assertFalse(linkedDeviceComponent.queryBySerialNumber(null, null).isPresent());
		assertFalse(linkedDeviceComponent.queryBySerialNumber(null, CUSTOMER_ID).isPresent());
		assertFalse(linkedDeviceComponent
				.queryBySerialNumber("not valid", CUSTOMER_ID)
				.isPresent());
		Optional<LinkedDevice> dbLinkedDevice =
				linkedDeviceComponent.queryBySerialNumber(TestUtils.getDevice().getSerialNumber(), CUSTOMER_ID);
		assertTrue(dbLinkedDevice.isPresent());
		assertNotEquals(0, dbLinkedDevice.get().getDate());
	}

	@Test
	public void update() {
		Optional<LinkedDevice> dbLinkedDevice =
				linkedDeviceComponent.queryBySerialNumber(TestUtils.getDevice().getSerialNumber(), CUSTOMER_ID);
		assertTrue(dbLinkedDevice.isPresent());
		assertNotEquals(0, dbLinkedDevice.get().getDate());
		dbLinkedDevice.get().setCriticalAlarm(ISolectriaConstants.AC_Contactor_Opened);
		dbLinkedDevice.get().setDate(1);
		linkedDeviceComponent.update(dbLinkedDevice.get());
		dbLinkedDevice =
				linkedDeviceComponent.queryBySerialNumber(TestUtils.getDevice().getSerialNumber(), CUSTOMER_ID);
		assertTrue(dbLinkedDevice.isPresent());
		assertEquals(1, dbLinkedDevice.get().getDate());
		assertEquals(
				ISolectriaConstants.AC_Contactor_Opened, dbLinkedDevice.get().getCriticalAlarm());
	}

	@Test
	public void delete() {
		assertTrue(linkedDeviceComponent
				.queryBySerialNumber(TestUtils.getDevice().getSerialNumber(), CUSTOMER_ID)
				.isPresent());
		linkedDeviceComponent.delete(TestUtils.getDevice().getSerialNumber(), null);
		assertTrue(linkedDeviceComponent
				.queryBySerialNumber(TestUtils.getDevice().getSerialNumber(), CUSTOMER_ID)
				.isPresent());
		linkedDeviceComponent.delete(TestUtils.getDevice().getSerialNumber(), "");
		assertTrue(linkedDeviceComponent
				.queryBySerialNumber(TestUtils.getDevice().getSerialNumber(), CUSTOMER_ID)
				.isPresent());
		linkedDeviceComponent.delete(TestUtils.getDevice().getSerialNumber(), "asdf");
		assertTrue(linkedDeviceComponent
				.queryBySerialNumber(TestUtils.getDevice().getSerialNumber(), CUSTOMER_ID)
				.isPresent());
		linkedDeviceComponent.delete(null, CUSTOMER_ID);
		assertTrue(linkedDeviceComponent
				.queryBySerialNumber(TestUtils.getDevice().getSerialNumber(), CUSTOMER_ID)
				.isPresent());
		linkedDeviceComponent.delete("", CUSTOMER_ID);
		assertTrue(linkedDeviceComponent
				.queryBySerialNumber(TestUtils.getDevice().getSerialNumber(), CUSTOMER_ID)
				.isPresent());
		linkedDeviceComponent.delete("asdf", CUSTOMER_ID);
		assertTrue(linkedDeviceComponent
				.queryBySerialNumber(TestUtils.getDevice().getSerialNumber(), CUSTOMER_ID)
				.isPresent());
		linkedDeviceComponent.delete(TestUtils.getDevice().getSerialNumber(), CUSTOMER_ID);
		assertFalse(linkedDeviceComponent
				.queryBySerialNumber(TestUtils.getDevice().getSerialNumber(), CUSTOMER_ID)
				.isPresent());
	}

	@Test
	public void deleteByCustomerId() {
		Device altDevice = deviceComponent.getDevicesForCustomerId(CUSTOMER_ID).stream()
				.filter(d -> d.getId().equals(TestUtils.getDevice().getId()))
				.findAny()
				.orElse(null);
		assertNotNull(altDevice);
		altDevice.setSerialNumber("xxxx");
		deviceComponent.updateDevice(altDevice);

		LinkedDevice altLinkedDevice = new LinkedDevice(
				altDevice.getSerialNumber(),
				CUSTOMER_ID,
				ISolectriaConstants.NOMINAL,
				ISolectriaConstants.NOMINAL,
				System.currentTimeMillis());
		linkedDeviceComponent.update(altLinkedDevice);

		assertTrue(linkedDeviceComponent
				.queryBySerialNumber(TestUtils.getDevice().getSerialNumber(), CUSTOMER_ID)
				.isPresent());
		assertTrue(linkedDeviceComponent
				.queryBySerialNumber(altDevice.getSerialNumber(), CUSTOMER_ID)
				.isPresent());
		linkedDeviceComponent.deleteByCustomerId(CUSTOMER_ID);
		assertFalse(linkedDeviceComponent
				.queryBySerialNumber(TestUtils.getDevice().getSerialNumber(), CUSTOMER_ID)
				.isPresent());
		assertFalse(linkedDeviceComponent
				.queryBySerialNumber(altDevice.getSerialNumber(), CUSTOMER_ID)
				.isPresent());
	}
}
