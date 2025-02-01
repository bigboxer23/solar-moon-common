package com.bigboxer23.solar_moon.search;

import static com.bigboxer23.solar_moon.search.OpenSearchConstants.*;
import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.TestConstants;
import com.bigboxer23.solar_moon.TestUtils;
import com.bigboxer23.solar_moon.alarm.ISolectriaConstants;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.ingest.MeterConstants;
import com.bigboxer23.solar_moon.util.TimeConstants;
import com.bigboxer23.solar_moon.util.TimeUtils;
import java.io.IOException;
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
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch.core.SearchResponse;

/** */
public class TestOpenSearchComponent implements IComponentRegistry, TestConstants, ISolectriaConstants {

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
	public void getDevices() throws IOException, XPathExpressionException {
		TestUtils.seedOpenSearchData();
		SearchJSON search = new SearchJSON();
		search.setEndDate(System.currentTimeMillis());
		search.setStartDate(System.currentTimeMillis() - (7 * TimeConstants.DAY));
		search.setType(DATA_SEARCH_TYPE);
		search.setCustomerId(CUSTOMER_ID);

		assertEquals(6, OSComponent.getDevicesFacet(search).size());

		search.setSite("fake site");
		assertEquals(0, OSComponent.getDevicesFacet(search).size());

		search.setSite(null);
		search.setSiteId(TestUtils.getSite().getId());
		assertEquals(6, OSComponent.getDevicesFacet(search).size());

		search.setSiteId(null);
		search.setDeviceId("fake device");
		assertEquals(0, OSComponent.getDevicesFacet(search).size());

		search.setDeviceId(TestUtils.getDevice().getId());
		assertEquals(1, OSComponent.getDevicesFacet(search).size());

		search.setStartDate(System.currentTimeMillis() - TimeConstants.HOUR);
		assertEquals(0, OSComponent.getDevicesFacet(search).size());
	}

	@Test
	public void getWeatherFacets() throws IOException {
		List<StringTermsBucket> terms = OSComponent.getWeatherFacets();
		assertFalse(terms.isEmpty());
		terms.forEach(term -> System.out.println(term.key() + ":" + term.docCount()));
	}

	@Test
	public void testGetLastDeviceEntry() throws XPathExpressionException, ResponseException {
		Date date = TimeUtils.get15mRoundedDate();
		LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
		Date prevDate =
				Date.from(ldt.minusMinutes(30).atZone(ZoneId.systemDefault()).toInstant());
		Date nextDate =
				Date.from(ldt.plusMinutes(15).atZone(ZoneId.systemDefault()).toInstant());
		obviousIngestComponent.handleDeviceBody(TestUtils.getDeviceXML(deviceName + 0, prevDate, -1), CUSTOMER_ID);
		DeviceData data = OSComponent.getDeviceEntryWithinLast15Min(
				CUSTOMER_ID, TestUtils.getDevice().getId());
		assertNull(data);
		obviousIngestComponent.handleDeviceBody(TestUtils.getDeviceXML(deviceName + 0, nextDate, -1), CUSTOMER_ID);
		data = OSComponent.getDeviceEntryWithinLast15Min(
				CUSTOMER_ID, TestUtils.getDevice().getId());
		assertNull(data);
		obviousIngestComponent.handleDeviceBody(TestUtils.getDeviceXML(deviceName + 0, date, -1), CUSTOMER_ID);
		data = OSComponent.getDeviceEntryWithinLast15Min(
				CUSTOMER_ID, TestUtils.getDevice().getId());
		assertNotNull(data);
		assertTrue(data.isValid());
		assertNotNull(data.getCustomerId());
		assertEquals(CUSTOMER_ID, data.getCustomerId());
	}

	@Test
	public void testGetDeviceByTimePeriod() throws XPathExpressionException, InterruptedException, ResponseException {
		Date date = TimeUtils.get15mRoundedDate();
		assertNull(OSComponent.getDeviceByTimePeriod(
				CUSTOMER_ID, TestUtils.getDevice().getId(), date));

		LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
		Date past =
				Date.from(ldt.minusMinutes(15).atZone(ZoneId.systemDefault()).toInstant());
		Date future =
				Date.from(ldt.plusMinutes(15).atZone(ZoneId.systemDefault()).toInstant());

		obviousIngestComponent.handleDeviceBody(
				TestUtils.getDeviceXML(TestUtils.getDevice().getDeviceName(), past, -1), CUSTOMER_ID);
		obviousIngestComponent.handleDeviceBody(
				TestUtils.getDeviceXML(TestUtils.getDevice().getDeviceName(), future, -1), CUSTOMER_ID);
		OpenSearchUtils.waitForIndexing();
		assertNull(OSComponent.getDeviceByTimePeriod(
				CUSTOMER_ID, TestUtils.getDevice().getDeviceName(), date));
		TestUtils.validateDateData(future);
		TestUtils.validateDateData(past);
		obviousIngestComponent.handleDeviceBody(
				TestUtils.getDeviceXML(TestUtils.getDevice().getDeviceName(), date, -1), CUSTOMER_ID);
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
		obviousIngestComponent.handleDeviceBody(
				TestUtils.getDeviceXML(TestUtils.getDevice().getDeviceName(), date, -1), CUSTOMER_ID);
		obviousIngestComponent.handleDeviceBody(
				TestUtils.getDeviceXML(TestUtils.getDevice().getDeviceName(), prevDate, -1), CUSTOMER_ID);
		obviousIngestComponent.handleDeviceBody(
				TestUtils.getDeviceXML(TestUtils.getDevice().getDeviceName(), nextDate, -1), CUSTOMER_ID);
		assertEquals(
				1,
				OSComponent.getSiteDevicesCountByTimePeriod(
						CUSTOMER_ID, TestUtils.getSite().getId(), date));
		assertEquals(
				1,
				OSComponent.getSiteDevicesCountByTimePeriod(
						CUSTOMER_ID, TestUtils.getSite().getId(), prevDate));
		assertEquals(
				1,
				OSComponent.getSiteDevicesCountByTimePeriod(
						CUSTOMER_ID, TestUtils.getSite().getId(), nextDate));
		obviousIngestComponent.handleDeviceBody(
				TestUtils.getDeviceXML(TestUtils.getDevice().getDeviceName(), nextDate, -1), CUSTOMER_ID);
		assertEquals(
				1,
				OSComponent.getSiteDevicesCountByTimePeriod(
						CUSTOMER_ID, TestUtils.getSite().getId(), date));
		assertEquals(
				1,
				OSComponent.getSiteDevicesCountByTimePeriod(
						CUSTOMER_ID, TestUtils.getSite().getId(), prevDate));
		assertEquals(
				1,
				OSComponent.getSiteDevicesCountByTimePeriod(
						CUSTOMER_ID,
						TestUtils.getSite().getId(),
						nextDate)); // Shouldn't stamp dup'd value for same device/date
	}

	@Test
	public void testTimeSeriesSearch() throws XPathExpressionException, ResponseException {
		TestUtils.seedOpenSearchData();
		LocalDateTime ldt = LocalDateTime.ofInstant(new Date().toInstant(), ZoneId.systemDefault())
				.minusDays(2);
		SearchJSON json = new SearchJSON(
				CUSTOMER_ID,
				TestUtils.getDevice().getId(),
				Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()).getTime(),
				Date.from(ldt.minusDays(1).atZone(ZoneId.systemDefault()).toInstant())
						.getTime());
		json.setType(TIME_SERIES_SEARCH_TYPE);
		json.setTimeZone(ZonedDateTime.now().getZone().getId());
		SearchResponse<DeviceData> response = OSComponent.search(json);
		assertNotNull(response.aggregations().get("2"));
		assertNotNull(response.aggregations().get("2")._get());
		List<DateHistogramBucket> buckets =
				response.aggregations().get("2").dateHistogram().buckets().array();
		for (int ai = 0; ai < buckets.size() - 1; ai++) {
			System.out.println(buckets.get(ai).aggregations().get("1").avg().value()
					+ " "
					+ buckets.get(ai + 1).aggregations().get("1").avg().value());
			assertTrue(buckets.get(ai).aggregations().get("1").avg().value()
					> buckets.get(ai + 1).aggregations().get("1").avg().value());
		}
	}

	@Test
	public void testTimeSeriesWithErrorsSearch() throws XPathExpressionException, ResponseException {
		TestUtils.seedOpenSearchData();
		LocalDateTime ldt = LocalDateTime.ofInstant(new Date().toInstant(), ZoneId.systemDefault())
				.minusDays(2);
		SearchJSON json = new SearchJSON(
				CUSTOMER_ID,
				TestUtils.getDevice().getId(),
				Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()).getTime(),
				Date.from(ldt.minusDays(1).atZone(ZoneId.systemDefault()).toInstant())
						.getTime());
		json.setType(TIME_SERIES_WITH_ERRORS_SEARCH_TYPE);
		json.setTimeZone(ZonedDateTime.now().getZone().getId());
		SearchResponse<DeviceData> response = OSComponent.search(json);
		assertNotNull(response.aggregations().get(MeterConstants.INFORMATIONAL_ERROR_STRING));
		assertNotNull(response.aggregations()
				.get(MeterConstants.INFORMATIONAL_ERROR_STRING)
				._get());
		List<StringTermsBucket> terms = response.aggregations()
				.get(MeterConstants.INFORMATIONAL_ERROR_STRING)
				.sterms()
				.buckets()
				.array();
		assertFalse(terms.isEmpty());
		assertEquals(1, terms.size());
		assertEquals(5, terms.getFirst().docCount());
		assertEquals(
				INFORMATIVE_ERROR_CODES.get(Fan_Life_Reached), terms.getFirst().key());
	}

	@Test
	public void testStackedTimeSeriesSearch() throws XPathExpressionException, ResponseException {
		TestUtils.seedOpenSearchData();
		LocalDateTime ldt = LocalDateTime.ofInstant(new Date().toInstant(), ZoneId.systemDefault())
				.minusDays(2);
		SearchJSON json = new SearchJSON(
				CUSTOMER_ID,
				TestUtils.getSite().getId(),
				Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()).getTime(),
				Date.from(ldt.minusDays(1).atZone(ZoneId.systemDefault()).toInstant())
						.getTime());
		json.setType(STACKED_TIME_SERIES_SEARCH_TYPE);
		json.setTimeZone(ZonedDateTime.now().getZone().getId());
		SearchResponse<DeviceData> response = OSComponent.search(json);
		assertNotNull(response.aggregations().get("2"));
		assertNotNull(response.aggregations().get("2")._get());
		List<DateHistogramBucket> buckets = ((Aggregate) response.aggregations().get("2"))
				.dateHistogram()
				.buckets()
				.array();
		assertFalse(buckets.isEmpty());
		for (int ai = 0; ai < buckets.size() - 1; ai++) {
			assertNotNull(buckets.getFirst().aggregations().get("terms"));
			assertNotNull(
					buckets.getFirst().aggregations().get("terms").sterms().buckets());
			assertNotNull(buckets.getFirst()
					.aggregations()
					.get("terms")
					.sterms()
					.buckets()
					.array()
					.getFirst()
					.aggregations()
					.get("1"));
			assertTrue(buckets.get(ai)
							.aggregations()
							.get("terms")
							.sterms()
							.buckets()
							.array()
							.getFirst()
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
							.getFirst()
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
				TestUtils.getDevice().getId(),
				Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()).getTime(),
				Date.from(ldt.minusDays(7).atZone(ZoneId.systemDefault()).toInstant())
						.getTime());
		json.setType(MAX_CURRENT_SEARCH_TYPE);
		json.setTimeZone(ZonedDateTime.now().getZone().getId());
		SearchResponse<DeviceData> response = OSComponent.search(json);
		assertEquals(
				40.0, ((Aggregate) response.aggregations().get("max")).max().value());
		assertFalse(response.hits().hits().isEmpty());
		assertEquals(
				"[0.0]",
				response.hits()
						.hits()
						.getFirst()
						.fields()
						.get(MeterConstants.TOTAL_REAL_POWER)
						.toString());
		assertFalse(response.hits().hits().isEmpty());

		json.setDeviceName(null);
		json.setDeviceId(TestUtils.getDevice().getId());
		response = OSComponent.search(json);
		assertEquals(40.0, response.aggregations().get("max").max().value());
		assertFalse(response.hits().hits().isEmpty());
		assertEquals(
				"[0.0]",
				response.hits()
						.hits()
						.getFirst()
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
				TestUtils.getDevice().getId(),
				Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()).getTime(),
				Date.from(ldt.minusDays(1).atZone(ZoneId.systemDefault()).toInstant())
						.getTime());
		json.setType(AVG_TOTAL_SEARCH_TYPE);
		json.setTimeZone(ZonedDateTime.now().getZone().getId());
		SearchResponse<DeviceData> response = OSComponent.search(json);
		assertEquals(2, response.aggregations().size());
		assertEquals(150, response.aggregations().get("total").sum().value());
		assertEquals(20, response.aggregations().get("avg").avg().value());
	}

	@Test
	public void testAddingNewDeviceViaDataPush() throws XPathExpressionException, ResponseException {
		String deviceName = TestConstants.deviceName + "shouldNotExist";
		assertFalse(deviceComponent.getDevicesForCustomerId(CUSTOMER_ID).stream()
				.filter(d -> !d.isDeviceSite())
				.anyMatch(device -> device.getDeviceName().equalsIgnoreCase(deviceName)));
		LocalDateTime ldt = LocalDateTime.ofInstant(
						TimeUtils.get15mRoundedDate().toInstant(), ZoneId.systemDefault())
				.minusDays(2);
		obviousIngestComponent.handleDeviceBody(
				TestUtils.getDeviceXML(
						deviceName,
						Date.from(ldt.minusMinutes(15)
								.atZone(ZoneId.systemDefault())
								.toInstant()),
						5),
				CUSTOMER_ID);
		OpenSearchUtils.waitForIndexing();
		Device SNEDevice = deviceComponent.getDevicesForCustomerId(CUSTOMER_ID).stream()
				.filter(d -> !d.isDeviceSite())
				.filter(device -> device.getDeviceName().equalsIgnoreCase(deviceName))
				.findAny()
				.orElse(null);
		assertNotNull(SNEDevice);
		assertNotNull(SNEDevice.getName());
		assertNotNull(OSComponent.getLastDeviceEntry(
				SNEDevice.getId(),
				OpenSearchQueries.getDeviceIdQuery(SNEDevice.getId()),
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
		json.setIsSite(true);
		assertEquals(5, OSComponent.search(json).hits().hits().size());
	}

	@Test
	public void searchFilterErrors() throws XPathExpressionException, ResponseException {
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
		json.setFilterErrors(true);
		assertEquals(5, OSComponent.search(json).hits().hits().size());
	}

	@Test
	public void isOpenSearchAvailable() {
		assertTrue(OSComponent.isOpenSearchAvailable());
	}

	@Test
	public void deleteOldLogs() {
		OSComponent.deleteOldLogs();
	}
}
