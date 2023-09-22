package com.bigboxer23.solar_moon.open_search;

import com.bigboxer23.solar_moon.MeterConstants;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.QueryBuilders;
import org.opensearch.client.opensearch.core.DeleteByQueryRequest;
import org.opensearch.client.opensearch.core.SearchRequest;

/** */
public class OpenSearchQueries implements OpenSearchConstants {
	public static Query getDeviceNameQuery(String deviceName) {
		return QueryBuilders.match()
				.field(getKeywordField(MeterConstants.DEVICE_NAME))
				.query(builder -> builder.stringValue(deviceName))
				.build()
				._toQuery();
	}

	public static Query getCustomerIdQuery(String customerId) {
		return QueryBuilders.match()
				.field(getKeywordField(MeterConstants.CUSTOMER_ID))
				.query(builder -> builder.stringValue(customerId))
				.build()
				._toQuery();
	}

	public static Query getSiteQuery(String site) {
		return QueryBuilders.match()
				.field(getKeywordField(MeterConstants.SITE))
				.query(builder -> builder.stringValue(site))
				.build()
				._toQuery();
	}

	public static Query getDateRangeQuery(Date date) {
		LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
		return QueryBuilders.range()
				.field(TIMESTAMP)
				.gte(JsonData.of(Date.from(
						ldt.minusMinutes(5).atZone(ZoneId.systemDefault()).toInstant())))
				.lte(JsonData.of(Date.from(
						ldt.plusMinutes(5).atZone(ZoneId.systemDefault()).toInstant())))
				.build()
				._toQuery();
	}

	public static Query getLast15MinQuery() {
		return QueryBuilders.range()
				.field(TIMESTAMP)
				.gte(JsonData.of("now-15m"))
				.lte(JsonData.of("now"))
				.build()
				._toQuery();
	}

	public static SearchRequest.Builder getSearchRequestBuilder() {
		return new SearchRequest.Builder().index(Collections.singletonList(INDEX_NAME));
	}

	public static DeleteByQueryRequest.Builder getDeleteRequestBuilder() {
		return new DeleteByQueryRequest.Builder().index(Collections.singletonList(INDEX_NAME));
	}

	public static String getKeywordField(String field) {
		return field + ".keyword";
	}
}
