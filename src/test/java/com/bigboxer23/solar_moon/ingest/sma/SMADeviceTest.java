package com.bigboxer23.solar_moon.ingest.sma;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.ingest.IngestComponent;
import java.lang.reflect.Field;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SMADeviceTest {

	private IngestComponent mockGenerationComponent;
	private IngestComponent originalGenerationComponent;

	@BeforeEach
	void setUp() throws Exception {
		originalGenerationComponent = IComponentRegistry.generationComponent;
		mockGenerationComponent = mock(IngestComponent.class);
		setFinalStatic(IComponentRegistry.class.getField("generationComponent"), mockGenerationComponent);
	}

	@AfterEach
	void tearDown() throws Exception {
		setFinalStatic(IComponentRegistry.class.getField("generationComponent"), originalGenerationComponent);
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
	void testAddRecord_firstRecord() {
		SMADevice smaDevice = new SMADevice("customer-1");
		Device device = createTestDevice();

		SMARecord record = createTestRecord();

		when(mockGenerationComponent.findDeviceFromDeviceNameFuzzy("customer-1", "TestDevice"))
				.thenReturn(device);

		smaDevice.addRecord(record);

		assertEquals("TestDevice", smaDevice.getDeviceName());
		assertNotNull(smaDevice.getDevice());
		assertEquals(device.getId(), smaDevice.getDevice().getId());
		assertEquals(1, smaDevice.getRecords().size());
		assertNotNull(smaDevice.getTimestamp());
	}

	@Test
	void testAddRecord_subsequentRecord() {
		SMADevice smaDevice = new SMADevice("customer-1");
		Device device = createTestDevice();

		SMARecord record1 = createTestRecord();
		SMARecord record2 = createTestRecord();
		record2.setAttributeName("Power");

		when(mockGenerationComponent.findDeviceFromDeviceNameFuzzy("customer-1", "TestDevice"))
				.thenReturn(device);

		smaDevice.addRecord(record1);
		smaDevice.addRecord(record2);

		assertEquals(2, smaDevice.getRecords().size());
		verify(mockGenerationComponent, times(1)).findDeviceFromDeviceNameFuzzy(anyString(), anyString());
	}

	@Test
	void testAddRecord_noDeviceCreated() {
		SMADevice smaDevice = new SMADevice("customer-1");

		SMARecord record = createTestRecord();

		when(mockGenerationComponent.findDeviceFromDeviceNameFuzzy("customer-1", "TestDevice"))
				.thenReturn(null);

		smaDevice.addRecord(record);

		assertEquals("TestDevice", smaDevice.getDeviceName());
		assertNull(smaDevice.getDevice());
		assertEquals(0, smaDevice.getRecords().size());
	}

	@Test
	void testAddRecord_withTimestamp() {
		SMADevice smaDevice = new SMADevice("customer-1");
		Device device = createTestDevice();

		SMARecord record = createTestRecord();
		record.setTimestamp("2025-01-15T10:30:00");

		when(mockGenerationComponent.findDeviceFromDeviceNameFuzzy("customer-1", "TestDevice"))
				.thenReturn(device);

		smaDevice.addRecord(record);

		assertNotNull(smaDevice.getTimestamp());
	}

	@Test
	void testAddRecord_withEmptyTimestamp() {
		SMADevice smaDevice = new SMADevice("customer-1");
		Device device = createTestDevice();

		SMARecord record = createTestRecord();
		record.setTimestamp("");

		when(mockGenerationComponent.findDeviceFromDeviceNameFuzzy("customer-1", "TestDevice"))
				.thenReturn(device);

		smaDevice.addRecord(record);

		assertNull(smaDevice.getTimestamp());
	}

	@Test
	void testAddRecord_withNullTimestamp() {
		SMADevice smaDevice = new SMADevice("customer-1");
		Device device = createTestDevice();

		SMARecord record = createTestRecord();
		record.setTimestamp(null);

		when(mockGenerationComponent.findDeviceFromDeviceNameFuzzy("customer-1", "TestDevice"))
				.thenReturn(device);

		smaDevice.addRecord(record);

		assertNull(smaDevice.getTimestamp());
	}

	@Test
	void testAddRecord_invalidTimestampFormat() {
		SMADevice smaDevice = new SMADevice("customer-1");
		Device device = createTestDevice();

		SMARecord record = createTestRecord();
		record.setTimestamp("invalid-timestamp");

		when(mockGenerationComponent.findDeviceFromDeviceNameFuzzy("customer-1", "TestDevice"))
				.thenReturn(device);

		smaDevice.addRecord(record);

		assertNull(smaDevice.getTimestamp());
	}

	@Test
	void testConstructor_setsCustomerId() {
		SMADevice smaDevice = new SMADevice("customer-1");

		assertEquals("customer-1", smaDevice.getCustomerId());
		assertNotNull(smaDevice.getRecords());
		assertTrue(smaDevice.getRecords().isEmpty());
	}

	@Test
	void testAddRecord_multipleRecordsFromSameDevice() {
		SMADevice smaDevice = new SMADevice("customer-1");
		Device device = createTestDevice();

		when(mockGenerationComponent.findDeviceFromDeviceNameFuzzy("customer-1", "TestDevice"))
				.thenReturn(device);

		for (int i = 0; i < 5; i++) {
			SMARecord record = createTestRecord();
			record.setAttributeName("Attribute" + i);
			smaDevice.addRecord(record);
		}

		assertEquals(5, smaDevice.getRecords().size());
		verify(mockGenerationComponent, times(1)).findDeviceFromDeviceNameFuzzy("customer-1", "TestDevice");
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

	private SMARecord createTestRecord() {
		SMARecord record = new SMARecord();
		record.setDevice("TestDevice");
		record.setAttributeName("Total yield");
		record.setTimestamp("2025-01-15T10:30:00");
		record.setValue("1000");
		return record;
	}
}
