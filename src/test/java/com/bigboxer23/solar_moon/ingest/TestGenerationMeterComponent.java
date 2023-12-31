package com.bigboxer23.solar_moon.ingest;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.TestConstants;
import com.bigboxer23.solar_moon.TestUtils;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.xml.xpath.XPathExpressionException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opensearch.client.ResponseException;

/** */
public class TestGenerationMeterComponent implements TestConstants, IComponentRegistry {

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
		assertEquals(device1Name, generationComponent.findDeviceName(device1Xml));
		try {
			generationComponent.findDeviceName("invalid xml");
		} catch (XPathExpressionException e) {
			return;
		}
		fail();
	}

	@Test
	public void testIsUpdateEvent() throws XPathExpressionException {
		assertTrue(generationComponent.isUpdateEvent(device1Xml));
		assertFalse(generationComponent.isUpdateEvent(nonUpdateStatus));
		try {
			generationComponent.isUpdateEvent("invalid xml");
		} catch (XPathExpressionException e) {
			return;
		}
		fail();
	}

	@Test
	public void testCalculatedTotalRealPower() {
		Device device = TestUtils.getDevice();
		DeviceData aDeviceData2 = generationComponent.parseDeviceInformation(
				device2Xml, "site1", TestConstants.deviceName, device.getClientId(), device.getId());
		assertEquals(2.134055f, aDeviceData2.getTotalRealPower());
		aDeviceData2.setPowerFactor(-aDeviceData2.getPowerFactor());
		assertEquals(2.134055f, aDeviceData2.getTotalRealPower());
	}

	@Test
	public void testParseDeviceInformation() {
		DeviceData aDeviceData = generationComponent.parseDeviceInformation(
				device2XmlNull, "site1", TestConstants.deviceName, device.getClientId(), device.getId());
		assertNotNull(aDeviceData);
		assertFalse(aDeviceData.isValid());
		aDeviceData = generationComponent.parseDeviceInformation(
				device2Xml, "site1", TestConstants.deviceName, device.getClientId(), device.getId());
		assertNotNull(aDeviceData);
		assertTrue(aDeviceData.isValid());
		assertNull(generationComponent.parseDeviceInformation(
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
		assertNull(generationComponent.handleDeviceBody(null, null));
		assertNull(generationComponent.handleDeviceBody(deviceXML, null));
		assertNull(generationComponent.handleDeviceBody("", null));
		assertNull(generationComponent.handleDeviceBody(null, TestConstants.CUSTOMER_ID));
		assertNull(generationComponent.handleDeviceBody("", null));
		assertNull(generationComponent.handleDeviceBody(deviceXML, ""));
		assertNull(generationComponent.handleDeviceBody(null, TestConstants.CUSTOMER_ID));
		assertNotNull(generationComponent.handleDeviceBody(deviceXML, TestConstants.CUSTOMER_ID + "invalid"));
		TestUtils.nukeCustomerId(TestConstants.CUSTOMER_ID + "invalid");
		assertNull(generationComponent.handleDeviceBody(nonUpdateStatus, TestConstants.CUSTOMER_ID));
		assertNull(generationComponent.handleDeviceBody(
				TestUtils.getDeviceXML(device2XmlNull, TestConstants.deviceName + 0, null, -1),
				TestConstants.CUSTOMER_ID));
		assertNotNull(generationComponent.handleDeviceBody(deviceXML, TestConstants.CUSTOMER_ID));
	}

	@Test
	public void testDateRead() {
		DeviceData aDeviceData = generationComponent.parseDeviceInformation(
				device2Xml, "site1", TestConstants.deviceName, device.getClientId(), device.getId());
		assertNotNull(aDeviceData.getDate());
		SimpleDateFormat sdf = new SimpleDateFormat(MeterConstants.DATE_PATTERN);
		assertEquals(sdf.format(aDeviceData.getDate()), "2020-08-21 12:30:00 CDT");
		aDeviceData = generationComponent.parseDeviceInformation(
				device2XmlNoDate, "site1", TestConstants.deviceName, device.getClientId(), device.getId());
		assertNull(aDeviceData.getDate());
		aDeviceData = generationComponent.parseDeviceInformation(
				device2XmlBadDate, "site1", TestConstants.deviceName, device.getClientId(), device.getId());
		assertNull(aDeviceData.getDate());
		aDeviceData = generationComponent.parseDeviceInformation(
				device2XmlNoTZ, "site1", TestConstants.deviceName, device.getClientId(), device.getId());
		assertNull(aDeviceData.getDate());
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
				generationComponent.findError(deviceError));
	}

	@Test
	public void isOK() {
		assertTrue(generationComponent.isOK(device2Xml));
		assertFalse(generationComponent.isOK(deviceError));
		assertFalse(generationComponent.isOK(null));
		assertFalse(generationComponent.isOK(""));
	}
}
