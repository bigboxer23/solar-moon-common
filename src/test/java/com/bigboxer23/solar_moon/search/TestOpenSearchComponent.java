package com.bigboxer23.solar_moon.search;

import static com.bigboxer23.solar_moon.search.OpenSearchConstants.*;
import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.TestConstants;
import com.bigboxer23.solar_moon.TestUtils;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.ingest.MeterConstants;
import com.bigboxer23.solar_moon.util.TimeUtils;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import javax.xml.xpath.XPathExpressionException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.ResponseException;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.DateHistogramBucket;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;

/** */
public class TestOpenSearchComponent implements IComponentRegistry, TestConstants {
	@BeforeEach
	public void setup() {
		TestUtils.setupSite();
	}

	@Test
	public void testGetTotalEnergyConsumed() {
		// test invalid case
		assertNull(OSComponent.getTotalEnergyConsumed(deviceName + System.currentTimeMillis()));
	}

	@AfterAll
	public static void afterAll() {
		TestUtils.nukeCustomerId(CUSTOMER_ID);
	}

	@Test
	public void testGetLastDeviceEntry() throws XPathExpressionException, ResponseException {
		Date date = TimeUtils.get15mRoundedDate();
		LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
		Date prevDate =
				Date.from(ldt.minusMinutes(30).atZone(ZoneId.systemDefault()).toInstant());
		Date nextDate =
				Date.from(ldt.plusMinutes(15).atZone(ZoneId.systemDefault()).toInstant());
		generationComponent.handleDeviceBody(TestUtils.getDeviceXML(deviceName + 0, prevDate, -1), CUSTOMER_ID);
		DeviceData data = OSComponent.getDeviceEntryWithinLast15Min(CUSTOMER_ID, deviceName + 0);
		assertNull(data);
		generationComponent.handleDeviceBody(TestUtils.getDeviceXML(deviceName + 0, nextDate, -1), CUSTOMER_ID);
		data = OSComponent.getDeviceEntryWithinLast15Min(CUSTOMER_ID, deviceName + 0);
		assertNull(data);
		generationComponent.handleDeviceBody(TestUtils.getDeviceXML(deviceName + 0, date, -1), CUSTOMER_ID);
		data = OSComponent.getDeviceEntryWithinLast15Min(CUSTOMER_ID, deviceName + 0);
		assertNotNull(data);
		assertTrue(data.isValid());
		assertNotNull(data.getCustomerId());
		assertEquals(CUSTOMER_ID, data.getCustomerId());
	}

	@Test
	public void testGetDeviceByTimePeriod() throws XPathExpressionException, InterruptedException, ResponseException {
		Date date = TimeUtils.get15mRoundedDate();
		assertNull(OSComponent.getDeviceByTimePeriod(CUSTOMER_ID, deviceName + 0, date));

		LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
		Date past =
				Date.from(ldt.minusMinutes(15).atZone(ZoneId.systemDefault()).toInstant());
		Date future =
				Date.from(ldt.plusMinutes(15).atZone(ZoneId.systemDefault()).toInstant());

		generationComponent.handleDeviceBody(TestUtils.getDeviceXML(deviceName + 0, past, -1), CUSTOMER_ID);
		generationComponent.handleDeviceBody(TestUtils.getDeviceXML(deviceName + 0, future, -1), CUSTOMER_ID);
		OpenSearchUtils.waitForIndexing();
		assertNull(OSComponent.getDeviceByTimePeriod(CUSTOMER_ID, deviceName + 0, date));
		TestUtils.validateDateData(future);
		TestUtils.validateDateData(past);
		generationComponent.handleDeviceBody(TestUtils.getDeviceXML(deviceName + 0, date, -1), CUSTOMER_ID);
		OpenSearchUtils.waitForIndexing();
		TestUtils.validateDateData(date);
	}

	@Test
	public void testGetDeviceCountByTimePeriod() throws XPathExpressionException, ResponseException {
		Date date = TimeUtils.get15mRoundedDate();
		LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
		Date prevDate =
				Date.from(ldt.minusMinutes(15).atZone(ZoneId.systemDefault()).toInstant());
		Date nextDate =
				Date.from(ldt.plusMinutes(15).atZone(ZoneId.systemDefault()).toInstant());
		generationComponent.handleDeviceBody(TestUtils.getDeviceXML(deviceName + 0, date, -1), CUSTOMER_ID);
		generationComponent.handleDeviceBody(TestUtils.getDeviceXML(deviceName + 0, prevDate, -1), CUSTOMER_ID);
		generationComponent.handleDeviceBody(TestUtils.getDeviceXML(deviceName + 0, nextDate, -1), CUSTOMER_ID);
		assertEquals(1, OSComponent.getSiteDevicesCountByTimePeriod(CUSTOMER_ID, SITE, date));
		assertEquals(1, OSComponent.getSiteDevicesCountByTimePeriod(CUSTOMER_ID, SITE, prevDate));
		assertEquals(1, OSComponent.getSiteDevicesCountByTimePeriod(CUSTOMER_ID, SITE, nextDate));

		generationComponent.handleDeviceBody(TestUtils.getDeviceXML(deviceName + 0, nextDate, -1), CUSTOMER_ID);
		assertEquals(1, OSComponent.getSiteDevicesCountByTimePeriod(CUSTOMER_ID, SITE, date));
		assertEquals(1, OSComponent.getSiteDevicesCountByTimePeriod(CUSTOMER_ID, SITE, prevDate));
		assertEquals(2, OSComponent.getSiteDevicesCountByTimePeriod(CUSTOMER_ID, SITE, nextDate));
	}

	@Test
	public void testTimeSeriesSearch() throws XPathExpressionException, ResponseException {
		TestUtils.seedOpenSearchData();
		LocalDateTime ldt = LocalDateTime.ofInstant(new Date().toInstant(), ZoneId.systemDefault())
				.minusDays(2);
		SearchJSON json = new SearchJSON(
				CUSTOMER_ID,
				deviceName + 0,
				Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()).getTime(),
				Date.from(ldt.minusDays(1).atZone(ZoneId.systemDefault()).toInstant())
						.getTime());
		json.setType(TS_SEARCH_TYPE);
		json.setTimeZone(ZonedDateTime.now().getZone().getId());
		SearchResponse response = OSComponent.search(json);
		assertNotNull(response.aggregations().get("2"));
		assertNotNull(((Aggregate) response.aggregations().get("2"))._get());
		List<DateHistogramBucket> buckets = ((Aggregate) response.aggregations().get("2"))
				.dateHistogram()
				.buckets()
				.array();
		for (int ai = 0; ai < buckets.size() - 1; ai++) {
			System.out.println(buckets.get(ai).aggregations().get("1").avg().value()
					+ " "
					+ buckets.get(ai + 1).aggregations().get("1").avg().value());
			assertTrue(buckets.get(ai).aggregations().get("1").avg().value()
					> buckets.get(ai + 1).aggregations().get("1").avg().value());
		}
	}

	@Test
	public void testStackedTimeSeriesSearch() throws XPathExpressionException, ResponseException {
		TestUtils.seedOpenSearchData();
		LocalDateTime ldt = LocalDateTime.ofInstant(new Date().toInstant(), ZoneId.systemDefault())
				.minusDays(2);
		SearchJSON json = new SearchJSON(
				CUSTOMER_ID,
				SITE,
				Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()).getTime(),
				Date.from(ldt.minusDays(1).atZone(ZoneId.systemDefault()).toInstant())
						.getTime());
		json.setType(STS_SEARCH_TYPE);
		json.setTimeZone(ZonedDateTime.now().getZone().getId());
		SearchResponse response = OSComponent.search(json);
		assertNotNull(response.aggregations().get("2"));
		assertNotNull(((Aggregate) response.aggregations().get("2"))._get());
		List<DateHistogramBucket> buckets = ((Aggregate) response.aggregations().get("2"))
				.dateHistogram()
				.buckets()
				.array();
		assertFalse(buckets.isEmpty());
		for (int ai = 0; ai < buckets.size() - 1; ai++) {
			assertNotNull(buckets.get(0).aggregations().get("terms"));
			assertNotNull(buckets.get(0).aggregations().get("terms").sterms().buckets());
			assertNotNull(buckets.get(0)
					.aggregations()
					.get("terms")
					.sterms()
					.buckets()
					.array()
					.get(0)
					.aggregations()
					.get("1"));
			assertTrue(buckets.get(ai)
							.aggregations()
							.get("terms")
							.sterms()
							.buckets()
							.array()
							.get(0)
							.aggregations()
							.get("1")
							.avg()
							.value()
					> buckets.get(ai + 1)
							.aggregations()
							.get("terms")
							.sterms()
							.buckets()
							.array()
							.get(0)
							.aggregations()
							.get("1")
							.avg()
							.value());
		}
	}

	@Test
	public void testMaxCurrentSearch() throws XPathExpressionException, ResponseException {
		TestUtils.seedOpenSearchData();
		LocalDateTime ldt = LocalDateTime.ofInstant(new Date().toInstant(), ZoneId.systemDefault())
				.minusDays(2);
		SearchJSON json = new SearchJSON(
				CUSTOMER_ID,
				deviceName + 0,
				Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()).getTime(),
				Date.from(ldt.minusDays(7).atZone(ZoneId.systemDefault()).toInstant())
						.getTime());
		json.setType(MC_SEARCH_TYPE);
		json.setTimeZone(ZonedDateTime.now().getZone().getId());
		SearchResponse response = OSComponent.search(json);
		assertEquals(
				90.0, ((Aggregate) response.aggregations().get("max")).max().value());
		assertFalse(response.hits().hits().isEmpty());
		assertEquals(
				"[0.0]",
				((Hit) response.hits().hits().get(0))
						.fields()
						.get(MeterConstants.TOTAL_REAL_POWER)
						.toString());
		assertFalse(response.hits().hits().isEmpty());
	}

	@Test
	public void testAverageTotalSearch() throws XPathExpressionException, ResponseException {
		TestUtils.seedOpenSearchData();
		LocalDateTime ldt = LocalDateTime.ofInstant(new Date().toInstant(), ZoneId.systemDefault())
				.minusDays(2);
		SearchJSON json = new SearchJSON(
				CUSTOMER_ID,
				deviceName + 0,
				Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()).getTime(),
				Date.from(ldt.minusDays(1).atZone(ZoneId.systemDefault()).toInstant())
						.getTime());
		json.setType(AT_SEARCH_TYPE);
		json.setTimeZone(ZonedDateTime.now().getZone().getId());
		SearchResponse response = OSComponent.search(json);
		assertEquals(2, response.aggregations().size());
		assertEquals(
				-9.7032755E7,
				((Aggregate) response.aggregations().get("total")).sum().value());
		assertEquals(
				45.0, ((Aggregate) response.aggregations().get("avg")).avg().value());
	}

	@Test
	public void testAddingNewDeviceViaDataPush() throws XPathExpressionException, ResponseException {
		String deviceName = TestConstants.deviceName + "shouldNotExist";
		assertFalse(deviceComponent.getDevicesForCustomerId(CUSTOMER_ID).stream()
				.filter(d -> !d.isVirtual())
				.anyMatch(device -> device.getDeviceName().equalsIgnoreCase(deviceName)));
		LocalDateTime ldt = LocalDateTime.ofInstant(
						TimeUtils.get15mRoundedDate().toInstant(), ZoneId.systemDefault())
				.minusDays(2);
		generationComponent.handleDeviceBody(
				TestUtils.getDeviceXML(
						deviceName,
						Date.from(ldt.minusMinutes(15)
								.atZone(ZoneId.systemDefault())
								.toInstant()),
						5),
				CUSTOMER_ID);
		OpenSearchUtils.waitForIndexing();
		Device SNEDevice = deviceComponent.getDevicesForCustomerId(CUSTOMER_ID).stream()
				.filter(d -> !d.isVirtual())
				.filter(device -> device.getDeviceName().equalsIgnoreCase(deviceName))
				.findAny()
				.orElse(null);
		assertNotNull(SNEDevice);
		assertNull(SNEDevice.getName());
		assertNotNull(OSComponent.getLastDeviceEntry(
				SNEDevice.getDeviceName(),
				OpenSearchQueries.getDeviceNameQuery(SNEDevice.getDeviceName()),
				OpenSearchQueries.getCustomerIdQuery(CUSTOMER_ID)));
	}

	@Test
	public void virtualDevices() throws XPathExpressionException, ResponseException {
		TestUtils.seedOpenSearchData();
		LocalDateTime ldt = LocalDateTime.ofInstant(new Date().toInstant(), ZoneId.systemDefault())
				.minusDays(2);
		SearchJSON json = new SearchJSON(
				CUSTOMER_ID,
				null,
				Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()).getTime(),
				Date.from(ldt.minusDays(1).atZone(ZoneId.systemDefault()).toInstant())
						.getTime());
		json.setSize(500);
		json.setType(DATA_SEARCH_TYPE);
		json.setTimeZone(ZonedDateTime.now().getZone().getId());
		assertEquals(30, OSComponent.search(json).hits().hits().size());
		json.setVirtual(true);
		assertEquals(5, OSComponent.search(json).hits().hits().size());
	}
}
