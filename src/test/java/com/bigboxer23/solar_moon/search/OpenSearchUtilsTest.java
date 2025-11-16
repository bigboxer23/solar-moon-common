package com.bigboxer23.solar_moon.search;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.QueryBuilders;

class OpenSearchUtilsTest {

	@Test
	void testQueryToJson_withMatchAllQuery() {
		Query query = QueryBuilders.matchAll().build().toQuery();

		String json = OpenSearchUtils.queryToJson(query);

		assertNotNull(json);
		assertTrue(json.contains("match_all"));
		assertTrue(json.startsWith("{"));
		assertTrue(json.endsWith("}"));
	}

	@Test
	void testQueryToJson_withMatchQuery() {
		Query query = QueryBuilders.match()
				.field("customerId")
				.query(builder -> builder.stringValue("test-customer"))
				.build()
				.toQuery();

		String json = OpenSearchUtils.queryToJson(query);

		assertNotNull(json);
		assertTrue(json.contains("match"));
		assertTrue(json.contains("customerId"));
		assertTrue(json.contains("test-customer"));
	}

	@Test
	void testQueryToJson_withBoolQuery() {
		Query query = QueryBuilders.bool()
				.must(QueryBuilders.match()
						.field("field1")
						.query(builder -> builder.stringValue("value1"))
						.build()
						.toQuery())
				.should(QueryBuilders.match()
						.field("field2")
						.query(builder -> builder.stringValue("value2"))
						.build()
						.toQuery())
				.build()
				.toQuery();

		String json = OpenSearchUtils.queryToJson(query);

		assertNotNull(json);
		assertTrue(json.contains("bool"));
		assertTrue(json.contains("must"));
		assertTrue(json.contains("should"));
		assertTrue(json.contains("field1"));
		assertTrue(json.contains("field2"));
	}

	@Test
	void testQueryToJson_withRangeQuery() {
		Query query = QueryBuilders.range()
				.field("date")
				.gte(org.opensearch.client.json.JsonData.of(1000L))
				.lte(org.opensearch.client.json.JsonData.of(2000L))
				.build()
				.toQuery();

		String json = OpenSearchUtils.queryToJson(query);

		assertNotNull(json);
		assertTrue(json.contains("range"));
		assertTrue(json.contains("date"));
	}

	@Test
	void testWaitForIndexing() {
		long startTime = System.currentTimeMillis();

		assertDoesNotThrow(OpenSearchUtils::waitForIndexing);

		long elapsedTime = System.currentTimeMillis() - startTime;
		assertTrue(elapsedTime >= 1500, "Should wait at least 1500ms, but waited " + elapsedTime + "ms");
		assertTrue(elapsedTime < 2000, "Should not wait more than 2000ms, but waited " + elapsedTime + "ms");
	}

	@Test
	void testQueryToJson_producesValidJson() {
		Query query = QueryBuilders.match()
				.field("testField")
				.query(builder -> builder.stringValue("testValue"))
				.build()
				.toQuery();

		String json = OpenSearchUtils.queryToJson(query);

		assertNotNull(json);
		assertFalse(json.isEmpty());
		assertTrue(json.trim().startsWith("{"));
		assertTrue(json.trim().endsWith("}"));
	}
}
