package com.bigboxer23.solar_moon.device;

import static com.bigboxer23.solar_moon.TestConstants.CUSTOMER_ID;
import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.TestUtils;
import com.bigboxer23.solar_moon.alarm.ISolectriaConstants;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.LinkedDevice;
import java.util.Optional;
import org.junit.jupiter.api.*;

/** */
public class TestLinkedDeviceComponent implements IComponentRegistry {

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
		assertEquals("1234", dbLinkedDevice.get().getCriticalAlarm());
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
