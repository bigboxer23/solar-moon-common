package com.bigboxer23.solar_moon.upgrade;

import static com.bigboxer23.solar_moon.search.OpenSearchConstants.DATA_SEARCH_TYPE;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.TestConstants;
import com.bigboxer23.solar_moon.search.OpenSearchUtils;
import com.bigboxer23.solar_moon.search.SearchJSON;
import com.bigboxer23.solar_moon.util.TimeConstants;
import javax.xml.xpath.XPathExpressionException;
import org.opensearch.client.ResponseException;

/** */
public class OpenSearchDataUpgrade implements IComponentRegistry, TestConstants {

	public void updateByQuery() throws ResponseException, XPathExpressionException {
		SearchJSON search = new SearchJSON();
		search.setEndDate(System.currentTimeMillis());
		search.setStartDate(System.currentTimeMillis() - (7 * TimeConstants.DAY));
		search.setType(DATA_SEARCH_TYPE);
		search.setCustomerId(CUSTOMER_ID);
		System.out.println(OpenSearchUtils.queryToJson(OSComponent.updateByQuery(search, "siteId", "test")));
	}
}
