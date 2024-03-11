package com.bigboxer23.solar_moon.ingest.sma;

import static com.bigboxer23.solar_moon.search.OpenSearchConstants.DATA_SEARCH_TYPE;
import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.TestConstants;
import com.bigboxer23.solar_moon.TestUtils;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.ingest.MeterConstants;
import com.bigboxer23.solar_moon.search.OpenSearchQueries;
import com.bigboxer23.solar_moon.search.OpenSearchUtils;
import com.bigboxer23.solar_moon.search.SearchJSON;
import com.bigboxer23.solar_moon.util.TimeConstants;
import java.io.*;
import java.util.*;
import javax.xml.xpath.XPathExpressionException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/** */
public class TestSMAIngestComponent implements TestConstants, IComponentRegistry, MeterConstants {
	private final File t120000 = new File("target/test-files/120000.xml");
	private final File t121500 = new File("target/test-files/121500.xml");
	private final File t123000 = new File("target/test-files/123000.xml");

	private final File t231500 = new File("target/test-files/231500.xml");

	private final Map<String, File> testFiles = new HashMap<>() {
		{
			put("test/120000.xml", t120000);
			put("test/121500.xml", t121500);
			put("test/123000.xml", t123000);
			put("test/231500.xml", t231500);
		}
	};

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

	@Test
	public void ingest() throws IOException, XPathExpressionException {
		TestUtils.setupSite();
		subscriptionComponent.updateSubscription(CUSTOMER_ID, 3);
		smaIngestComponent.ingestXMLFile(IOUtils.toString(new FileReader(t120000)), CUSTOMER_ID);

		List<Device> devices = deviceComponent.getDevicesForCustomerId(CUSTOMER_ID);
		assertFalse(devices.isEmpty());
		assertEquals(36, devices.size());
		smaIngestComponent.ingestXMLFile(IOUtils.toString(new FileReader(t121500)), CUSTOMER_ID);
		smaIngestComponent.ingestXMLFile(IOUtils.toString(new FileReader(t123000)), CUSTOMER_ID);

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

		smaIngestComponent.ingestXMLFile(IOUtils.toString(new FileReader(t231500)), CUSTOMER_ID);
		OpenSearchUtils.waitForIndexing();

		response = OSComponent.search(search);
		assertEquals(120, response.hits().total().value());
	}
}
