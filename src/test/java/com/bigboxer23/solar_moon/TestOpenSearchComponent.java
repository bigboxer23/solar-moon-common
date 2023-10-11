package com.bigboxer23.solar_moon;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.open_search.OpenSearchComponent;
import com.bigboxer23.solar_moon.open_search.OpenSearchUtils;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import javax.xml.xpath.XPathExpressionException;
import org.junit.jupiter.api.Test;

/** */
public class TestOpenSearchComponent {

	private OpenSearchComponent OSComponent = new OpenSearchComponent();

	private DeviceComponent deviceComponent = new DeviceComponent();

	private GenerationMeterComponent generationComponent = new GenerationMeterComponent(
			OSComponent,
			new AlarmComponent(new OpenWeatherComponent()),
			deviceComponent,
			new SiteComponent(OSComponent, deviceComponent));

	@Test
	public void testGetTotalEnergyConsumed() {
		// test invalid case
		Float consumed = OSComponent.getTotalEnergyConsumed(TestDeviceComponent.deviceName);
		assertNull(consumed);
	}

	@Test
	public void testGetLastDeviceEntry() throws XPathExpressionException {
		TestUtils.setupSite(deviceComponent, OSComponent);
		Date date = TimeUtils.get15mRoundedDate();
		LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
		Date prevDate =
				Date.from(ldt.minusMinutes(30).atZone(ZoneId.systemDefault()).toInstant());
		Date nextDate =
				Date.from(ldt.plusMinutes(15).atZone(ZoneId.systemDefault()).toInstant());
		generationComponent.handleDeviceBody(
				TestUtils.getDeviceXML(TestDeviceComponent.deviceName + 0, prevDate), TestDeviceComponent.clientId);
		DeviceData data =
				OSComponent.getLastDeviceEntry(TestDeviceComponent.clientId, TestDeviceComponent.deviceName + 0);
		assertNull(data);
		generationComponent.handleDeviceBody(
				TestUtils.getDeviceXML(TestDeviceComponent.deviceName + 0, nextDate), TestDeviceComponent.clientId);
		data = OSComponent.getLastDeviceEntry(TestDeviceComponent.clientId, TestDeviceComponent.deviceName + 0);
		assertNull(data);
		generationComponent.handleDeviceBody(
				TestUtils.getDeviceXML(TestDeviceComponent.deviceName + 0, date), TestDeviceComponent.clientId);
		data = OSComponent.getLastDeviceEntry(TestDeviceComponent.clientId, TestDeviceComponent.deviceName + 0);
		assertNotNull(data);
		assertTrue(data.isValid());
		assertNotNull(data.getCustomerId());
		assertEquals(TestDeviceComponent.clientId, data.getCustomerId());
	}

	@Test
	public void testGetDeviceByTimePeriod() throws XPathExpressionException, InterruptedException {
		TestUtils.setupSite(deviceComponent, OSComponent);
		OpenSearchUtils.waitForIndexing();
		Date date = TimeUtils.get15mRoundedDate();
		assertNull(OSComponent.getDeviceByTimePeriod(
				TestDeviceComponent.clientId, TestDeviceComponent.deviceName + 0, date));

		LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
		Date past =
				Date.from(ldt.minusMinutes(15).atZone(ZoneId.systemDefault()).toInstant());
		Date future =
				Date.from(ldt.plusMinutes(15).atZone(ZoneId.systemDefault()).toInstant());

		generationComponent.handleDeviceBody(
				TestUtils.getDeviceXML(TestDeviceComponent.deviceName + 0, past), TestDeviceComponent.clientId);
		generationComponent.handleDeviceBody(
				TestUtils.getDeviceXML(TestDeviceComponent.deviceName + 0, future), TestDeviceComponent.clientId);
		OpenSearchUtils.waitForIndexing();
		assertNull(OSComponent.getDeviceByTimePeriod(
				TestDeviceComponent.clientId, TestDeviceComponent.deviceName + 0, date));
		TestUtils.validateDateData(OSComponent, future);
		TestUtils.validateDateData(OSComponent, past);
		generationComponent.handleDeviceBody(
				TestUtils.getDeviceXML(TestDeviceComponent.deviceName + 0, date), TestDeviceComponent.clientId);
		OpenSearchUtils.waitForIndexing();
		TestUtils.validateDateData(OSComponent, date);
	}

	@Test
	public void testGetDeviceCountByTimePeriod() throws XPathExpressionException {
		TestUtils.setupSite(deviceComponent, OSComponent);
		Date date = TimeUtils.get15mRoundedDate();
		LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
		Date prevDate =
				Date.from(ldt.minusMinutes(15).atZone(ZoneId.systemDefault()).toInstant());
		Date nextDate =
				Date.from(ldt.plusMinutes(15).atZone(ZoneId.systemDefault()).toInstant());
		generationComponent.handleDeviceBody(
				TestUtils.getDeviceXML(TestDeviceComponent.deviceName + 0, date), TestDeviceComponent.clientId);
		generationComponent.handleDeviceBody(
				TestUtils.getDeviceXML(TestDeviceComponent.deviceName + 0, prevDate), TestDeviceComponent.clientId);
		generationComponent.handleDeviceBody(
				TestUtils.getDeviceXML(TestDeviceComponent.deviceName + 0, nextDate), TestDeviceComponent.clientId);
		assertEquals(
				1,
				OSComponent.getSiteDevicesCountByTimePeriod(
						TestDeviceComponent.clientId, TestDeviceComponent.SITE, date));
		assertEquals(
				1,
				OSComponent.getSiteDevicesCountByTimePeriod(
						TestDeviceComponent.clientId, TestDeviceComponent.SITE, prevDate));
		assertEquals(
				1,
				OSComponent.getSiteDevicesCountByTimePeriod(
						TestDeviceComponent.clientId, TestDeviceComponent.SITE, nextDate));

		generationComponent.handleDeviceBody(
				TestUtils.getDeviceXML(TestDeviceComponent.deviceName + 0, nextDate), TestDeviceComponent.clientId);
		assertEquals(
				1,
				OSComponent.getSiteDevicesCountByTimePeriod(
						TestDeviceComponent.clientId, TestDeviceComponent.SITE, date));
		assertEquals(
				1,
				OSComponent.getSiteDevicesCountByTimePeriod(
						TestDeviceComponent.clientId, TestDeviceComponent.SITE, prevDate));
		assertEquals(
				2,
				OSComponent.getSiteDevicesCountByTimePeriod(
						TestDeviceComponent.clientId, TestDeviceComponent.SITE, nextDate));
	}
}
