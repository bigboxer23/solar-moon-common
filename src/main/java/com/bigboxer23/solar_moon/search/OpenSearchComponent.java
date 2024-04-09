package com.bigboxer23.solar_moon.search;

import com.bigboxer23.solar_moon.data.DeviceAttribute;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.data.OpenSearchDTO;
import com.bigboxer23.solar_moon.ingest.MeterConstants;
import com.bigboxer23.solar_moon.util.TimeConstants;
import com.bigboxer23.solar_moon.util.TimeUtils;
import com.bigboxer23.utils.properties.PropertyUtils;
import java.io.IOException;
import java.util.*;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.opensearch.client.ResponseException;
import org.opensearch.client.RestClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.*;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.QueryBuilders;
import org.opensearch.client.opensearch.core.*;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.IndexOperation;
import org.opensearch.client.opensearch.core.search.SourceConfig;
import org.opensearch.client.opensearch.core.search.SourceFilter;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.utils.StringUtils;

/** */
public class OpenSearchComponent implements OpenSearchConstants {

	private static final Logger logger = LoggerFactory.getLogger(OpenSearchComponent.class);

	private OpenSearchClient client;

	private final String openSearchUrl;

	private final String user;

	private final String pass;

	public OpenSearchComponent() {
		openSearchUrl = PropertyUtils.getProperty("opensearch.url");
		user = PropertyUtils.getProperty("opensearch.user");
		pass = PropertyUtils.getProperty("opensearch.pw");
	}

	public void logData(Date fetchDate, List<DeviceData> deviceData) throws ResponseException {
		logger.debug("sending to opensearch component");
		BulkRequest.Builder bulkRequest = new BulkRequest.Builder().index(INDEX_NAME);
		deviceData.forEach(device -> {
			device.addAttribute(
					new DeviceAttribute(TIMESTAMP, null, device.getDate() == null ? fetchDate : device.getDate()));
			bulkRequest.operations(new BulkOperation.Builder()
					.index(new IndexOperation.Builder<OpenSearchDTO>()
							.id(device.getDeviceId()
									+ ":"
									+ (device.getDate() != null
											? device.getDate().getTime()
											: System.currentTimeMillis()))
							.document(new OpenSearchDTO(device))
							.build())
					.build());
		});
		logger.debug("Sending Request to open search");
		try {
			BulkResponse response = getClient().bulk(bulkRequest.build());
			if (response.errors()) {
				response.items().forEach(item -> logger.warn("error:" + item.error()));
			}
		} catch (IOException e) {
			logger.error("logStatusEvent", e);
			if (e instanceof ResponseException) {
				throw (ResponseException) e;
			}
		}
	}

	public Float getTotalEnergyConsumed(String deviceId) {
		try {
			SearchRequest request = OpenSearchQueries.getSearchRequestBuilder()
					.query(OpenSearchQueries.getDeviceIdQuery(deviceId))
					.sort(new SortOptions.Builder()
							.field(new FieldSort.Builder()
									.field(TIMESTAMP)
									.order(SortOrder.Desc)
									.build())
							.build())
					.size(1)
					.source(new SourceConfig.Builder()
							.filter(new SourceFilter.Builder()
									.includes(Collections.singletonList(MeterConstants.TOTAL_ENG_CONS))
									.build())
							.build())
					.build();
			SearchResponse<Map> response = getClient().search(request, Map.class);
			if (response.hits().hits().isEmpty()) {
				logger.warn("couldn't find previous value for " + deviceId);
				return null;
			}
			Map<String, Object> fields = response.hits().hits().get(0).source();
			if (fields == null) {
				logger.warn("No fields associated with result for " + deviceId);
				return null;
			}
			return Optional.ofNullable((Double) fields.get(MeterConstants.TOTAL_ENG_CONS))
					.map(Double::floatValue)
					.orElseGet(() -> {
						logger.debug("null/empty value stored in OS");
						return null;
					});
		} catch (IOException e) {
			logger.error("getTotalEnergyConsumed", e);
			return null;
		}
	}

	public DeviceData getDeviceEntryWithinLast15Min(String customerId, String deviceId) {
		return getLastDeviceEntry(
				deviceId,
				OpenSearchQueries.getDeviceIdQuery(deviceId),
				OpenSearchQueries.getCustomerIdQuery(customerId),
				OpenSearchQueries.getLast15MinQuery());
	}

	public DeviceData getLastDeviceEntry(String deviceId, Query query, Query... queries) {
		try {
			SearchRequest request = OpenSearchQueries.getSearchRequestBuilder()
					.query(QueryBuilders.bool().filter(query, queries).build()._toQuery())
					.sort(OpenSearchQueries.sortByTimeStampDesc())
					.size(1)
					.build();
			SearchResponse<Map> response = getClient().search(request, Map.class);
			if (response.hits().hits().isEmpty()) {
				logger.debug("couldn't find previous value for " + deviceId);
				return null;
			}
			return OpenSearchUtils.getDeviceDataFromFields(
					deviceId, response.hits().hits().get(0).source());
		} catch (IOException e) {
			logger.error("getLastDeviceEntry:", e);
			return null;
		}
	}

	public void deleteByCustomerId(String customerId) {
		try {
			getClient()
					.deleteByQuery(OpenSearchQueries.getDeleteRequestBuilder()
							.query(OpenSearchQueries.getCustomerIdQuery(customerId))
							.build());
		} catch (IOException e) {
			logger.error("deleteByCustomerId: ", e);
		}
	}

	public void deleteBySiteId(String siteId, String customerId) {
		try {
			getClient()
					.deleteByQuery(OpenSearchQueries.getDeleteRequestBuilder()
							.query(QueryBuilders.bool()
									.filter(
											OpenSearchQueries.getCustomerIdQuery(customerId),
											OpenSearchQueries.getSiteIdQuery(siteId))
									.build()
									._toQuery())
							.build());
		} catch (IOException e) {
			logger.error("deleteByCustomerId: ", e);
		}
	}

	public void deleteByDeviceId(String deviceId, String customerId) {
		try {
			getClient()
					.deleteByQuery(OpenSearchQueries.getDeleteRequestBuilder()
							.query(QueryBuilders.bool()
									.filter(
											OpenSearchQueries.getCustomerIdQuery(customerId),
											OpenSearchQueries.getDeviceIdQuery(deviceId))
									.build()
									._toQuery())
							.build());
		} catch (IOException e) {
			logger.error("deleteByCustomerId: ", e);
		}
	}

	public void deleteById(String id) {
		try {
			getClient()
					.delete(new DeleteRequest.Builder().index(INDEX_NAME).id(id).build());
		} catch (IOException e) {
			logger.error("deleteById: " + id, e);
		}
	}

	public DeviceData getDeviceByTimePeriod(String customerId, String deviceId, Date date) {
		try {
			SearchRequest request = OpenSearchQueries.getSearchRequestBuilder()
					.query(QueryBuilders.bool()
							.filter(
									OpenSearchQueries.getCustomerIdQuery(customerId),
									OpenSearchQueries.getDeviceIdQuery(deviceId),
									OpenSearchQueries.getDateRangeQuery(date))
							.build()
							._toQuery())
					.build();
			SearchResponse<Map> response = getClient().search(request, Map.class);
			if (response.hits().hits().isEmpty()) {
				logger.debug("Couldn't find previous value for " + deviceId);
				return null;
			}
			if (response.hits().hits().size() > 1) {
				throw new IOException(
						"too many device results: " + response.hits().hits().size());
			}
			return OpenSearchUtils.getDeviceDataFromFields(
					deviceId, response.hits().hits().get(0).source());
		} catch (IOException e) {
			logger.error("getDeviceByTimePeriod", e);
			return null;
		}
	}

	public List<DeviceData> getDevicesForSiteByTimePeriod(String customerId, String siteId, Date date) {
		try {
			SearchRequest request = OpenSearchQueries.getSearchRequestBuilder()
					.query(QueryBuilders.bool()
							.filter(
									OpenSearchQueries.getCustomerIdQuery(customerId),
									OpenSearchQueries.getSiteIdQuery(siteId),
									OpenSearchQueries.getDateRangeQuery(date))
							.build()
							._toQuery())
					.build();
			return OpenSearchUtils.getDeviceDataFromResults(
					getClient().search(request, Map.class).hits().hits());
		} catch (IOException e) {
			logger.error("getDeviceCountByTimePeriod", e);
			return Collections.emptyList();
		}
	}

	public int getSiteDevicesCountByTimePeriod(String customerId, String siteId, Date date) {
		try {
			SearchRequest request = OpenSearchQueries.getSearchRequestBuilder()
					.query(QueryBuilders.bool()
							.filter(
									OpenSearchQueries.getCustomerIdQuery(customerId),
									OpenSearchQueries.getSiteIdQuery(siteId),
									OpenSearchQueries.getDateRangeQuery(date))
							.build()
							._toQuery())
					.build();
			return getClient().search(request, Map.class).hits().hits().size();
		} catch (IOException e) {
			logger.error("getDeviceCountByTimePeriod " + customerId + ":" + siteId, e);
			return -1;
		}
	}

	public SearchResponse search(SearchJSON searchJSON) {
		try {
			SearchRequest request =
					getSearchRequest(searchJSON).query(getQuery(searchJSON)).build();
			return getClient().search(request, Map.class);
		} catch (IOException e) {
			logger.error("search " + searchJSON.getCustomerId() + ":" + searchJSON.getDeviceName(), e);
		}
		return null;
	}

	public UpdateByQueryResponse updateByQuery(SearchJSON searchJSON, String field, Object value) {
		try {
			UpdateByQueryRequest request = OpenSearchQueries.getUpdateByQueryRequestBuilder()
					.query(getQuery(searchJSON))
					.script(OpenSearchQueries.getUpdateScript(field, value))
					.build();
			return getClient().updateByQuery(request);
		} catch (IOException e) {
			logger.error("search " + searchJSON.getCustomerId() + ":" + searchJSON.getDeviceName(), e);
		}
		return null;
	}

	public double getAverageEnergyConsumedPerDay(SearchJSON searchJSON) {
		searchJSON.setType(TOTAL_ENERGY_CONSUMED_SEARCH_TYPE);
		searchJSON.setIsSite(true);
		searchJSON.setBucketSize("1d");
		Date end = TimeUtils.getStartOfDay(searchJSON.getTimeZone());
		searchJSON.setEndDate(end.getTime() - TimeConstants.SECOND);
		searchJSON.setStartDate(end.getTime() - TimeConstants.NINETY_DAYS);
		return ((Aggregate) search(searchJSON).aggregations().get("2"))
				.dateHistogram().buckets().array().stream()
						.map(bucket -> bucket.aggregations().get("1").sum().value())
						.mapToDouble(a -> a)
						.average()
						.orElse(-1);
	}

	private Query getQuery(SearchJSON searchJSON) {
		return QueryBuilders.bool()
				.filter(getFiltersByType(searchJSON))
				.mustNot(getMustNotByType(searchJSON))
				.build()
				._toQuery();
	}

	private List<Query> getMustNotByType(SearchJSON searchJSON) {
		List<Query> filters = new ArrayList<>();
		if (searchJSON.isNoVirtual()) {
			filters.add(OpenSearchQueries.getNotVirtual());
		}
		if (searchJSON.isNoIsSite()) {
			filters.add(OpenSearchQueries.getNotIsSite());
		}
		return filters;
	}

	private List<Query> getFiltersByType(SearchJSON searchJSON) {
		List<Query> filters = new ArrayList<>();
		if (!StringUtils.isBlank(searchJSON.getDeviceName())) {
			filters.add(OpenSearchQueries.getDeviceNameQuery(searchJSON.getDeviceName()));
		}
		if (!StringUtils.isBlank(searchJSON.getSite())) {
			filters.add(OpenSearchQueries.getSiteQuery(searchJSON.getSite()));
		}
		if (!StringUtils.isBlank(searchJSON.getSiteId())) {
			filters.add(OpenSearchQueries.getSiteIdQuery(searchJSON.getSiteId()));
		}
		if (!StringUtils.isBlank(searchJSON.getDeviceId())) {
			filters.add(OpenSearchQueries.getDeviceIdQuery(searchJSON.getDeviceId()));
		}
		if (searchJSON.isVirtual()) {
			filters.add(OpenSearchQueries.getIsVirtual());
		}
		if (searchJSON.getIsSite()) {
			filters.add(OpenSearchQueries.getIsSite());
		}
		if (searchJSON.isDaylight()) {
			filters.add(OpenSearchQueries.getIsDaylight());
		}
		filters.addAll(Arrays.asList(
				OpenSearchQueries.getDateRangeQuery(searchJSON.getJavaStartDate(), searchJSON.getJavaEndDate()),
				OpenSearchQueries.getCustomerIdQuery(searchJSON.getCustomerId())));
		return filters;
	}

	private SearchRequest.Builder getSearchRequest(SearchJSON searchJSON) {
		return switch (searchJSON.getType()) {
			case TIME_SERIES_SEARCH_TYPE -> OpenSearchQueries.getTimeSeriesBuilder(
					searchJSON.getTimeZone(), searchJSON.getBucketSize());
			case TIME_SERIES_MAX_SEARCH_TYPE -> OpenSearchQueries.getTimeSeriesMaxBuilder(
					searchJSON.getTimeZone(), searchJSON.getBucketSize());
			case AVG_TOTAL_SEARCH_TYPE -> OpenSearchQueries.getAverageTotalBuilder(
					searchJSON.getTimeZone(), searchJSON.getBucketSize());
			case AVG_SEARCH_TYPE -> OpenSearchQueries.getAverageBuilder(
					searchJSON.getTimeZone(), searchJSON.getBucketSize());
			case TOTAL_SEARCH_TYPE -> OpenSearchQueries.getTotalBuilder(
					searchJSON.getTimeZone(), searchJSON.getBucketSize());
			case MAX_CURRENT_SEARCH_TYPE -> OpenSearchQueries.getMaxCurrentBuilder();
			case MAX_ENERGY_CONSUMED_SEARCH_TYPE -> OpenSearchQueries.getMaxEnergyConsumed();
			case STACKED_TIME_SERIES_SEARCH_TYPE, GROUPED_BAR_SEARCH_TYPE -> OpenSearchQueries
					.getStackedTimeSeriesBuilder(searchJSON.getTimeZone(), searchJSON.getBucketSize());
			case DATA_SEARCH_TYPE -> OpenSearchQueries.getDataSearch(
					searchJSON.getOffset(), searchJSON.getSize(), searchJSON.isIncludeSource());
			case TOTAL_ENERGY_CONSUMED_SEARCH_TYPE -> OpenSearchQueries.getTotalEnergyConsumedBuilder(
					searchJSON.getTimeZone(), searchJSON.getBucketSize());
			default -> null;
		};
	}

	public List<StringTermsBucket> getWeatherFacets() throws IOException {
		return getClient()
				.search(OpenSearchQueries.getWeatherSummaryFacet().build(), Map.class)
				.aggregations()
				.get("terms")
				.sterms()
				.buckets()
				.array();
	}

	public List<StringTermsBucket> getDevicesFacet(SearchJSON searchJSON) throws IOException {
		return getClient()
				.search(
						OpenSearchQueries.geDeviceIdFacet()
								.query(getQuery(searchJSON))
								.build(),
						Map.class)
				.aggregations()
				.get("terms")
				.sterms()
				.buckets()
				.array();
	}

	public List<DeviceData> getRecentDeviceData(String customerId, String deviceId, long offset) throws IOException {
		double maxRecordCount = Math.round((float) offset / TimeConstants.FIFTEEN_MINUTES);
		SearchJSON searchJSON =
				new SearchJSON(customerId, deviceId, System.currentTimeMillis(), System.currentTimeMillis() - offset);
		searchJSON.setSize(Double.valueOf(maxRecordCount).intValue());

		return OpenSearchUtils.getDeviceDataFromResults(getClient()
				.search(
						OpenSearchQueries.getSearchRequestBuilder()
								.query(getQuery(searchJSON))
								.build(),
						Map.class)
				.hits()
				.hits());
	}

	public float getMaxTotalEnergyConsumed(String customerId, String deviceId, long offset) {
		SearchJSON search = new SearchJSON();
		search.setCustomerId(customerId);
		search.setType(OpenSearchConstants.MAX_ENERGY_CONSUMED_SEARCH_TYPE);
		search.setBucketSize("3h");
		Date end = TimeUtils.get15mRoundedDate();
		search.setDeviceId(deviceId);
		search.setEndDate(end.getTime());
		search.setStartDate(end.getTime() - offset);
		SearchResponse response = search(search);
		return Double.valueOf(
						((Aggregate) response.aggregations().get("max")).max().value())
				.floatValue();
	}

	public boolean isOpenSearchAvailable() {
		try {
			if (!getClient().ping().value()) {
				logger.warn("isOpenSearchAvailable false ping");
				return false;
			}
			getClient().indices().stats();
		} catch (IOException e) {
			logger.error("isOpenSearchAvailable", e);
			return false;
		}
		return true;
	}

	private OpenSearchClient getClient() {
		if (client == null) {
			final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
			credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, pass));
			final RestClient restClient = RestClient.builder(HttpHost.create(openSearchUrl))
					.setHttpClientConfigCallback(
							httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
					.build();
			client = new OpenSearchClient(new RestClientTransport(restClient, new JacksonJsonpMapper()));
		}
		return client;
	}
}
