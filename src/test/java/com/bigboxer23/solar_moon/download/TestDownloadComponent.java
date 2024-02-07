package com.bigboxer23.solar_moon.download;

import static com.bigboxer23.solar_moon.search.OpenSearchConstants.DATA_SEARCH_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.TestConstants;
import com.bigboxer23.solar_moon.search.SearchJSON;
import com.bigboxer23.solar_moon.util.TimeConstants;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/** */
public class TestDownloadComponent implements IComponentRegistry, TestConstants {
	@Test
	public void getPageSizeDays() {
		assertEquals(104, downloadComponent.getPageSizeDays(1));
		assertEquals(6, downloadComponent.getPageSizeDays(16));
		assertEquals(1, downloadComponent.getPageSizeDays(104));
		assertEquals(0, downloadComponent.getPageSizeDays(105));
		assertEquals(0, downloadComponent.getPageSizeDays(0));
	}

	@Test
	public void download() throws IOException {
		SearchJSON search = new SearchJSON();
		search.setTimeZone("America/Chicago");
		search.setEndDate(System.currentTimeMillis());
		search.setStartDate(System.currentTimeMillis() - (7 * TimeConstants.DAY));
		search.setType(DATA_SEARCH_TYPE);
		search.setCustomerId(CUSTOMER_ID);
		search.setType(DATA_SEARCH_TYPE);
		search.setSize(10000);
		downloadComponent.download(search);
	}
}
