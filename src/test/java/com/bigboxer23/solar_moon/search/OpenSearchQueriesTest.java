package com.bigboxer23.solar_moon.search;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.ingest.MeterConstants;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch._types.Script;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.DeleteByQueryRequest;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.UpdateByQueryRequest;

class OpenSearchQueriesTest implements OpenSearchConstants {

	@Test
	void testGetDeviceNameQuery() {
		String deviceName = "test-device";
		Query query = OpenSearchQueries.getDeviceNameQuery(deviceName);

		assertNotNull(query);
		String json = OpenSearchUtils.queryToJson(query);
		assertTrue(json.contains("match"));
		assertTrue(json.contains(deviceName));
	}

	@Test
	void testGetNotVirtual() {
		Query query = OpenSearchQueries.getNotVirtual();

		assertNotNull(query);
		String json = OpenSearchUtils.queryToJson(query);
		assertTrue(json.contains("exists"));
		assertTrue(json.contains(MeterConstants.VIRTUAL));
	}

	@Test
	void testGetIsVirtual() {
		Query query = OpenSearchQueries.getIsVirtual();

		assertNotNull(query);
		String json = OpenSearchUtils.queryToJson(query);
		assertTrue(json.contains("match"));
		assertTrue(json.contains(MeterConstants.VIRTUAL));
	}

	@Test
	void testGetInformationalErrors() {
		Query query = OpenSearchQueries.getInformationalErrors();

		assertNotNull(query);
		String json = OpenSearchUtils.queryToJson(query);
		assertTrue(json.contains("exists"));
		assertTrue(json.contains(MeterConstants.INFORMATIONAL_ERROR_STRING));
	}

	@Test
	void testGetNotIsSite() {
		Query query = OpenSearchQueries.getNotIsSite();

		assertNotNull(query);
		String json = OpenSearchUtils.queryToJson(query);
		assertTrue(json.contains("exists"));
		assertTrue(json.contains(MeterConstants.IS_SITE));
	}

	@Test
	void testGetIsSite() {
		Query query = OpenSearchQueries.getIsSite();

		assertNotNull(query);
		String json = OpenSearchUtils.queryToJson(query);
		assertTrue(json.contains("match"));
		assertTrue(json.contains(MeterConstants.IS_SITE));
	}

	@Test
	void testGetIsDaylight() {
		Query query = OpenSearchQueries.getIsDaylight();

		assertNotNull(query);
		String json = OpenSearchUtils.queryToJson(query);
		assertTrue(json.contains("match"));
		assertTrue(json.contains(MeterConstants.DAYLIGHT));
	}

	@Test
	void testGetDeviceIdQuery() {
		String deviceId = "device-123";
		Query query = OpenSearchQueries.getDeviceIdQuery(deviceId);

		assertNotNull(query);
		String json = OpenSearchUtils.queryToJson(query);
		assertTrue(json.contains("match"));
		assertTrue(json.contains(deviceId));
	}

	@Test
	void testGetCustomerIdQuery() {
		String customerId = "customer-456";
		Query query = OpenSearchQueries.getCustomerIdQuery(customerId);

		assertNotNull(query);
		String json = OpenSearchUtils.queryToJson(query);
		assertTrue(json.contains("match"));
		assertTrue(json.contains(customerId));
	}

	@Test
	void testGetSiteQuery() {
		String site = "test-site";
		Query query = OpenSearchQueries.getSiteQuery(site);

		assertNotNull(query);
		String json = OpenSearchUtils.queryToJson(query);
		assertTrue(json.contains("match"));
		assertTrue(json.contains(site));
	}

	@Test
	void testGetSiteIdQuery() {
		String siteId = "site-789";
		Query query = OpenSearchQueries.getSiteIdQuery(siteId);

		assertNotNull(query);
		String json = OpenSearchUtils.queryToJson(query);
		assertTrue(json.contains("match"));
		assertTrue(json.contains(siteId));
	}

	@Test
	void testIsErrorLog() {
		Query query = OpenSearchQueries.isErrorLog();

		assertNotNull(query);
		String json = OpenSearchUtils.queryToJson(query);
		assertTrue(json.contains("match"));
		assertTrue(json.contains("ERROR"));
	}

	@Test
	void testGetErrorLogSearch() {
		Date startDate = new Date(System.currentTimeMillis() - 3600000);
		Date endDate = new Date();

		var queries = OpenSearchQueries.getErrorLogSearch(startDate, endDate);

		assertNotNull(queries);
		assertEquals(2, queries.size());
	}

	@Test
	void testGetDateRangeQuery_singleDate() {
		Date date = new Date();

		Query query = OpenSearchQueries.getDateRangeQuery(date);

		assertNotNull(query);
		String json = OpenSearchUtils.queryToJson(query);
		assertTrue(json.contains("range"));
		assertTrue(json.contains(TIMESTAMP));
	}

	@Test
	void testGetDateRangeQuery_dateRange() {
		Date startDate = new Date(System.currentTimeMillis() - 3600000);
		Date endDate = new Date();

		Query query = OpenSearchQueries.getDateRangeQuery(startDate, endDate);

		assertNotNull(query);
		String json = OpenSearchUtils.queryToJson(query);
		assertTrue(json.contains("range"));
		assertTrue(json.contains(TIMESTAMP));
	}

	@Test
	void testGetLast15MinQuery() {
		Query query = OpenSearchQueries.getLast15MinQuery();

		assertNotNull(query);
		String json = OpenSearchUtils.queryToJson(query);
		assertTrue(json.contains("range"));
		assertTrue(json.contains("now-15m"));
	}

	@Test
	void testGetLargeEnergyConsumedQuery() {
		Query query = OpenSearchQueries.getLargeEnergyConsumedQuery();

		assertNotNull(query);
		String json = OpenSearchUtils.queryToJson(query);
		assertTrue(json.contains("range"));
		assertTrue(json.contains(MeterConstants.ENG_CONS));
	}

	@Test
	void testGetElasticDocumentIdQuery() {
		String id = "doc-123";

		Query query = OpenSearchQueries.getElasticDocumentIdQuery(id);

		assertNotNull(query);
		String json = OpenSearchUtils.queryToJson(query);
		assertTrue(json.contains("match"));
		assertTrue(json.contains(id));
	}

	@Test
	void testGetUpdateScript_withStringValue() {
		Script script = OpenSearchQueries.getUpdateScript("fieldName", "stringValue");

		assertNotNull(script);
		assertNotNull(script.inline());
		assertTrue(script.inline().source().contains("ctx._source"));
		assertTrue(script.inline().source().contains("fieldName"));
	}

	@Test
	void testGetUpdateScript_withIntegerValue() {
		Script script = OpenSearchQueries.getUpdateScript("count", 42);

		assertNotNull(script);
		assertNotNull(script.inline());
		assertTrue(script.inline().source().contains("ctx._source"));
		assertTrue(script.inline().source().contains("count"));
	}

	@Test
	void testGetSearchRequestBuilder() {
		SearchRequest.Builder builder = OpenSearchQueries.getSearchRequestBuilder();

		assertNotNull(builder);
		SearchRequest request = builder.build();
		assertNotNull(request.index());
		assertTrue(request.index().contains(INDEX_NAME));
	}

	@Test
	void testGetLogSearchRequestBuilder() {
		SearchRequest.Builder builder = OpenSearchQueries.getLogSearchRequestBuilder();

		assertNotNull(builder);
		SearchRequest request = builder.build();
		assertNotNull(request.index());
		assertTrue(request.index().getFirst().contains(LOGS_INDEX_NAME));
	}

	@Test
	void testGetUpdateByQueryRequestBuilder() {
		UpdateByQueryRequest.Builder builder = OpenSearchQueries.getUpdateByQueryRequestBuilder();

		assertNotNull(builder);
		UpdateByQueryRequest request = builder.build();
		assertNotNull(request.index());
		assertTrue(request.index().contains(INDEX_NAME));
	}

	@Test
	void testGetDeleteRequestBuilder() {
		DeleteByQueryRequest.Builder builder = OpenSearchQueries.getDeleteRequestBuilder();

		assertNotNull(builder);
		DeleteByQueryRequest request = builder.build();
		assertNotNull(request.index());
		assertTrue(request.index().contains(INDEX_NAME));
	}

	@Test
	void testGetKeywordField() {
		String field = "testField";

		String keywordField = OpenSearchQueries.getKeywordField(field);

		assertEquals("testField.keyword", keywordField);
	}

	@Test
	void testSortByTimeStampDesc() {
		SortOptions sortOptions = OpenSearchQueries.sortByTimeStampDesc();

		assertNotNull(sortOptions);
		assertNotNull(sortOptions.field());
		assertEquals(TIMESTAMP, sortOptions.field().field());
	}

	@Test
	void testGetLogSearchBuilder() {
		SearchRequest.Builder builder = OpenSearchQueries.getLogSearchBuilder(25);

		assertNotNull(builder);
		SearchRequest request = builder.build();
		assertEquals(25, request.size());
		assertNotNull(request.sort());
		assertFalse(request.sort().isEmpty());
	}

	@Test
	void testGetWeatherSummaryFacet() {
		SearchRequest.Builder builder = OpenSearchQueries.getWeatherSummaryFacet();

		assertNotNull(builder);
		SearchRequest request = builder.build();
		assertEquals(0, request.size());
		assertNotNull(request.aggregations());
		assertTrue(request.aggregations().containsKey("terms"));
	}

	@Test
	void testAppendInformationErrorsFacet() {
		SearchRequest.Builder builder = OpenSearchQueries.getSearchRequestBuilder();

		SearchRequest.Builder result = OpenSearchQueries.appendInformationErrorsFacet(builder);

		assertNotNull(result);
		SearchRequest request = result.build();
		assertNotNull(request.aggregations());
		assertTrue(request.aggregations().containsKey(MeterConstants.INFORMATIONAL_ERROR_STRING));
	}

	@Test
	void testGetDeviceIdFacet() {
		SearchRequest.Builder builder = OpenSearchQueries.getDeviceIdFacet();

		assertNotNull(builder);
		SearchRequest request = builder.build();
		assertEquals(0, request.size());
		assertNotNull(request.aggregations());
		assertTrue(request.aggregations().containsKey("terms"));
	}

	@Test
	void testGetDataSearch_withIncludeSource() {
		SearchRequest.Builder builder = OpenSearchQueries.getDataSearch(0, 100, true, false);

		assertNotNull(builder);
		SearchRequest request = builder.build();
		assertEquals(100, request.size());
		assertEquals(0, request.from());
		assertNotNull(request.source());
	}

	@Test
	void testGetDataSearch_withoutIncludeSource() {
		SearchRequest.Builder builder = OpenSearchQueries.getDataSearch(10, 50, false, true);

		assertNotNull(builder);
		SearchRequest request = builder.build();
		assertEquals(50, request.size());
		assertEquals(10, request.from());
	}

	@Test
	void testGetDataSearch_reportSize() {
		SearchRequest.Builder builder = OpenSearchQueries.getDataSearch(0, 500, true, false);

		assertNotNull(builder);
		SearchRequest request = builder.build();
		assertEquals(500, request.size());
		assertNotNull(request.docvalueFields());
	}

	@Test
	void testGetStackedTimeSeriesBuilder() {
		SearchRequest.Builder builder = OpenSearchQueries.getStackedTimeSeriesBuilder("UTC", "1h");

		assertNotNull(builder);
		SearchRequest request = builder.build();
		assertEquals(0, request.size());
		assertNotNull(request.aggregations());
		assertTrue(request.aggregations().containsKey("2"));
	}

	@Test
	void testGetTimeSeriesBuilder() {
		SearchRequest.Builder builder = OpenSearchQueries.getTimeSeriesBuilder("America/New_York", "1d");

		assertNotNull(builder);
		SearchRequest request = builder.build();
		assertEquals(0, request.size());
		assertNotNull(request.aggregations());
		assertTrue(request.aggregations().containsKey("2"));
	}

	@Test
	void testGetTimeSeriesMaxBuilder() {
		SearchRequest.Builder builder = OpenSearchQueries.getTimeSeriesMaxBuilder("Europe/London", "15m");

		assertNotNull(builder);
		SearchRequest request = builder.build();
		assertEquals(0, request.size());
		assertNotNull(request.aggregations());
		assertTrue(request.aggregations().containsKey("2"));
	}

	@Test
	void testGetMaxCurrentBuilder() {
		SearchRequest.Builder builder = OpenSearchQueries.getMaxCurrentBuilder();

		assertNotNull(builder);
		SearchRequest request = builder.build();
		assertEquals(1, request.size());
		assertNotNull(request.aggregations());
		assertTrue(request.aggregations().containsKey("max"));
	}

	@Test
	void testGetMaxEnergyConsumed() {
		SearchRequest.Builder builder = OpenSearchQueries.getMaxEnergyConsumed();

		assertNotNull(builder);
		SearchRequest request = builder.build();
		assertEquals(0, request.size());
		assertNotNull(request.aggregations());
		assertTrue(request.aggregations().containsKey("max"));
	}

	@Test
	void testGetAverageTotalBuilder() {
		SearchRequest.Builder builder = OpenSearchQueries.getAverageTotalBuilder("UTC", "1h");

		assertNotNull(builder);
		SearchRequest request = builder.build();
		assertEquals(0, request.size());
		assertNotNull(request.aggregations());
		assertTrue(request.aggregations().containsKey("avg"));
		assertTrue(request.aggregations().containsKey("total"));
	}

	@Test
	void testGetAverageBuilder() {
		SearchRequest.Builder builder = OpenSearchQueries.getAverageBuilder("UTC", "1h");

		assertNotNull(builder);
		SearchRequest request = builder.build();
		assertEquals(0, request.size());
		assertNotNull(request.aggregations());
		assertTrue(request.aggregations().containsKey("avg"));
	}

	@Test
	void testGetTotalBuilder() {
		SearchRequest.Builder builder = OpenSearchQueries.getTotalBuilder("UTC", "1h");

		assertNotNull(builder);
		SearchRequest request = builder.build();
		assertEquals(0, request.size());
		assertNotNull(request.aggregations());
		assertTrue(request.aggregations().containsKey("total"));
	}

	@Test
	void testGetTotalEnergyConsumedBuilder() {
		SearchRequest.Builder builder = OpenSearchQueries.getTotalEnergyConsumedBuilder("UTC", "1h");

		assertNotNull(builder);
		SearchRequest request = builder.build();
		assertEquals(0, request.size());
		assertNotNull(request.aggregations());
		assertTrue(request.aggregations().containsKey("2"));
	}
}
