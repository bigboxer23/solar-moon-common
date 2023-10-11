package com.bigboxer23.solar_moon;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.open_search.OpenSearchComponent;
import com.bigboxer23.solar_moon.open_search.OpenSearchUtils;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import javax.xml.xpath.XPathExpressionException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;

/** */
public class TestSiteComponent {

	private OpenSearchComponent OSComponent = new OpenSearchComponent();

	private DeviceComponent deviceComponent = new DeviceComponent();

	private GenerationMeterComponent generationComponent = new GenerationMeterComponent(
			OSComponent,
			new AlarmComponent(new OpenWeatherComponent()),
			deviceComponent,
			new SiteComponent(OSComponent, deviceComponent));

	@BeforeEach
	public void setup() {
		TestUtils.setupSite(deviceComponent, OSComponent);
	}

	@Test
	public void testHandleSiteConcurrent() throws InterruptedException {
		Date date = TimeUtils.get15mRoundedDate();
		CountDownLatch latch = new CountDownLatch(5);
		for (int ai = 0; ai < 5; ai++) {
			new Thread(new TestHandleBodyRunnable(ai, date, latch)).start();
		}
		latch.await();
		OpenSearchUtils.waitForIndexing();
		assertNotNull(OSComponent.getDeviceByTimePeriod(TestDeviceComponent.clientId, TestDeviceComponent.SITE, date));
	}

	@Test
	public void testHandleSite() throws XPathExpressionException {
		Date date = TimeUtils.get15mRoundedDate();
		for (int ai = 0; ai < 4; ai++) {
			generationComponent.handleDeviceBody(
					TestUtils.getDeviceXML(TestDeviceComponent.deviceName + ai, date), TestDeviceComponent.clientId);
		}
		OpenSearchUtils.waitForIndexing();
		assertNull(OSComponent.getLastDeviceEntry(TestDeviceComponent.clientId, TestDeviceComponent.SITE));
		generationComponent.handleDeviceBody(
				TestUtils.getDeviceXML(TestDeviceComponent.deviceName + 4, date), TestDeviceComponent.clientId);
		OpenSearchUtils.waitForIndexing();
		TestUtils.validateDateData(OSComponent, TestDeviceComponent.SITE, date);
	}

	@Test
	public void testHandleSiteInterleaved() throws XPathExpressionException {
		Date date = TimeUtils.get15mRoundedDate();
		LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
		Date past =
				Date.from(ldt.minusMinutes(15).atZone(ZoneId.systemDefault()).toInstant());
		Date future =
				Date.from(ldt.plusMinutes(15).atZone(ZoneId.systemDefault()).toInstant());
		for (int ai = 0; ai < 4; ai++) {
			generationComponent.handleDeviceBody(
					TestUtils.getDeviceXML(TestDeviceComponent.deviceName + ai, date), TestDeviceComponent.clientId);
		}
		for (int ai = 0; ai < 5; ai++) {
			generationComponent.handleDeviceBody(
					TestUtils.getDeviceXML(TestDeviceComponent.deviceName + ai, past), TestDeviceComponent.clientId);
		}
		OpenSearchUtils.waitForIndexing();
		TestUtils.validateDateData(OSComponent, TestDeviceComponent.SITE, past);
		assertNull(OSComponent.getDeviceByTimePeriod(TestDeviceComponent.clientId, TestDeviceComponent.SITE, date));

		for (int ai = 0; ai < 5; ai++) {
			generationComponent.handleDeviceBody(
					TestUtils.getDeviceXML(TestDeviceComponent.deviceName + ai, future), TestDeviceComponent.clientId);
		}
		OpenSearchUtils.waitForIndexing();
		TestUtils.validateDateData(OSComponent, TestDeviceComponent.SITE, future);
		assertNull(OSComponent.getDeviceByTimePeriod(TestDeviceComponent.clientId, TestDeviceComponent.SITE, date));
		generationComponent.handleDeviceBody(
				TestUtils.getDeviceXML(TestDeviceComponent.deviceName + 4, date), TestDeviceComponent.clientId);
		OpenSearchUtils.waitForIndexing();
		TestUtils.validateDateData(OSComponent, TestDeviceComponent.SITE, date);
	}

	private class TestHandleBodyRunnable implements Runnable {
		private final int count;
		private final Date date;

		private final CountDownLatch latch;

		public TestHandleBodyRunnable(int count, Date date, CountDownLatch latch) {
			this.count = count;
			this.date = date;
			this.latch = latch;
		}

		@SneakyThrows
		@Override
		public void run() {
			generationComponent.handleDeviceBody(
					TestUtils.getDeviceXML(TestDeviceComponent.deviceName + count, date), TestDeviceComponent.clientId);
			latch.countDown();
		}
	}
}
