package com.bigboxer23.solar_moon;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.xml.xpath.XPathExpressionException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** */
public class TestGenerationMeterComponent implements TestConstants, IComponentRegistry {

	private static Device device;

	@BeforeAll
	public static void before() {
		TestUtils.setupSite();
		device = TestUtils.getDevice();
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
				device2Xml,
				"site1",
				TestDeviceComponent.deviceName,
				device.getClientId(),
				device.getId());
		assertEquals(2.134055f, aDeviceData2.getTotalRealPower());
		aDeviceData2.setPowerFactor(-aDeviceData2.getPowerFactor());
		assertEquals(2.134055f, aDeviceData2.getTotalRealPower());
	}

	@Test
	public void testParseDeviceInformation() {
		DeviceData aDeviceData = generationComponent.parseDeviceInformation(
				device2XmlNull,
				"site1",
				TestDeviceComponent.deviceName,
				device.getClientId(),
				device.getId());
		assertNotNull(aDeviceData);
		assertFalse(aDeviceData.isValid());
		aDeviceData = generationComponent.parseDeviceInformation(
				device2Xml,
				"site1",
				TestDeviceComponent.deviceName,
				device.getClientId(),
				device.getId());
		assertNotNull(aDeviceData);
		assertTrue(aDeviceData.isValid());
		assertNull(generationComponent.parseDeviceInformation(
				TestDeviceComponent.deviceName,
				TestDeviceComponent.deviceName,
				TestDeviceComponent.deviceName,
				device.getClientId(),
				device.getId()));
	}

	@Test
	public void testHandleDeviceBody() throws XPathExpressionException {
		String deviceXML = TestUtils.getDeviceXML(TestDeviceComponent.deviceName + 0, new Date(), -1);
		TestUtils.setupSite();
		assertNull(generationComponent.handleDeviceBody(null, null));
		assertNull(generationComponent.handleDeviceBody(deviceXML, null));
		assertNull(generationComponent.handleDeviceBody("", null));
		assertNull(generationComponent.handleDeviceBody(null, TestDeviceComponent.clientId));
		assertNull(generationComponent.handleDeviceBody("", null));
		assertNull(generationComponent.handleDeviceBody(deviceXML, ""));
		assertNull(generationComponent.handleDeviceBody(null, TestDeviceComponent.clientId));
		assertNotNull(generationComponent.handleDeviceBody(deviceXML, TestDeviceComponent.clientId + "invalid"));
		assertNull(generationComponent.handleDeviceBody(nonUpdateStatus, TestDeviceComponent.clientId));
		assertNull(generationComponent.handleDeviceBody(
				TestUtils.getDeviceXML(device2XmlNull, TestDeviceComponent.deviceName + 0, null, -1),
				TestDeviceComponent.clientId));
		assertNotNull(generationComponent.handleDeviceBody(deviceXML, TestDeviceComponent.clientId));
	}

	@Test
	public void testDateRead() {
		DeviceData aDeviceData = generationComponent.parseDeviceInformation(
				device2Xml,
				"site1",
				TestDeviceComponent.deviceName,
				device.getClientId(),
				device.getId());
		assertNotNull(aDeviceData.getDate());
		SimpleDateFormat sdf = new SimpleDateFormat(MeterConstants.DATE_PATTERN);
		assertEquals(sdf.format(aDeviceData.getDate()), "2020-08-21 12:30:00 CDT");
		aDeviceData = generationComponent.parseDeviceInformation(
				device2XmlNoDate,
				"site1",
				TestDeviceComponent.deviceName,
				device.getClientId(),
				device.getId());
		assertNull(aDeviceData.getDate());
		aDeviceData = generationComponent.parseDeviceInformation(
				device2XmlBadDate,
				"site1",
				TestDeviceComponent.deviceName,
				device.getClientId(),
				device.getId());
		assertNull(aDeviceData.getDate());
		aDeviceData = generationComponent.parseDeviceInformation(
				device2XmlNoTZ,
				"site1",
				TestDeviceComponent.deviceName,
				device.getClientId(),
				device.getId());
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
}
