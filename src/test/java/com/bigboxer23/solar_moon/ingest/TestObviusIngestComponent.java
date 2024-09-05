package com.bigboxer23.solar_moon.ingest;

import static com.bigboxer23.solar_moon.ingest.MeterConstants.CRITICAL_ALARMS;
import static com.bigboxer23.solar_moon.ingest.MeterConstants.INFORMATIVE_ALARMS;
import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.TestConstants;
import com.bigboxer23.solar_moon.TestUtils;
import com.bigboxer23.solar_moon.alarm.IAlarmConstants;
import com.bigboxer23.solar_moon.data.Alarm;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.xml.xpath.XPathExpressionException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opensearch.client.ResponseException;

/** */
public class TestObviusIngestComponent implements TestConstants, IComponentRegistry {

	private static Device device;

	@BeforeAll
	public static void before() {
		TestUtils.setupSite();
		device = TestUtils.getDevice();
	}

	@AfterAll
	public static void afterAll() {
		TestUtils.nukeCustomerId(CUSTOMER_ID);
	}

	@Test
	public void testFindDeviceName() throws XPathExpressionException {
		assertEquals(device1Name, obviousIngestComponent.findDeviceName(device1Xml));
		try {
			obviousIngestComponent.findDeviceName("invalid xml");
		} catch (XPathExpressionException e) {
			return;
		}
		fail();
	}

	@Test
	public void testIsLinkedDevice() throws XPathExpressionException {
		assertFalse(obviousIngestComponent.isLinkedDevice(device1Xml));
		assertTrue(obviousIngestComponent.isLinkedDevice(LINKED_DEVICE_XML));
		assertTrue(obviousIngestComponent.isLinkedDevice(LINKED_DEVICE_XML.replace(CRITICAL_ALARMS, "xxx")));
		assertTrue(obviousIngestComponent.isLinkedDevice(LINKED_DEVICE_XML.replace(INFORMATIVE_ALARMS, "xxx")));
		assertFalse(obviousIngestComponent.isLinkedDevice(
				LINKED_DEVICE_XML.replace(CRITICAL_ALARMS, "xxx").replace(INFORMATIVE_ALARMS, "yyy")));
	}

	@Test
	public void handleLinkedBody() throws XPathExpressionException {
		linkedDeviceComponent.delete(serialNumber, CUSTOMER_ID);
		assertFalse(linkedDeviceComponent
				.queryBySerialNumber(serialNumber, CUSTOMER_ID)
				.isPresent());

		obviousIngestComponent.handleLinkedBody(null, CUSTOMER_ID);
		assertFalse(linkedDeviceComponent
				.queryBySerialNumber(serialNumber, CUSTOMER_ID)
				.isPresent());
		obviousIngestComponent.handleLinkedBody(
				LINKED_DEVICE_XML.replace("<serial>", "<ss>").replace("</serial>", "</ss>"), CUSTOMER_ID);
		assertFalse(linkedDeviceComponent
				.queryBySerialNumber(serialNumber, CUSTOMER_ID)
				.isPresent());

		obviousIngestComponent.handleLinkedBody(LINKED_DEVICE_XML, null);
		assertFalse(linkedDeviceComponent
				.queryBySerialNumber(serialNumber, CUSTOMER_ID)
				.isPresent());

		obviousIngestComponent.handleLinkedBody(LINKED_DEVICE_XML.replace(date, "garbage date"), CUSTOMER_ID);
		assertFalse(linkedDeviceComponent
				.queryBySerialNumber(serialNumber, CUSTOMER_ID)
				.isPresent());

		obviousIngestComponent.handleLinkedBody(LINKED_DEVICE_XML, CUSTOMER_ID);
		assertTrue(linkedDeviceComponent
				.queryBySerialNumber(serialNumber, CUSTOMER_ID)
				.isPresent());
	}

	@Test
	public void findSerialNumber() throws XPathExpressionException {
		try {
			obviousIngestComponent.findSerialNumber(serialNumber);
			fail();
		} catch (XPathExpressionException ignored) {

		}
		assertFalse(obviousIngestComponent.findSerialNumber(null).isPresent());
		assertFalse(obviousIngestComponent.findSerialNumber("").isPresent());

		Optional<String> serial = obviousIngestComponent.findSerialNumber(
				LINKED_DEVICE_XML.replace("<serial>", "<ss>").replace("</serial>", "</ss>"));
		assertFalse(serial.isPresent());

		serial = obviousIngestComponent.findSerialNumber(LINKED_DEVICE_XML);
		assertTrue(serial.isPresent());
		assertEquals(serialNumber, serial.get());
		serial = obviousIngestComponent.findSerialNumber(device1Xml);
		assertTrue(serial.isPresent());
		assertEquals(serialNumber, serial.get());
	}

	@Test
	public void handleSerialNumber() throws XPathExpressionException {
		assertNotNull(TestUtils.getDevice().getSerialNumber());
		TestUtils.getDevice().setSerialNumber(null);
		deviceComponent.updateDevice(TestUtils.getDevice());
		Optional<Device> dbDevice =
				deviceComponent.findDeviceById(TestUtils.getDevice().getId(), CUSTOMER_ID);
		assertTrue(dbDevice.isPresent());
		assertNull(dbDevice.get().getSerialNumber());

		obviousIngestComponent.handleSerialNumber(device, null);
		dbDevice = deviceComponent.findDeviceById(TestUtils.getDevice().getId(), CUSTOMER_ID);
		assertTrue(dbDevice.isPresent());
		assertNull(dbDevice.get().getSerialNumber());
		obviousIngestComponent.handleSerialNumber(device, device1Xml);
		dbDevice = deviceComponent.findDeviceById(TestUtils.getDevice().getId(), CUSTOMER_ID);
		assertTrue(dbDevice.isPresent());
		assertEquals(TestConstants.serialNumber, dbDevice.get().getSerialNumber());

		obviousIngestComponent.handleSerialNumber(
				device, device1Xml.replace(TestConstants.serialNumber, "testingSerial"));
		dbDevice = deviceComponent.findDeviceById(TestUtils.getDevice().getId(), CUSTOMER_ID);
		assertTrue(dbDevice.isPresent());
		assertEquals(TestConstants.serialNumber, dbDevice.get().getSerialNumber());
	}

	@Test
	public void testIsUpdateEvent() throws XPathExpressionException {
		assertTrue(obviousIngestComponent.isUpdateEvent(device1Xml));
		assertFalse(obviousIngestComponent.isUpdateEvent(nonUpdateStatus));
		try {
			obviousIngestComponent.isUpdateEvent("invalid xml");
		} catch (XPathExpressionException e) {
			return;
		}
		fail();
	}

	@Test
	public void testCalculatedTotalRealPower() {
		DeviceData aDeviceData2 = obviousIngestComponent.parseDeviceInformation(
				device2Xml, device.getSiteId(), TestConstants.deviceName, device.getClientId(), device.getId());
		assertEquals(2.134055f, aDeviceData2.getTotalRealPower());
		aDeviceData2.setPowerFactor(-aDeviceData2.getPowerFactor());
		assertEquals(2.134055f, aDeviceData2.getTotalRealPower());
	}

	@Test
	public void getTimestampFromBody() throws XPathExpressionException {
		assertTrue(obviousIngestComponent.getTimestampFromBody(device2XmlNull).isPresent());
		assertFalse(
				obviousIngestComponent.getTimestampFromBody(device2XmlBadDate).isPresent());
		assertFalse(
				obviousIngestComponent.getTimestampFromBody(device2XmlNoDate).isPresent());
		assertFalse(obviousIngestComponent.getTimestampFromBody(device2XmlNoTZ).isPresent());
	}

	@Test
	public void testParseDeviceInformation() {
		DeviceData deviceData = obviousIngestComponent.parseDeviceInformation(
				device2XmlNull, device.getSiteId(), TestConstants.deviceName, device.getClientId(), device.getId());
		assertNotNull(deviceData);
		assertEquals(0, deviceData.getTotalRealPower());
		assertEquals(0, deviceData.getAverageVoltage());
		assertEquals(0, deviceData.getAverageCurrent());
		deviceData = obviousIngestComponent.parseDeviceInformation(
				device2Xml, device.getSiteId(), TestConstants.deviceName, device.getClientId(), device.getId());
		assertNotNull(deviceData);
		assertTrue(deviceData.isValid());
		assertNull(obviousIngestComponent.parseDeviceInformation(
				TestConstants.deviceName,
				TestConstants.deviceName,
				TestConstants.deviceName,
				device.getClientId(),
				device.getId()));
	}

	@Test
	public void testHandleDeviceBody() throws XPathExpressionException, ResponseException {
		String deviceXML = TestUtils.getDeviceXML(TestConstants.deviceName + 0, new Date(), -1);
		TestUtils.setupSite();
		assertNull(obviousIngestComponent.handleDeviceBody(null, null));
		assertNull(obviousIngestComponent.handleDeviceBody(deviceXML, null));
		assertNull(obviousIngestComponent.handleDeviceBody("", null));
		assertNull(obviousIngestComponent.handleDeviceBody(null, TestConstants.CUSTOMER_ID));
		assertNull(obviousIngestComponent.handleDeviceBody("", null));
		assertNull(obviousIngestComponent.handleDeviceBody(deviceXML, ""));
		assertNull(obviousIngestComponent.handleDeviceBody(null, TestConstants.CUSTOMER_ID));
		assertNull(obviousIngestComponent.handleDeviceBody(
				deviceXML, TestConstants.CUSTOMER_ID + "invalid")); // No subscription
		TestUtils.nukeCustomerId(TestConstants.CUSTOMER_ID + "invalid");
		assertNull(obviousIngestComponent.handleDeviceBody(nonUpdateStatus, TestConstants.CUSTOMER_ID));

		assertNotNull(obviousIngestComponent.handleDeviceBody(
				TestUtils.getDeviceXML(device2XmlNull, TestUtils.getDevice().getDeviceName(), new Date(), -1),
				TestUtils.getDevice().getClientId()));
		List<Alarm> alarms = alarmComponent.findAlarmsByDevice(
				TestUtils.getDevice().getClientId(), TestUtils.getDevice().getId());
		assertFalse(alarms.isEmpty());
		assertEquals(IAlarmConstants.ACTIVE, alarms.getFirst().getState());

		assertNotNull(obviousIngestComponent.handleDeviceBody(deviceXML, TestConstants.CUSTOMER_ID));
	}

	@Test
	public void testDateRead() {
		DeviceData deviceData = obviousIngestComponent.parseDeviceInformation(
				device2Xml, device.getSiteId(), TestConstants.deviceName, device.getClientId(), device.getId());
		assertNotNull(deviceData.getDate());
		SimpleDateFormat sdf = new SimpleDateFormat(MeterConstants.DATE_PATTERN);
		assertEquals(sdf.format(deviceData.getDate()), "2020-08-21 12:30:00 CDT");
		deviceData = obviousIngestComponent.parseDeviceInformation(
				device2XmlNoDate, device.getSiteId(), TestConstants.deviceName, device.getClientId(), device.getId());
		assertNull(deviceData);
		deviceData = obviousIngestComponent.parseDeviceInformation(
				device2XmlBadDate, device.getSiteId(), TestConstants.deviceName, device.getClientId(), device.getId());
		assertNull(deviceData);
		deviceData = obviousIngestComponent.parseDeviceInformation(
				device2XmlNoTZ, device.getSiteId(), TestConstants.deviceName, device.getClientId(), device.getId());
		assertNull(deviceData);
	}

	@Test
	public void calcTotalRealPower() {
		float avgVoltage = 290f;
		float avgCurrent = 67.56f;
		float powerFactor = .99f;
		double rp = (avgVoltage * avgCurrent * Math.abs(powerFactor / 100) * Math.sqrt(3)) / 1000f;
		System.out.println(rp);
	}

	@Test
	public void findError() {
		assertEquals(
				"Device Failed to Respond (the modbus device may be off or disconnected)" + " errorCode:139",
				obviousIngestComponent.findError(deviceError));
	}

	@Test
	public void isOK() {
		assertTrue(obviousIngestComponent.isOK(device2Xml));
		assertFalse(obviousIngestComponent.isOK(deviceError));
		assertFalse(obviousIngestComponent.isOK(null));
		assertFalse(obviousIngestComponent.isOK(""));
	}
}
