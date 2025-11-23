package com.bigboxer23.solar_moon.ingest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.alarm.AlarmComponent;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.data.LinkedDevice;
import com.bigboxer23.solar_moon.device.DeviceComponent;
import com.bigboxer23.solar_moon.device.LinkedDeviceComponent;
import com.bigboxer23.solar_moon.mapping.MappingComponent;
import com.bigboxer23.solar_moon.search.OpenSearchComponent;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import javax.xml.xpath.XPathExpressionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ObviusIngestComponentTest {

	private ObviusIngestComponent component;
	private IngestComponent mockGenerationComponent;
	private IngestComponent originalGenerationComponent;
	private DeviceComponent mockDeviceComponent;
	private DeviceComponent originalDeviceComponent;
	private AlarmComponent mockAlarmComponent;
	private AlarmComponent originalAlarmComponent;
	private LinkedDeviceComponent mockLinkedDeviceComponent;
	private LinkedDeviceComponent originalLinkedDeviceComponent;
	private OpenSearchComponent mockOSComponent;
	private OpenSearchComponent originalOSComponent;
	private MappingComponent mockMappingComponent;
	private MappingComponent originalMappingComponent;

	private static final String VALID_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
			+ "<DAS>"
			+ "<mode>STREAM</mode>"
			+ "<serial>ABC123456</serial>"
			+ "<devices>"
			+ "<device>"
			+ "<name>TestDevice</name>"
			+ "<records>"
			+ "<record>"
			+ "<time zone=\"EST\">2025-01-15 10:30:00</time>"
			+ "<error text=\"Ok\">0</error>"
			+ "<point name=\"Total Energy Consumption\" value=\"1000.5\"/>"
			+ "<point name=\"Total Real Power\" value=\"500.2\"/>"
			+ "<point name=\"Average Current\" value=\"10.5\"/>"
			+ "<point name=\"Average Voltage (L-N)\" value=\"240.0\"/>"
			+ "<point name=\"Total (System) Power Factor\" value=\"95.0\"/>"
			+ "</record>"
			+ "</records>"
			+ "</device>"
			+ "</devices>"
			+ "</DAS>";

	private static final String LINKED_DEVICE_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
			+ "<DAS>"
			+ "<serial>XYZ789012</serial>"
			+ "<devices>"
			+ "<device>"
			+ "<name>LinkedDevice</name>"
			+ "<records>"
			+ "<record>"
			+ "<time zone=\"EST\">2025-01-15 10:30:00</time>"
			+ "<point name=\"Critical Alarms\" value=\"0x0001\"/>"
			+ "<point name=\"Informative Alarms\" value=\"0x0002\"/>"
			+ "</record>"
			+ "</records>"
			+ "</device>"
			+ "</devices>"
			+ "</DAS>";

	private static final String ERROR_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
			+ "<DAS>"
			+ "<devices>"
			+ "<device>"
			+ "<name>ErrorDevice</name>"
			+ "<records>"
			+ "<record>"
			+ "<time zone=\"EST\">2025-01-15 10:30:00</time>"
			+ "<error text=\"Fault Detected\">5</error>"
			+ "</record>"
			+ "</records>"
			+ "</device>"
			+ "</devices>"
			+ "</DAS>";

	private static final String UPDATE_EVENT_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
			+ "<DAS>"
			+ "<mode>LOGFILEUPLOAD</mode>"
			+ "<devices>"
			+ "<device>"
			+ "<name>TestDevice</name>"
			+ "</device>"
			+ "</devices>"
			+ "</DAS>";

	@BeforeEach
	void setUp() throws Exception {
		originalGenerationComponent = IComponentRegistry.generationComponent;
		originalDeviceComponent = IComponentRegistry.deviceComponent;
		originalAlarmComponent = IComponentRegistry.alarmComponent;
		originalLinkedDeviceComponent = IComponentRegistry.linkedDeviceComponent;
		originalOSComponent = IComponentRegistry.OSComponent;
		originalMappingComponent = IComponentRegistry.mappingComponent;

		mockGenerationComponent = mock(IngestComponent.class);
		mockDeviceComponent = mock(DeviceComponent.class);
		mockAlarmComponent = mock(AlarmComponent.class);
		mockLinkedDeviceComponent = mock(LinkedDeviceComponent.class);
		mockOSComponent = mock(OpenSearchComponent.class);
		mockMappingComponent = mock(MappingComponent.class);

		setFinalStatic(IComponentRegistry.class.getField("generationComponent"), mockGenerationComponent);
		setFinalStatic(IComponentRegistry.class.getField("deviceComponent"), mockDeviceComponent);
		setFinalStatic(IComponentRegistry.class.getField("alarmComponent"), mockAlarmComponent);
		setFinalStatic(IComponentRegistry.class.getField("linkedDeviceComponent"), mockLinkedDeviceComponent);
		setFinalStatic(IComponentRegistry.class.getField("OSComponent"), mockOSComponent);
		setFinalStatic(IComponentRegistry.class.getField("mappingComponent"), mockMappingComponent);

		component = new ObviusIngestComponent();
	}

	@AfterEach
	void tearDown() throws Exception {
		setFinalStatic(IComponentRegistry.class.getField("generationComponent"), originalGenerationComponent);
		setFinalStatic(IComponentRegistry.class.getField("deviceComponent"), originalDeviceComponent);
		setFinalStatic(IComponentRegistry.class.getField("alarmComponent"), originalAlarmComponent);
		setFinalStatic(IComponentRegistry.class.getField("linkedDeviceComponent"), originalLinkedDeviceComponent);
		setFinalStatic(IComponentRegistry.class.getField("OSComponent"), originalOSComponent);
		setFinalStatic(IComponentRegistry.class.getField("mappingComponent"), originalMappingComponent);
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
	void testHandleDeviceBody_nullCustomerId() throws Exception {
		DeviceData result = component.handleDeviceBody(VALID_XML, null);

		assertNull(result);
		verify(mockGenerationComponent, never()).handleDevice(any(), any());
	}

	@Test
	void testHandleDeviceBody_blankCustomerId() throws Exception {
		DeviceData result = component.handleDeviceBody(VALID_XML, "");

		assertNull(result);
		verify(mockGenerationComponent, never()).handleDevice(any(), any());
	}

	@Test
	void testHandleDeviceBody_nullBody() throws Exception {
		DeviceData result = component.handleDeviceBody(null, "customer-1");

		assertNull(result);
		verify(mockGenerationComponent, never()).handleDevice(any(), any());
	}

	@Test
	void testHandleDeviceBody_blankBody() throws Exception {
		DeviceData result = component.handleDeviceBody("", "customer-1");

		assertNull(result);
		verify(mockGenerationComponent, never()).handleDevice(any(), any());
	}

	@Test
	void testHandleDeviceBody_linkedDevice() throws Exception {
		DeviceData result = component.handleDeviceBody(LINKED_DEVICE_XML, "customer-1");

		assertNull(result);
		verify(mockLinkedDeviceComponent).update(any(LinkedDevice.class));
		verify(mockGenerationComponent, never()).handleDevice(any(), any());
	}

	@Test
	void testHandleDeviceBody_validDevice() throws Exception {
		Device device = createTestDevice();
		DeviceData deviceData = createTestDeviceData();

		when(mockGenerationComponent.findDeviceFromDeviceName("customer-1", "TestDevice"))
				.thenReturn(device);
		when(mockMappingComponent.getMappings("customer-1")).thenReturn(Collections.emptyList());
		when(mockGenerationComponent.handleDevice(eq(device), any(DeviceData.class)))
				.thenReturn(deviceData);

		DeviceData result = component.handleDeviceBody(VALID_XML, "customer-1");

		assertNotNull(result);
		assertEquals(deviceData, result);
		verify(mockGenerationComponent).handleDevice(eq(device), any(DeviceData.class));
	}

	@Test
	void testHandleDeviceBody_nullDevice() throws Exception {
		when(mockGenerationComponent.findDeviceFromDeviceName("customer-1", "TestDevice"))
				.thenReturn(null);

		DeviceData result = component.handleDeviceBody(VALID_XML, "customer-1");

		assertNull(result);
		verify(mockGenerationComponent, never()).handleDevice(any(), any());
	}

	@Test
	void testIsLinkedDevice_withCriticalAlarms() throws Exception {
		boolean result = component.isLinkedDevice(LINKED_DEVICE_XML);

		assertTrue(result);
	}

	@Test
	void testIsLinkedDevice_normalDevice() throws Exception {
		boolean result = component.isLinkedDevice(VALID_XML);

		assertFalse(result);
	}

	@Test
	void testHandleLinkedBody_validLinkedDevice() throws Exception {
		component.handleLinkedBody(LINKED_DEVICE_XML, "customer-1");

		ArgumentCaptor<LinkedDevice> captor = ArgumentCaptor.forClass(LinkedDevice.class);
		verify(mockLinkedDeviceComponent).update(captor.capture());

		LinkedDevice linkedDevice = captor.getValue();
		assertEquals("XYZ789012", linkedDevice.getId());
		assertEquals("customer-1", linkedDevice.getCustomerId());
		assertTrue(linkedDevice.getDate() > 0);
		assertTrue(linkedDevice.getCriticalAlarm() != -1);
		assertTrue(linkedDevice.getInformativeAlarm() != -1);
	}

	@Test
	void testHandleLinkedBody_nullBody() throws Exception {
		component.handleLinkedBody(null, "customer-1");

		verify(mockLinkedDeviceComponent, never()).update(any());
	}

	@Test
	void testHandleLinkedBody_nullCustomerId() throws Exception {
		component.handleLinkedBody(LINKED_DEVICE_XML, null);

		verify(mockLinkedDeviceComponent, never()).update(any());
	}

	@Test
	void testHandleLinkedBody_noSerialNumber() throws Exception {
		String xmlNoSerial = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<DAS>"
				+ "<devices><device><name>Test</name></device></devices>"
				+ "</DAS>";

		component.handleLinkedBody(xmlNoSerial, "customer-1");

		verify(mockLinkedDeviceComponent, never()).update(any());
	}

	@Test
	void testIsUpdateEvent_fileData() throws Exception {
		boolean result = component.isUpdateEvent(UPDATE_EVENT_XML);

		assertTrue(result);
	}

	@Test
	void testIsUpdateEvent_streamMode() throws Exception {
		boolean result = component.isUpdateEvent(VALID_XML);

		assertFalse(result);
	}

	@Test
	void testFindDeviceName_validXml() throws Exception {
		String deviceName = component.findDeviceName(VALID_XML);

		assertEquals("TestDevice", deviceName);
	}

	@Test
	void testFindDeviceName_noDeviceName() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><DAS></DAS>";

		String deviceName = component.findDeviceName(xml);

		assertNull(deviceName);
	}

	@Test
	void testIsOK_validOkResponse() {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<DAS>"
				+ "<devices>"
				+ "<device>"
				+ "<records>"
				+ "<record>"
				+ "<error text=\"Ok\">0</error>"
				+ "</record>"
				+ "</records>"
				+ "</device>"
				+ "</devices>"
				+ "</DAS>";

		boolean result = component.isOK(xml);

		assertTrue(result);
	}

	@Test
	void testIsOK_errorResponse() {
		boolean result = component.isOK(ERROR_XML);

		assertFalse(result);
	}

	@Test
	void testFindError_okError() {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<DAS>"
				+ "<devices>"
				+ "<device>"
				+ "<records>"
				+ "<record>"
				+ "<error text=\"Ok\">0</error>"
				+ "</record>"
				+ "</records>"
				+ "</device>"
				+ "</devices>"
				+ "</DAS>";

		String error = component.findError(xml);

		assertEquals("Ok errorCode:0", error);
	}

	@Test
	void testFindError_actualError() {
		String error = component.findError(ERROR_XML);

		assertEquals("Fault Detected errorCode:5", error);
	}

	@Test
	void testFindError_noError() {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><DAS></DAS>";

		String error = component.findError(xml);

		assertNull(error);
	}

	@Test
	void testParseDeviceInformation_validXml() {
		when(mockMappingComponent.getMappings("customer-1")).thenReturn(Collections.emptyList());

		DeviceData result =
				component.parseDeviceInformation(VALID_XML, "site-1", "TestDevice", "customer-1", "device-1");

		assertNotNull(result);
		assertEquals("site-1", result.getSiteId());
		assertEquals("customer-1", result.getCustomerId());
		assertEquals("device-1", result.getDeviceId());
		assertEquals(1000.5f, result.getTotalEnergyConsumed());
		assertEquals(500.2f, result.getTotalRealPower());
		assertEquals(10.5f, result.getAverageCurrent());
		assertEquals(240.0f, result.getAverageVoltage());
		assertEquals(95.0f, result.getPowerFactor());
		assertNotNull(result.getDate());
	}

	@Test
	void testParseDeviceInformation_errorDevice() {
		when(mockMappingComponent.getMappings("customer-1")).thenReturn(Collections.emptyList());
		when(mockOSComponent.getMaxTotalEnergyConsumed(eq("customer-1"), eq("device-1"), anyLong()))
				.thenReturn(800f);

		DeviceData result =
				component.parseDeviceInformation(ERROR_XML, "site-1", "ErrorDevice", "customer-1", "device-1");

		assertNotNull(result);
		assertEquals(800f, result.getTotalEnergyConsumed());
		assertEquals(0f, result.getTotalRealPower());
		verify(mockAlarmComponent).faultDetected("customer-1", "device-1", "site-1", "Fault Detected errorCode:5");
	}

	@Test
	void testParseDeviceInformation_calculatesRealPower() {
		String xmlNoRealPower = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<DAS>"
				+ "<devices>"
				+ "<device>"
				+ "<name>TestDevice</name>"
				+ "<records>"
				+ "<record>"
				+ "<time zone=\"EST\">2025-01-15 10:30:00</time>"
				+ "<error text=\"Ok\">0</error>"
				+ "<point name=\"Average Current\" value=\"10.0\"/>"
				+ "<point name=\"Average Voltage (L-N)\" value=\"240.0\"/>"
				+ "<point name=\"Total (System) Power Factor\" value=\"100.0\"/>"
				+ "</record>"
				+ "</records>"
				+ "</device>"
				+ "</devices>"
				+ "</DAS>";

		when(mockMappingComponent.getMappings("customer-1")).thenReturn(Collections.emptyList());

		DeviceData result =
				component.parseDeviceInformation(xmlNoRealPower, "site-1", "TestDevice", "customer-1", "device-1");

		assertNotNull(result);
		assertTrue(result.getTotalRealPower() > 0);
	}

	@Test
	void testGetTimestampFromBody_validTimestamp() throws XPathExpressionException {
		Optional<Date> result = component.getTimestampFromBody(VALID_XML);

		assertTrue(result.isPresent());
		assertNotNull(result.get());
	}

	@Test
	void testGetTimestampFromBody_nullTimestamp() throws XPathExpressionException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<DAS>"
				+ "<devices>"
				+ "<device>"
				+ "<records>"
				+ "<record>"
				+ "<time zone=\"EST\">NULL</time>"
				+ "</record>"
				+ "</records>"
				+ "</device>"
				+ "</devices>"
				+ "</DAS>";

		Optional<Date> result = component.getTimestampFromBody(xml);

		assertTrue(result.isEmpty());
	}

	@Test
	void testGetTimestampFromBody_noTimestamp() throws XPathExpressionException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><DAS></DAS>";

		Optional<Date> result = component.getTimestampFromBody(xml);

		assertTrue(result.isEmpty());
	}

	@Test
	void testFindSerialNumber_validSerial() throws XPathExpressionException {
		Optional<String> result = component.findSerialNumber(VALID_XML);

		assertTrue(result.isPresent());
		assertEquals("ABC123456", result.get());
	}

	@Test
	void testFindSerialNumber_noSerial() throws XPathExpressionException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><DAS></DAS>";

		Optional<String> result = component.findSerialNumber(xml);

		assertTrue(result.isEmpty());
	}

	@Test
	void testHandleSerialNumber_nullDevice() throws XPathExpressionException {
		component.handleSerialNumber(null, VALID_XML);

		verify(mockDeviceComponent, never()).updateDevice(any());
	}

	@Test
	void testHandleSerialNumber_blankBody() throws XPathExpressionException {
		Device device = createTestDevice();

		component.handleSerialNumber(device, "");

		verify(mockDeviceComponent, never()).updateDevice(any());
	}

	@Test
	void testHandleSerialNumber_deviceAlreadyHasSerial() throws XPathExpressionException {
		Device device = createTestDevice();
		device.setSerialNumber("EXISTING123");

		component.handleSerialNumber(device, VALID_XML);

		verify(mockDeviceComponent, never()).updateDevice(any());
	}

	@Test
	void testParseDeviceInformation_handlesNullValueAttribute() {
		String xmlNullValue = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<DAS>"
				+ "<devices>"
				+ "<device>"
				+ "<name>TestDevice</name>"
				+ "<records>"
				+ "<record>"
				+ "<time zone=\"EST\">2025-01-15 10:30:00</time>"
				+ "<error text=\"Ok\">0</error>"
				+ "<point name=\"Total Energy Consumption\" value=\"NULL\"/>"
				+ "</record>"
				+ "</records>"
				+ "</device>"
				+ "</devices>"
				+ "</DAS>";

		when(mockMappingComponent.getMappings("customer-1")).thenReturn(Collections.emptyList());

		DeviceData result =
				component.parseDeviceInformation(xmlNullValue, "site-1", "TestDevice", "customer-1", "device-1");

		assertNotNull(result);
		verify(mockAlarmComponent).faultDetected(eq("customer-1"), eq("device-1"), eq("site-1"), anyString());
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
