package com.bigboxer23.solar_moon.search;

import com.bigboxer23.solar_moon.ingest.MeterConstants;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.FieldSort;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.Time;
import org.opensearch.client.opensearch._types.aggregations.*;
import org.opensearch.client.opensearch._types.query_dsl.FieldAndFormat;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.QueryBuilders;
import org.opensearch.client.opensearch.core.DeleteByQueryRequest;
import org.opensearch.client.opensearch.core.SearchRequest;

/** */
public class OpenSearchQueries implements OpenSearchConstants, MeterConstants {
	public static Query getDeviceNameQuery(String deviceName) {
		return QueryBuilders.match()
				.field(getKeywordField(DEVICE_NAME))
				.query(builder -> builder.stringValue(deviceName))
				.build()
				._toQuery();
	}

	public static Query getNotVirtual() {
		return QueryBuilders.exists().field(VIRTUAL).build()._toQuery();
	}

	public static Query getIsVirtual() {
		return QueryBuilders.match()
				.field(VIRTUAL)
				.query(builder -> builder.booleanValue(true))
				.build()
				._toQuery();
	}

	public static Query getNotIsSite() {
		return QueryBuilders.exists().field(IS_SITE).build()._toQuery();
	}

	public static Query getIsSite() {
		return QueryBuilders.match()
				.field(IS_SITE)
				.query(builder -> builder.booleanValue(true))
				.build()
				._toQuery();
	}

	public static Query getIsDaylight() {
		return QueryBuilders.match()
				.field(DAYLIGHT)
				.query(builder -> builder.booleanValue(true))
				.build()
				._toQuery();
	}

	public static Query getDeviceIdQuery(String id) {
		return QueryBuilders.match()
				.field(getKeywordField(DEVICE_ID))
				.query(builder -> builder.stringValue(id))
				.build()
				._toQuery();
	}

	public static Query getCustomerIdQuery(String customerId) {
		return QueryBuilders.match()
				.field(getKeywordField(CUSTOMER_ID_ATTRIBUTE))
				.query(builder -> builder.stringValue(customerId))
				.build()
				._toQuery();
	}

	public static Query getSiteQuery(String site) {
		return QueryBuilders.match()
				.field(getKeywordField(SITE))
				.query(builder -> builder.stringValue(site))
				.build()
				._toQuery();
	}

	public static Query getSiteIdQuery(String siteId) {
		return QueryBuilders.match()
				.field(getKeywordField(SITE_ID))
				.query(builder -> builder.stringValue(siteId))
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

	public static Query getDateRangeQuery(Date startDate, Date endDate) {
		return QueryBuilders.range()
				.field(TIMESTAMP)
				.gte(JsonData.of(startDate.toInstant().toString()))
				.lte(JsonData.of(endDate.toInstant().toString()))
				.format("strict_date_optional_time")
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

	public static SortOptions sortByTimeStampDesc() {
		return new SortOptions.Builder()
				.field(new FieldSort.Builder()
						.field(TIMESTAMP)
						.order(SortOrder.Desc)
						.build())
				.build();
	}

	private static SearchRequest.Builder getBaseBuilder(int count) {
		return getSearchRequestBuilder()
				.storedFields("*")
				.size(count)
				.docvalueFields(new FieldAndFormat.Builder()
						.field(TIMESTAMP)
						.format("date_time")
						.build())
				.docvalueFields(
						new FieldAndFormat.Builder().field(TOTAL_REAL_POWER).build());
	}

	public static SearchRequest.Builder getWeatherSummaryFacet() {
		return getBaseBuilder(0)
				.aggregations(
						"terms",
						AggregationBuilders.terms()
								.field(getKeywordField(MeterConstants.WEATHER_SUMMARY))
								.size(40)
								.build()
								._toAggregation());
	}

	public static SearchRequest.Builder geDeviceNameFacet() {
		return getBaseBuilder(0)
				.aggregations(
						"terms",
						AggregationBuilders.terms()
								.field(getKeywordField(MeterConstants.DEVICE_NAME))
								.size(1000)
								.build()
								._toAggregation());
	}

	public static SearchRequest.Builder getDataSearch(int offset, int size) {
		SearchRequest.Builder search = getBaseBuilder(size)
				.from(offset)
				.docvalueFields(new FieldAndFormat.Builder()
						.field(getKeywordField(SITE))
						.build())
				.docvalueFields(new FieldAndFormat.Builder()
						.field(getKeywordField(DEVICE_NAME))
						.build())
				.docvalueFields(new FieldAndFormat.Builder().field(ENG_CONS).build())
				.docvalueFields(
						new FieldAndFormat.Builder().field(TOTAL_ENG_CONS).build())
				.sort(builder -> builder.field(new FieldSort.Builder()
						.field(TIMESTAMP)
						.order(SortOrder.Desc)
						.build()));
		// If size == 500, it's for the report.  If it's 10000, it's extracting the report (which
		// does not include weather)
		if (size == 500) {
			search.docvalueFields(new FieldAndFormat.Builder()
							.field(getKeywordField(WEATHER_SUMMARY))
							.build())
					.docvalueFields(
							new FieldAndFormat.Builder().field(TEMPERATURE).build());
		}
		return search;
	}

	public static SearchRequest.Builder getStackedTimeSeriesBuilder(String timezone, String bucketSize) {

		return getBaseBuilder(0)
				.aggregations(
						"2",
						new Aggregation.Builder()
								.dateHistogram(AggregationBuilders.dateHistogram()
										.field(TIMESTAMP)
										.fixedInterval(new Time.Builder()
												.time(bucketSize)
												.build())
										.timeZone(timezone)
										.minDocCount(1)
										.build())
								.aggregations(
										"terms",
										new Aggregation.Builder()
												.terms(new TermsAggregation.Builder()
														.field(getKeywordField(DEVICE_NAME))
														.order(Collections.singletonList(
																Collections.singletonMap("1", SortOrder.Desc)))
														.size(10)
														.build())
												.aggregations(
														"1",
														new Aggregation.Builder()
																.avg(new AverageAggregation.Builder()
																		.field(TOTAL_REAL_POWER)
																		.build())
																.build())
												.build())
								.build());
	}

	public static SearchRequest.Builder getTimeSeriesBuilder(String timezone, String bucketSize) {
		return getBaseBuilder(0)
				.aggregations(
						"2",
						new Aggregation.Builder()
								.dateHistogram(AggregationBuilders.dateHistogram()
										.field(TIMESTAMP)
										.fixedInterval(new Time.Builder()
												.time(bucketSize)
												.build())
										.timeZone(timezone)
										.minDocCount(1)
										.build())
								.aggregations(
										"1",
										new Aggregation.Builder()
												.avg(new AverageAggregation.Builder()
														.field(TOTAL_REAL_POWER)
														.build())
												.build())
								.build());
	}

	public static SearchRequest.Builder getTimeSeriesMaxBuilder(String timezone, String bucketSize) {
		return getBaseBuilder(0)
				.aggregations(
						"2",
						new Aggregation.Builder()
								.dateHistogram(AggregationBuilders.dateHistogram()
										.field(TIMESTAMP)
										.fixedInterval(new Time.Builder()
												.time(bucketSize)
												.build())
										.timeZone(timezone)
										.minDocCount(1)
										.build())
								.aggregations(
										"1",
										new Aggregation.Builder()
												.max(new MaxAggregation.Builder()
														.field(TOTAL_REAL_POWER)
														.build())
												.build())
								.build());
	}

	public static SearchRequest.Builder getMaxCurrentBuilder(String timezone, String bucketSize) {
		return getBaseBuilder(1)
				.aggregations(
						"max",
						new Aggregation.Builder()
								.max(new MaxAggregation.Builder()
										.field(TOTAL_REAL_POWER)
										.build())
								.build())
				.sort(builder -> builder.field(new FieldSort.Builder()
						.field(TIMESTAMP)
						.order(SortOrder.Desc)
						.build()));
	}

	public static SearchRequest.Builder getAverageTotalBuilder(String timezone, String bucketSize) {
		return getBaseBuilder(0)
				.aggregations("avg", getAverageAggregation())
				.aggregations("total", getTotalAggregation());
	}

	public static SearchRequest.Builder getAverageBuilder(String timezone, String bucketSize) {
		return getBaseBuilder(0).aggregations("avg", getAverageAggregation());
	}

	public static SearchRequest.Builder getTotalBuilder(String timezone, String bucketSize) {
		return getBaseBuilder(0).aggregations("total", getTotalAggregation());
	}

	private static Aggregation getAverageAggregation() {
		return new Aggregation.Builder()
				.avg(new AverageAggregation.Builder().field(TOTAL_REAL_POWER).build())
				.build();
	}

	private static Aggregation getTotalAggregation() {
		return new Aggregation.Builder()
				.sum(new SumAggregation.Builder().field(ENG_CONS).build())
				.build();
	}

	public static SearchRequest.Builder getTotalEnergyConsumedBuilder(String timezone, String bucketSize) {
		return getBaseBuilder(0)
				.aggregations(
						"2",
						new Aggregation.Builder()
								.dateHistogram(AggregationBuilders.dateHistogram()
										.field(TIMESTAMP)
										.fixedInterval(new Time.Builder()
												.time(bucketSize)
												.build())
										.timeZone(timezone)
										.minDocCount(1)
										.build())
								.aggregations(
										"1",
										new Aggregation.Builder()
												.sum(new SumAggregation.Builder()
														.field(ENG_CONS)
														.build())
												.build())
								.build());
	}
}
