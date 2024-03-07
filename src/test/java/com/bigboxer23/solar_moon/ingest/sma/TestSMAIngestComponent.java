package com.bigboxer23.solar_moon.ingest.sma;

import static com.bigboxer23.solar_moon.search.OpenSearchConstants.DATA_SEARCH_TYPE;
import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.TestConstants;
import com.bigboxer23.solar_moon.TestUtils;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.device.DeviceComponent;
import com.bigboxer23.solar_moon.ingest.MeterConstants;
import com.bigboxer23.solar_moon.search.OpenSearchQueries;
import com.bigboxer23.solar_moon.search.SearchJSON;
import com.bigboxer23.solar_moon.util.TimeConstants;
import java.io.*;
import java.net.URI;
import java.util.*;
import javax.xml.xpath.XPathExpressionException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;

/** */
public class TestSMAIngestComponent implements TestConstants, IComponentRegistry, MeterConstants {
	private final File t120000 = new File("target/test-files/120000.xml");
	private final File t121500 = new File("target/test-files/121500.xml");
	private final File t123000 = new File("target/test-files/123000.xml");

	private final Map<String, File> testFiles = new HashMap<>() {
		{
			put("https://solarmoonanalytics.s3.us-west-2.amazonaws.com/test/120000.xml", t120000);
			put("https://solarmoonanalytics.s3.us-west-2.amazonaws.com/test/121500.xml", t121500);
			put("https://solarmoonanalytics.s3.us-west-2.amazonaws.com/test/123000.xml", t123000);
		}
	};

	@BeforeEach
	public void fetchTestFiles() throws IOException {
		for (String url : testFiles.keySet()) {
			if (!testFiles.get(url).exists()) {
				FileUtils.copyURLToFile(URI.create(url).toURL(), testFiles.get(url));
			}
		}
	}

	@Test
	public void ingest() throws IOException, XPathExpressionException {
		TestUtils.setupSite();
		subscriptionComponent.updateSubscription(CUSTOMER_ID, 3);
		smaIngestComponent.ingestXMLFile(IOUtils.toString(new FileReader(t120000)), CUSTOMER_ID);

		List<Device> devices = deviceComponent.getDevicesForCustomerId(CUSTOMER_ID);
		assertFalse(devices.isEmpty());
		assertEquals(36, devices.size());
		devices.stream()
				.filter(device -> DeviceComponent.NO_SITE.equalsIgnoreCase(device.getSiteId()))
				.forEach(device -> {
					System.out.println("adjusting site");
					device.setSite(TestUtils.getSite().getDisplayName());
					device.setSiteId(TestUtils.getSite().getId());
					deviceComponent.updateDevice(device);
				});
		smaIngestComponent.ingestXMLFile(IOUtils.toString(new FileReader(t121500)), CUSTOMER_ID);
		smaIngestComponent.ingestXMLFile(IOUtils.toString(new FileReader(t123000)), CUSTOMER_ID);
		SearchJSON search = new SearchJSON();
		search.setCustomerId(CUSTOMER_ID);
		Date march_06_2024 = new Date(1709705000000L);
		search.setEndDate(march_06_2024.getTime() + TimeConstants.DAY);
		search.setStartDate(march_06_2024.getTime());
		search.setType(DATA_SEARCH_TYPE);
		search.setSize(5000);
		SearchResponse response = OSComponent.search(search);
		assertEquals(90, response.hits().total().value());

		Map firstEntryFields = ((Hit) response.hits().hits().getFirst()).fields();
		assertTrue(firstEntryFields.containsKey(OpenSearchQueries.getKeywordField(SITE_ID)));
		assertTrue(firstEntryFields.containsKey(TOTAL_REAL_POWER));
		assertTrue(firstEntryFields.containsKey(ENG_CONS));
		assertTrue(firstEntryFields.containsKey(TOTAL_ENG_CONS));
	}
}
