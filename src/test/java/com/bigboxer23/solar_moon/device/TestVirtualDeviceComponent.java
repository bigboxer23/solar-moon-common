package com.bigboxer23.solar_moon.device;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.TestConstants;
import com.bigboxer23.solar_moon.TestUtils;
import com.bigboxer23.solar_moon.search.OpenSearchUtils;
import com.bigboxer23.solar_moon.util.TimeUtils;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import javax.xml.xpath.XPathExpressionException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.ResponseException;

/** */
public class TestVirtualDeviceComponent implements IComponentRegistry, TestConstants {

	@BeforeEach
	public void setup() {
		TestUtils.setupSite();
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
		assertNotNull(OSComponent.getDeviceByTimePeriod(
				CUSTOMER_ID, TestUtils.getSite().getId(), date));
	}

	@Test
	public void testHandleSite() throws XPathExpressionException, ResponseException {
		Date date = TimeUtils.get15mRoundedDate();
		for (int ai = 0; ai < 4; ai++) {
			obviousIngestComponent.handleDeviceBody(TestUtils.getDeviceXML(deviceName + ai, date, -1), CUSTOMER_ID);
		}
		OpenSearchUtils.waitForIndexing();
		assertNull(OSComponent.getDeviceEntryWithinLast15Min(
				CUSTOMER_ID, TestUtils.getSite().getId()));
		obviousIngestComponent.handleDeviceBody(TestUtils.getDeviceXML(deviceName + 4, date, -1), CUSTOMER_ID);
		OpenSearchUtils.waitForIndexing();
		TestUtils.validateDateData(TestUtils.getSite().getId(), date);
	}

	@Test
	public void testHandleSiteInterleaved() throws XPathExpressionException, ResponseException {
		Date date = TimeUtils.get15mRoundedDate();
		LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
		Date past =
				Date.from(ldt.minusMinutes(15).atZone(ZoneId.systemDefault()).toInstant());
		Date future =
				Date.from(ldt.plusMinutes(15).atZone(ZoneId.systemDefault()).toInstant());
		for (int ai = 0; ai < 4; ai++) {
			obviousIngestComponent.handleDeviceBody(TestUtils.getDeviceXML(deviceName + ai, date, -1), CUSTOMER_ID);
		}
		for (int ai = 0; ai < 5; ai++) {
			obviousIngestComponent.handleDeviceBody(TestUtils.getDeviceXML(deviceName + ai, past, -1), CUSTOMER_ID);
		}
		OpenSearchUtils.waitForIndexing();
		TestUtils.validateDateData(TestUtils.getSite().getId(), past);
		assertNull(OSComponent.getDeviceByTimePeriod(
				CUSTOMER_ID, TestUtils.getSite().getId(), date));

		for (int ai = 0; ai < 5; ai++) {
			obviousIngestComponent.handleDeviceBody(TestUtils.getDeviceXML(deviceName + ai, future, -1), CUSTOMER_ID);
		}
		OpenSearchUtils.waitForIndexing();
		TestUtils.validateDateData(TestUtils.getSite().getId(), future);
		assertNull(OSComponent.getDeviceByTimePeriod(
				CUSTOMER_ID, TestUtils.getSite().getId(), date));
		obviousIngestComponent.handleDeviceBody(TestUtils.getDeviceXML(deviceName + 4, date, -1), CUSTOMER_ID);
		OpenSearchUtils.waitForIndexing();
		TestUtils.validateDateData(TestUtils.getSite().getId(), date);
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
			obviousIngestComponent.handleDeviceBody(TestUtils.getDeviceXML(deviceName + count, date, -1), CUSTOMER_ID);
			latch.countDown();
		}
	}
}
