package com.bigboxer23.solar_moon;

import static com.bigboxer23.solar_moon.open_search.OpenSearchConstants.*;
import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.open_search.OpenSearchComponent;
import com.bigboxer23.solar_moon.open_search.OpenSearchQueries;
import com.bigboxer23.solar_moon.open_search.OpenSearchUtils;
import com.bigboxer23.solar_moon.open_search.SearchJSON;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import javax.xml.xpath.XPathExpressionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.DateHistogramBucket;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;

/** */
public class TestOpenSearchComponent {

	private static OpenSearchComponent OSComponent = new OpenSearchComponent();

	private static final SubscriptionComponent subscriptionComponent = new SubscriptionComponent();

	private static DeviceComponent deviceComponent = new DeviceComponent(subscriptionComponent);

	private static GenerationMeterComponent generationComponent = new GenerationMeterComponent(
			OSComponent,
			new AlarmComponent(new OpenWeatherComponent()),
			deviceComponent,
			new SiteComponent(OSComponent, deviceComponent));

	@BeforeEach
	public void setup() {
		TestUtils.setupSite(deviceComponent, OSComponent, subscriptionComponent);
	}

	@Test
	public void testGetTotalEnergyConsumed() {
		// test invalid case
		assertNull(OSComponent.getTotalEnergyConsumed(TestDeviceComponent.deviceName));
	}

	@Test
	public void testGetLastDeviceEntry() throws XPathExpressionException {
		Date date = TimeUtils.get15mRoundedDate();
		LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
		Date prevDate =
				Date.from(ldt.minusMinutes(30).atZone(ZoneId.systemDefault()).toInstant());
		Date nextDate =
				Date.from(ldt.plusMinutes(15).atZone(ZoneId.systemDefault()).toInstant());
		generationComponent.handleDeviceBody(
				TestUtils.getDeviceXML(TestDeviceComponent.deviceName + 0, prevDate, -1), TestDeviceComponent.clientId);
		DeviceData data =
				OSComponent.getLastDeviceEntry(TestDeviceComponent.clientId, TestDeviceComponent.deviceName + 0);
		assertNull(data);
		generationComponent.handleDeviceBody(
				TestUtils.getDeviceXML(TestDeviceComponent.deviceName + 0, nextDate, -1), TestDeviceComponent.clientId);
		data = OSComponent.getLastDeviceEntry(TestDeviceComponent.clientId, TestDeviceComponent.deviceName + 0);
		assertNull(data);
		generationComponent.handleDeviceBody(
				TestUtils.getDeviceXML(TestDeviceComponent.deviceName + 0, date, -1), TestDeviceComponent.clientId);
		data = OSComponent.getLastDeviceEntry(TestDeviceComponent.clientId, TestDeviceComponent.deviceName + 0);
		assertNotNull(data);
		assertTrue(data.isValid());
		assertNotNull(data.getCustomerId());
		assertEquals(TestDeviceComponent.clientId, data.getCustomerId());
	}

	@Test
	public void testGetDeviceByTimePeriod() throws XPathExpressionException, InterruptedException {
		Date date = TimeUtils.get15mRoundedDate();
		assertNull(OSComponent.getDeviceByTimePeriod(
				TestDeviceComponent.clientId, TestDeviceComponent.deviceName + 0, date));

		LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
		Date past =
				Date.from(ldt.minusMinutes(15).atZone(ZoneId.systemDefault()).toInstant());
		Date future =
				Date.from(ldt.plusMinutes(15).atZone(ZoneId.systemDefault()).toInstant());

		generationComponent.handleDeviceBody(
				TestUtils.getDeviceXML(TestDeviceComponent.deviceName + 0, past, -1), TestDeviceComponent.clientId);
		generationComponent.handleDeviceBody(
				TestUtils.getDeviceXML(TestDeviceComponent.deviceName + 0, future, -1), TestDeviceComponent.clientId);
		OpenSearchUtils.waitForIndexing();
		assertNull(OSComponent.getDeviceByTimePeriod(
				TestDeviceComponent.clientId, TestDeviceComponent.deviceName + 0, date));
		TestUtils.validateDateData(OSComponent, future);
		TestUtils.validateDateData(OSComponent, past);
		generationComponent.handleDeviceBody(
				TestUtils.getDeviceXML(TestDeviceComponent.deviceName + 0, date, -1), TestDeviceComponent.clientId);
		OpenSearchUtils.waitForIndexing();
		TestUtils.validateDateData(OSComponent, date);
	}

	@Test
	public void testGetDeviceCountByTimePeriod() throws XPathExpressionException {
		Date date = TimeUtils.get15mRoundedDate();
		LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
		Date prevDate =
				Date.from(ldt.minusMinutes(15).atZone(ZoneId.systemDefault()).toInstant());
		Date nextDate =
				Date.from(ldt.plusMinutes(15).atZone(ZoneId.systemDefault()).toInstant());
		generationComponent.handleDeviceBody(
				TestUtils.getDeviceXML(TestDeviceComponent.deviceName + 0, date, -1), TestDeviceComponent.clientId);
		generationComponent.handleDeviceBody(
				TestUtils.getDeviceXML(TestDeviceComponent.deviceName + 0, prevDate, -1), TestDeviceComponent.clientId);
		generationComponent.handleDeviceBody(
				TestUtils.getDeviceXML(TestDeviceComponent.deviceName + 0, nextDate, -1), TestDeviceComponent.clientId);
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
				TestUtils.getDeviceXML(TestDeviceComponent.deviceName + 0, nextDate, -1), TestDeviceComponent.clientId);
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

	@Test
	public void testTimeSeriesSearch() throws XPathExpressionException {
		TestUtils.seedOpenSearchData(generationComponent);
		LocalDateTime ldt = LocalDateTime.ofInstant(new Date().toInstant(), ZoneId.systemDefault())
				.minusDays(2);
		SearchJSON json = new SearchJSON(
				TestDeviceComponent.clientId,
				TestDeviceComponent.deviceName + 0,
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
			assertTrue(buckets.get(ai).aggregations().get("1").avg().value()
					> buckets.get(ai + 1).aggregations().get("1").avg().value());
		}
	}

	@Test
	public void testStackedTimeSeriesSearch() throws XPathExpressionException {
		TestUtils.seedOpenSearchData(generationComponent);
		LocalDateTime ldt = LocalDateTime.ofInstant(new Date().toInstant(), ZoneId.systemDefault())
				.minusDays(2);
		SearchJSON json = new SearchJSON(
				TestDeviceComponent.clientId,
				TestDeviceComponent.SITE,
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
	public void testMaxCurrentSearch() throws XPathExpressionException {
		TestUtils.seedOpenSearchData(generationComponent);
		LocalDateTime ldt = LocalDateTime.ofInstant(new Date().toInstant(), ZoneId.systemDefault())
				.minusDays(2);
		SearchJSON json = new SearchJSON(
				TestDeviceComponent.clientId,
				TestDeviceComponent.deviceName + 0,
				Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()).getTime(),
				Date.from(ldt.minusDays(7).atZone(ZoneId.systemDefault()).toInstant())
						.getTime());
		json.setType(MC_SEARCH_TYPE);
		json.setTimeZone(ZonedDateTime.now().getZone().getId());
		SearchResponse response = OSComponent.search(json);
		assertEquals(
				333.70001220703125,
				((Aggregate) response.aggregations().get("max")).max().value());
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
	public void testAverageTotalSearch() throws XPathExpressionException {
		TestUtils.seedOpenSearchData(generationComponent);
		LocalDateTime ldt = LocalDateTime.ofInstant(new Date().toInstant(), ZoneId.systemDefault())
				.minusDays(2);
		SearchJSON json = new SearchJSON(
				TestDeviceComponent.clientId,
				TestDeviceComponent.deviceName + 0,
				Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()).getTime(),
				Date.from(ldt.minusDays(1).atZone(ZoneId.systemDefault()).toInstant())
						.getTime());
		json.setType(AT_SEARCH_TYPE);
		json.setTimeZone(ZonedDateTime.now().getZone().getId());
		SearchResponse response = OSComponent.search(json);
		assertEquals(2, response.aggregations().size());
		assertEquals(0, ((Aggregate) response.aggregations().get("total")).sum().value());
		assertEquals(
				166.85000076293946,
				((Aggregate) response.aggregations().get("avg")).avg().value());
	}

	@Test
	public void testAddingNewDeviceViaDataPush() throws XPathExpressionException {
		String deviceName = TestDeviceComponent.deviceName + "shouldNotExist";
		assertFalse(deviceComponent.getDevices(TestDeviceComponent.clientId).stream()
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
				TestDeviceComponent.clientId);
		Device SNEDevice = deviceComponent.getDevices(TestDeviceComponent.clientId).stream()
				.filter(d -> !d.isVirtual())
				.filter(device -> device.getDeviceName().equalsIgnoreCase(deviceName))
				.findAny()
				.orElse(null);
		assertNotNull(SNEDevice);
		assertNull(SNEDevice.getName());

		assertNotNull(OSComponent.getLastDeviceEntry(
				SNEDevice.getDeviceName(),
				OpenSearchQueries.getDeviceNameQuery(SNEDevice.getDeviceName()),
				OpenSearchQueries.getCustomerIdQuery(TestDeviceComponent.clientId)));
	}
}
