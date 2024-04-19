package com.bigboxer23.solar_moon.ingest.sma;

import static com.bigboxer23.solar_moon.search.OpenSearchConstants.DATA_SEARCH_TYPE;
import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.TestConstants;
import com.bigboxer23.solar_moon.TestUtils;
import com.bigboxer23.solar_moon.data.Customer;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.ingest.MeterConstants;
import com.bigboxer23.solar_moon.search.OpenSearchQueries;
import com.bigboxer23.solar_moon.search.OpenSearchUtils;
import com.bigboxer23.solar_moon.search.SearchJSON;
import com.bigboxer23.solar_moon.util.TimeConstants;
import com.bigboxer23.utils.properties.PropertyUtils;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.xml.xpath.XPathExpressionException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/** */
public class TestSMAIngestComponent implements TestConstants, IComponentRegistry, MeterConstants {
	private final File t120000 = new File("target/test-files/120000.xml");
	private final File t121500 = new File("target/test-files/121500.xml");
	private final File t123000 = new File("target/test-files/123000.xml");

	private final File t231500 = new File("target/test-files/231500.xml");

	private final File tAdditionalDevice = new File("target/test-files/additionalDevice.xml");

	private final Map<String, File> testFiles = new HashMap<>() {
		{
			put("test/120000.xml", t120000);
			put("test/121500.xml", t121500);
			put("test/123000.xml", t123000);
			put("test/231500.xml", t231500);
			put("test/additionalDevice.xml", tAdditionalDevice);
		}
	};

	@AfterAll
	public static void cleanup() {
		TestUtils.nukeCustomerId(CUSTOMER_ID);
	}

	@BeforeEach
	public void fetchTestFiles() throws IOException {
		for (String url : testFiles.keySet()) {
			if (!testFiles.get(url).exists()) {
				ResponseBytes<GetObjectResponse> objectBytes = TestUtils.getS3Client()
						.getObjectAsBytes(GetObjectRequest.builder()
								.key(url)
								.bucket("solarmoonanalytics")
								.build());
				FileUtils.writeByteArrayToFile(testFiles.get(url), objectBytes.asByteArray());
			}
		}
	}

	private final String ADDED_DEVICE = "SN: XXXXX";

	/**
	 * Test to validate that if new device is added onto the existing site, we properly add it to
	 * the site
	 */
	@Test
	public void testNewDeviceGetsSite() throws IOException, XPathExpressionException {
		TestUtils.setupSite();
		subscriptionComponent.updateSubscription(CUSTOMER_ID, 3);
		smaIngestComponent.ingestXMLFile(xmlFileToString(t121500), CUSTOMER_ID);

		List<Device> devices = deviceComponent.getDevicesForCustomerId(CUSTOMER_ID);
		assertFalse(devices.isEmpty());
		assertEquals(36, devices.size());
		Device donor = devices.getFirst();

		smaIngestComponent.ingestXMLFile(xmlFileToString(tAdditionalDevice), CUSTOMER_ID);
		devices = deviceComponent.getDevicesForCustomerId(CUSTOMER_ID);
		assertEquals(37, devices.size());
		Optional<Device> newDevice = devices.stream()
				.filter(d -> ADDED_DEVICE.equals(d.getDeviceName()))
				.findFirst();
		assertTrue(newDevice.isPresent());
		assertEquals(donor.getSite(), newDevice.get().getSite());
		assertEquals(donor.getSiteId(), newDevice.get().getSiteId());
	}

	@Test
	public void ingest() throws IOException, XPathExpressionException, InterruptedException {
		TestUtils.setupSite();
		subscriptionComponent.updateSubscription(CUSTOMER_ID, 3);
		smaIngestComponent.ingestXMLFile(xmlFileToString(t120000), CUSTOMER_ID);

		List<Device> devices = deviceComponent.getDevicesForCustomerId(CUSTOMER_ID);
		assertFalse(devices.isEmpty());
		assertEquals(36, devices.size());
		smaIngestComponent.ingestXMLFile(xmlFileToString(t121500), CUSTOMER_ID);
		smaIngestComponent.ingestXMLFile(xmlFileToString(t123000), CUSTOMER_ID);

		SearchJSON search = new SearchJSON();
		search.setCustomerId(CUSTOMER_ID);
		Date march_06_2024 = new Date(1709705000000L);
		search.setEndDate(march_06_2024.getTime() + TimeConstants.DAY);
		search.setStartDate(march_06_2024.getTime());
		search.setType(DATA_SEARCH_TYPE);
		search.setSize(5000);

		OpenSearchUtils.waitForIndexing();

		SearchResponse response = OSComponent.search(search);
		assertEquals(90, response.hits().total().value());

		Map firstEntryFields = ((Hit) response.hits().hits().getFirst()).fields();
		assertTrue(firstEntryFields.containsKey(OpenSearchQueries.getKeywordField(SITE_ID)));
		assertTrue(firstEntryFields.containsKey(TOTAL_REAL_POWER));
		assertTrue(firstEntryFields.containsKey(ENG_CONS));
		assertTrue(firstEntryFields.containsKey(TOTAL_ENG_CONS));

		smaIngestComponent.ingestXMLFile(xmlFileToString(t231500), CUSTOMER_ID);
		OpenSearchUtils.waitForIndexing();

		response = OSComponent.search(search);
		assertEquals(120, response.hits().total().value());
	}

	@Test
	public void getDateFormatter() {
		customerComponent.deleteCustomerByCustomerId(CUSTOMER_ID);
		Optional<Customer> customer = TestUtils.setupCustomer();
		TestUtils.setupSite();

		TimeZone nativeZone = new SimpleDateFormat().getTimeZone();
		String testCustomerDefaultTZ = "America/New_York";
		String testSiteTZ = "America/Anchorage";

		assertEquals(nativeZone, SMAIngestComponent.getDateFormatter(null).getTimeZone());

		assertEquals(
				nativeZone,
				SMAIngestComponent.getDateFormatter(TestUtils.getDevice()).getTimeZone());

		assertTrue(customer.isPresent());
		customer.get().setDefaultTimezone(testCustomerDefaultTZ);
		customerComponent.updateCustomer(customer.get());

		assertEquals(
				testCustomerDefaultTZ,
				SMAIngestComponent.getDateFormatter(TestUtils.getDevice())
						.getTimeZone()
						.getID());

		Device site = TestUtils.getSite();
		site.setCity("Anchorage");
		site.setState("AK");
		site.setCountry("USA");
		deviceComponent.updateDevice(site);
		assertEquals(
				testSiteTZ,
				SMAIngestComponent.getDateFormatter(TestUtils.getDevice())
						.getTimeZone()
						.getID());
	}

	@Test
	public void handleAccessKeyChange() {
		Optional<Customer> customer = TestUtils.setupCustomer();
		assertTrue(customer.isPresent());
		smaIngestComponent.handleAccessKeyChange(null, null);
		smaIngestComponent.handleAccessKeyChange(null, customer.get().getAccessKey());
		File tmpTest = new File("target/" + customer.get().getAccessKey());
		tmpTest.delete();
		smaIngestComponent
				.getS3Client()
				.getObject(
						GetObjectRequest.builder()
								.bucket(PropertyUtils.getProperty("ftp.s3.bucket"))
								.key(customer.get().getAccessKey() + "/")
								.build(),
						tmpTest.toPath());
		smaIngestComponent
				.getS3Client()
				.deleteObject(DeleteObjectRequest.builder()
						.bucket(PropertyUtils.getProperty("ftp.s3.bucket"))
						.key(customer.get().getAccessKey() + "/")
						.build());
	}

	/** Strip potential BOM from file */
	private String xmlFileToString(File file) throws IOException {
		try (BOMInputStream stream = BOMInputStream.builder()
				.setInputStream(new FileInputStream(file))
				.get()) {
			return IOUtils.toString(stream, StandardCharsets.UTF_8);
		}
	}
}
