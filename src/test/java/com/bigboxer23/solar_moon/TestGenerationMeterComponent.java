package com.bigboxer23.solar_moon;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.open_search.OpenSearchComponent;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.xml.xpath.XPathExpressionException;
import org.junit.jupiter.api.Test;

/** */
public class TestGenerationMeterComponent implements TestConstants {

	private OpenSearchComponent OSComponent = new OpenSearchComponent();

	private DeviceComponent deviceComponent = new DeviceComponent();

	private GenerationMeterComponent generationComponent = new GenerationMeterComponent(
			OSComponent,
			new AlarmComponent(new OpenWeatherComponent()),
			deviceComponent,
			new SiteComponent(OSComponent, deviceComponent));

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
		DeviceData aDeviceData2 = generationComponent.parseDeviceInformation(
				device2Xml,
				"site1",
				TestDeviceComponent.deviceName,
				TestDeviceComponent.clientId,
				TestDeviceComponent.deviceId);
		assertEquals(aDeviceData2.getTotalRealPower(), 422.7f);
		aDeviceData2.setPowerFactor(-aDeviceData2.getPowerFactor());
		assertEquals(aDeviceData2.getTotalRealPower(), 422.7f);
	}

	@Test
	public void testParseDeviceInformation() {
		DeviceData aDeviceData = generationComponent.parseDeviceInformation(
				device2XmlNull,
				"site1",
				TestDeviceComponent.deviceName,
				TestDeviceComponent.clientId,
				TestDeviceComponent.deviceId);
		assertNotNull(aDeviceData);
		assertFalse(aDeviceData.isValid());
		aDeviceData = generationComponent.parseDeviceInformation(
				device2Xml,
				"site1",
				TestDeviceComponent.deviceName,
				TestDeviceComponent.clientId,
				TestDeviceComponent.deviceId);
		assertNotNull(aDeviceData);
		assertTrue(aDeviceData.isValid());
		assertNull(generationComponent.parseDeviceInformation(
				TestDeviceComponent.deviceName,
				TestDeviceComponent.deviceName,
				TestDeviceComponent.deviceName,
				TestDeviceComponent.clientId,
				TestDeviceComponent.deviceId));
	}

	@Test
	public void testHandleDeviceBody() throws XPathExpressionException {
		String deviceXML = TestUtils.getDeviceXML(TestDeviceComponent.deviceName + 0, new Date());
		TestUtils.setupSite(deviceComponent, OSComponent);
		assertNull(generationComponent.handleDeviceBody(null, null));
		assertNull(generationComponent.handleDeviceBody(deviceXML, null));
		assertNull(generationComponent.handleDeviceBody("", null));
		assertNull(generationComponent.handleDeviceBody(null, TestDeviceComponent.clientId));
		assertNull(generationComponent.handleDeviceBody("", null));
		assertNull(generationComponent.handleDeviceBody(deviceXML, ""));
		assertNull(generationComponent.handleDeviceBody(null, TestDeviceComponent.clientId));
		assertNull(generationComponent.handleDeviceBody(deviceXML, TestDeviceComponent.clientId + "invalid"));
		assertNull(generationComponent.handleDeviceBody(nonUpdateStatus, TestDeviceComponent.clientId));
		assertNull(generationComponent.handleDeviceBody(
				TestUtils.getDeviceXML(device2XmlNull, TestDeviceComponent.deviceName + 0, null),
				TestDeviceComponent.clientId));
		assertNotNull(generationComponent.handleDeviceBody(deviceXML, TestDeviceComponent.clientId));
	}

	@Test
	public void testDateRead() {
		DeviceData aDeviceData = generationComponent.parseDeviceInformation(
				device2Xml,
				"site1",
				TestDeviceComponent.deviceName,
				TestDeviceComponent.clientId,
				TestDeviceComponent.deviceId);
		assertNotNull(aDeviceData.getDate());
		SimpleDateFormat sdf = new SimpleDateFormat(MeterConstants.DATE_PATTERN);
		assertEquals(sdf.format(aDeviceData.getDate()), "2020-08-21 12:30:00 CDT");
		aDeviceData = generationComponent.parseDeviceInformation(
				device2XmlNoDate,
				"site1",
				TestDeviceComponent.deviceName,
				TestDeviceComponent.clientId,
				TestDeviceComponent.deviceId);
		assertNull(aDeviceData.getDate());
		aDeviceData = generationComponent.parseDeviceInformation(
				device2XmlBadDate,
				"site1",
				TestDeviceComponent.deviceName,
				TestDeviceComponent.clientId,
				TestDeviceComponent.deviceId);
		assertNull(aDeviceData.getDate());
		aDeviceData = generationComponent.parseDeviceInformation(
				device2XmlNoTZ,
				"site1",
				TestDeviceComponent.deviceName,
				TestDeviceComponent.clientId,
				TestDeviceComponent.deviceId);
		assertNull(aDeviceData.getDate());
	}
}
