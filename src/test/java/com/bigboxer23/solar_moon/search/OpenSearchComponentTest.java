package com.bigboxer23.solar_moon.search;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.ops.LogEntry;
import com.bigboxer23.solar_moon.util.TimeConstants;
import java.io.IOException;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.ResponseException;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.*;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.DateHistogramAggregate;
import org.opensearch.client.opensearch._types.aggregations.DateHistogramBucket;
import org.opensearch.client.opensearch._types.aggregations.MaxAggregate;
import org.opensearch.client.opensearch._types.aggregations.StringTermsAggregate;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch._types.aggregations.SumAggregate;
import org.opensearch.client.opensearch.core.*;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.HitsMetadata;
import org.opensearch.client.opensearch.indices.DeleteIndexRequest;
import org.opensearch.client.opensearch.indices.DeleteIndexResponse;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;

@ExtendWith(MockitoExtension.class)
class OpenSearchComponentTest {
	@Mock
	private OpenSearchClient mockClient;

	@Mock
	private OpenSearchIndicesClient mockIndicesClient;

	private OpenSearchComponent component;

	@BeforeEach
	void setUp() {
		component = new OpenSearchComponent() {
			@Override
			protected OpenSearchClient getClient() {
				return mockClient;
			}
		};
	}

	@Test
	void testLogData_success() throws IOException {
		BulkResponse bulkResponse = mock(BulkResponse.class);
		when(bulkResponse.errors()).thenReturn(false);
		when(mockClient.bulk(any(BulkRequest.class))).thenReturn(bulkResponse);

		Date fetchDate = new Date();
		DeviceData data1 = new DeviceData();
		data1.setDeviceId("device1");
		data1.setDate(fetchDate);

		DeviceData data2 = new DeviceData();
		data2.setDeviceId("device2");

		List<DeviceData> deviceDatas = Arrays.asList(data1, data2);

		assertDoesNotThrow(() -> component.logData(fetchDate, deviceDatas));

		verify(mockClient).bulk(any(BulkRequest.class));
		assertEquals(fetchDate, data2.getDate());
	}

	@Test
	void testLogData_withErrors() throws IOException {
		BulkResponse bulkResponse = mock(BulkResponse.class);
		BulkResponseItem errorItem = mock(BulkResponseItem.class);
		ErrorCause errorCause = mock(ErrorCause.class);

		when(bulkResponse.errors()).thenReturn(true);
		when(bulkResponse.items()).thenReturn(Collections.singletonList(errorItem));
		when(errorItem.error()).thenReturn(errorCause);
		when(mockClient.bulk(any(BulkRequest.class))).thenReturn(bulkResponse);

		Date fetchDate = new Date();
		DeviceData data = new DeviceData();
		data.setDeviceId("device1");

		assertDoesNotThrow(() -> component.logData(fetchDate, Collections.singletonList(data)));

		verify(mockClient).bulk(any(BulkRequest.class));
	}

	@Test
	void testLogData_withIOException() throws IOException {
		when(mockClient.bulk(any(BulkRequest.class))).thenThrow(new IOException("Test error"));

		Date fetchDate = new Date();
		DeviceData data = new DeviceData();
		data.setDeviceId("device1");

		assertDoesNotThrow(() -> component.logData(fetchDate, Collections.singletonList(data)));
	}

	@Test
	void testLogData_withResponseException() throws IOException {
		ResponseException responseException = mock(ResponseException.class);
		when(mockClient.bulk(any(BulkRequest.class))).thenThrow(responseException);

		Date fetchDate = new Date();
		DeviceData data = new DeviceData();
		data.setDeviceId("device1");

		assertThrows(ResponseException.class, () -> component.logData(fetchDate, Collections.singletonList(data)));
	}

	@Test
	void testGetTotalEnergyConsumed_success() throws IOException {
		SearchResponse<DeviceData> searchResponse = mock(SearchResponse.class);
		HitsMetadata<DeviceData> hitsMetadata = mock(HitsMetadata.class);
		Hit<DeviceData> hit = mock(Hit.class);
		DeviceData deviceData = new DeviceData();
		deviceData.setTotalEnergyConsumed(100.5f);

		when(mockClient.search(any(SearchRequest.class), eq(DeviceData.class))).thenReturn(searchResponse);
		when(searchResponse.hits()).thenReturn(hitsMetadata);
		when(hitsMetadata.hits()).thenReturn(Collections.singletonList(hit));
		when(hit.source()).thenReturn(deviceData);

		Float result = component.getTotalEnergyConsumed("device1");

		assertEquals(100.5f, result);
		verify(mockClient).search(any(SearchRequest.class), eq(DeviceData.class));
	}

	@Test
	void testGetTotalEnergyConsumed_noHits() throws IOException {
		SearchResponse<DeviceData> searchResponse = mock(SearchResponse.class);
		HitsMetadata<DeviceData> hitsMetadata = mock(HitsMetadata.class);

		when(mockClient.search(any(SearchRequest.class), eq(DeviceData.class))).thenReturn(searchResponse);
		when(searchResponse.hits()).thenReturn(hitsMetadata);
		when(hitsMetadata.hits()).thenReturn(Collections.emptyList());

		Float result = component.getTotalEnergyConsumed("device1");

		assertNull(result);
	}

	@Test
	void testGetTotalEnergyConsumed_nullSource() throws IOException {
		SearchResponse<DeviceData> searchResponse = mock(SearchResponse.class);
		HitsMetadata<DeviceData> hitsMetadata = mock(HitsMetadata.class);
		Hit<DeviceData> hit = mock(Hit.class);

		when(mockClient.search(any(SearchRequest.class), eq(DeviceData.class))).thenReturn(searchResponse);
		when(searchResponse.hits()).thenReturn(hitsMetadata);
		when(hitsMetadata.hits()).thenReturn(Collections.singletonList(hit));
		when(hit.source()).thenReturn(null);

		Float result = component.getTotalEnergyConsumed("device1");

		assertNull(result);
	}

	@Test
	void testGetTotalEnergyConsumed_ioException() throws IOException {
		when(mockClient.search(any(SearchRequest.class), eq(DeviceData.class)))
				.thenThrow(new IOException("Test error"));

		Float result = component.getTotalEnergyConsumed("device1");

		assertNull(result);
	}

	@Test
	void testGetDeviceEntryWithinLast15Min() throws IOException {
		SearchResponse<DeviceData> searchResponse = mock(SearchResponse.class);
		HitsMetadata<DeviceData> hitsMetadata = mock(HitsMetadata.class);
		Hit<DeviceData> hit = mock(Hit.class);
		DeviceData deviceData = new DeviceData();

		when(mockClient.search(any(SearchRequest.class), eq(DeviceData.class))).thenReturn(searchResponse);
		when(searchResponse.hits()).thenReturn(hitsMetadata);
		when(hitsMetadata.hits()).thenReturn(Collections.singletonList(hit));
		when(hit.source()).thenReturn(deviceData);

		DeviceData result = component.getDeviceEntryWithinLast15Min("customer1", "device1");

		assertNotNull(result);
		assertEquals(deviceData, result);
	}

	@Test
	void testGetLastDeviceEntry_success() throws IOException {
		SearchResponse<DeviceData> searchResponse = mock(SearchResponse.class);
		HitsMetadata<DeviceData> hitsMetadata = mock(HitsMetadata.class);
		Hit<DeviceData> hit = mock(Hit.class);
		DeviceData deviceData = new DeviceData();

		when(mockClient.search(any(SearchRequest.class), eq(DeviceData.class))).thenReturn(searchResponse);
		when(searchResponse.hits()).thenReturn(hitsMetadata);
		when(hitsMetadata.hits()).thenReturn(Collections.singletonList(hit));
		when(hit.source()).thenReturn(deviceData);

		DeviceData result = component.getLastDeviceEntry("device1", OpenSearchQueries.getDeviceIdQuery("device1"));

		assertNotNull(result);
		assertEquals(deviceData, result);
	}

	@Test
	void testGetLastDeviceEntry_noHits() throws IOException {
		SearchResponse<DeviceData> searchResponse = mock(SearchResponse.class);
		HitsMetadata<DeviceData> hitsMetadata = mock(HitsMetadata.class);

		when(mockClient.search(any(SearchRequest.class), eq(DeviceData.class))).thenReturn(searchResponse);
		when(searchResponse.hits()).thenReturn(hitsMetadata);
		when(hitsMetadata.hits()).thenReturn(Collections.emptyList());

		DeviceData result = component.getLastDeviceEntry("device1", OpenSearchQueries.getDeviceIdQuery("device1"));

		assertNull(result);
	}

	@Test
	void testGetLastDeviceEntry_ioException() throws IOException {
		when(mockClient.search(any(SearchRequest.class), eq(DeviceData.class)))
				.thenThrow(new IOException("Test error"));

		DeviceData result = component.getLastDeviceEntry("device1", OpenSearchQueries.getDeviceIdQuery("device1"));

		assertNull(result);
	}

	@Test
	void testDeleteByCustomerId() throws IOException {
		DeleteByQueryResponse response = mock(DeleteByQueryResponse.class);
		when(mockClient.deleteByQuery(any(DeleteByQueryRequest.class))).thenReturn(response);

		assertDoesNotThrow(() -> component.deleteByCustomerId("customer1"));

		verify(mockClient).deleteByQuery(any(DeleteByQueryRequest.class));
	}

	@Test
	void testDeleteByCustomerId_ioException() throws IOException {
		when(mockClient.deleteByQuery(any(DeleteByQueryRequest.class))).thenThrow(new IOException("Test error"));

		assertDoesNotThrow(() -> component.deleteByCustomerId("customer1"));
	}

	@Test
	void testDeleteOldLogs() throws IOException {
		when(mockClient.indices()).thenReturn(mockIndicesClient);
		DeleteIndexResponse response = mock(DeleteIndexResponse.class);
		when(mockIndicesClient.delete(any(DeleteIndexRequest.class))).thenReturn(response);

		assertDoesNotThrow(() -> component.deleteOldLogs());

		verify(mockIndicesClient).delete(any(DeleteIndexRequest.class));
	}

	@Test
	void testDeleteOldLogs_ioException() throws IOException {
		when(mockClient.indices()).thenReturn(mockIndicesClient);
		when(mockIndicesClient.delete(any(DeleteIndexRequest.class))).thenThrow(new IOException("Test error"));

		assertDoesNotThrow(() -> component.deleteOldLogs());
	}

	@Test
	void testDeleteOldLogs_openSearchException() throws IOException {
		when(mockClient.indices()).thenReturn(mockIndicesClient);
		ErrorResponse errorResponse = mock(ErrorResponse.class);
		ErrorCause errorCause = mock(ErrorCause.class);
		when(errorResponse.error()).thenReturn(errorCause);
		when(errorCause.type()).thenReturn("test_exception");
		OpenSearchException exception = new OpenSearchException(errorResponse);
		when(mockIndicesClient.delete(any(DeleteIndexRequest.class))).thenThrow(exception);

		assertDoesNotThrow(() -> component.deleteOldLogs());
	}

	@Test
	void testDeleteBySiteId() throws IOException {
		DeleteByQueryResponse response = mock(DeleteByQueryResponse.class);
		when(mockClient.deleteByQuery(any(DeleteByQueryRequest.class))).thenReturn(response);

		assertDoesNotThrow(() -> component.deleteBySiteId("site1", "customer1"));

		verify(mockClient).deleteByQuery(any(DeleteByQueryRequest.class));
	}

	@Test
	void testDeleteBySiteId_ioException() throws IOException {
		when(mockClient.deleteByQuery(any(DeleteByQueryRequest.class))).thenThrow(new IOException("Test error"));

		assertDoesNotThrow(() -> component.deleteBySiteId("site1", "customer1"));
	}

	@Test
	void testDeleteByDeviceId() throws IOException {
		DeleteByQueryResponse response = mock(DeleteByQueryResponse.class);
		when(mockClient.deleteByQuery(any(DeleteByQueryRequest.class))).thenReturn(response);

		assertDoesNotThrow(() -> component.deleteByDeviceId("device1", "customer1"));

		verify(mockClient).deleteByQuery(any(DeleteByQueryRequest.class));
	}

	@Test
	void testDeleteByDeviceId_ioException() throws IOException {
		when(mockClient.deleteByQuery(any(DeleteByQueryRequest.class))).thenThrow(new IOException("Test error"));

		assertDoesNotThrow(() -> component.deleteByDeviceId("device1", "customer1"));
	}

	@Test
	void testDeleteById() throws IOException {
		DeleteResponse response = mock(DeleteResponse.class);
		when(mockClient.delete(any(DeleteRequest.class))).thenReturn(response);

		assertDoesNotThrow(() -> component.deleteById("id1"));

		verify(mockClient).delete(any(DeleteRequest.class));
	}

	@Test
	void testDeleteById_ioException() throws IOException {
		when(mockClient.delete(any(DeleteRequest.class))).thenThrow(new IOException("Test error"));

		assertDoesNotThrow(() -> component.deleteById("id1"));
	}

	@Test
	void testGetDeviceByTimePeriod_success() throws IOException {
		SearchResponse<DeviceData> searchResponse = mock(SearchResponse.class);
		HitsMetadata<DeviceData> hitsMetadata = mock(HitsMetadata.class);
		Hit<DeviceData> hit = mock(Hit.class);
		DeviceData deviceData = new DeviceData();

		when(mockClient.search(any(SearchRequest.class), eq(DeviceData.class))).thenReturn(searchResponse);
		when(searchResponse.hits()).thenReturn(hitsMetadata);
		when(hitsMetadata.hits()).thenReturn(Collections.singletonList(hit));
		when(hit.source()).thenReturn(deviceData);

		DeviceData result = component.getDeviceByTimePeriod("customer1", "device1", new Date());

		assertNotNull(result);
		assertEquals(deviceData, result);
	}

	@Test
	void testGetDeviceByTimePeriod_noHits() throws IOException {
		SearchResponse<DeviceData> searchResponse = mock(SearchResponse.class);
		HitsMetadata<DeviceData> hitsMetadata = mock(HitsMetadata.class);

		when(mockClient.search(any(SearchRequest.class), eq(DeviceData.class))).thenReturn(searchResponse);
		when(searchResponse.hits()).thenReturn(hitsMetadata);
		when(hitsMetadata.hits()).thenReturn(Collections.emptyList());

		DeviceData result = component.getDeviceByTimePeriod("customer1", "device1", new Date());

		assertNull(result);
	}

	@Test
	void testGetDeviceByTimePeriod_tooManyResults() throws IOException {
		SearchResponse<DeviceData> searchResponse = mock(SearchResponse.class);
		HitsMetadata<DeviceData> hitsMetadata = mock(HitsMetadata.class);
		Hit<DeviceData> hit1 = mock(Hit.class);
		Hit<DeviceData> hit2 = mock(Hit.class);

		when(mockClient.search(any(SearchRequest.class), eq(DeviceData.class))).thenReturn(searchResponse);
		when(searchResponse.hits()).thenReturn(hitsMetadata);
		when(hitsMetadata.hits()).thenReturn(Arrays.asList(hit1, hit2));

		DeviceData result = component.getDeviceByTimePeriod("customer1", "device1", new Date());

		assertNull(result);
	}

	@Test
	void testGetDeviceByTimePeriod_ioException() throws IOException {
		when(mockClient.search(any(SearchRequest.class), eq(DeviceData.class)))
				.thenThrow(new IOException("Test error"));

		DeviceData result = component.getDeviceByTimePeriod("customer1", "device1", new Date());

		assertNull(result);
	}

	@Test
	void testGetDevicesForSiteByTimePeriod_success() throws IOException {
		SearchResponse<DeviceData> searchResponse = mock(SearchResponse.class);
		HitsMetadata<DeviceData> hitsMetadata = mock(HitsMetadata.class);
		Hit<DeviceData> hit1 = mock(Hit.class);
		Hit<DeviceData> hit2 = mock(Hit.class);
		DeviceData data1 = new DeviceData();
		DeviceData data2 = new DeviceData();

		when(mockClient.search(any(SearchRequest.class), eq(DeviceData.class))).thenReturn(searchResponse);
		when(searchResponse.hits()).thenReturn(hitsMetadata);
		when(hitsMetadata.hits()).thenReturn(Arrays.asList(hit1, hit2));
		when(hit1.source()).thenReturn(data1);
		when(hit2.source()).thenReturn(data2);

		List<DeviceData> result = component.getDevicesForSiteByTimePeriod("customer1", "site1", new Date());

		assertNotNull(result);
		assertEquals(2, result.size());
		assertEquals(data1, result.get(0));
		assertEquals(data2, result.get(1));
	}

	@Test
	void testGetDevicesForSiteByTimePeriod_ioException() throws IOException {
		when(mockClient.search(any(SearchRequest.class), eq(DeviceData.class)))
				.thenThrow(new IOException("Test error"));

		List<DeviceData> result = component.getDevicesForSiteByTimePeriod("customer1", "site1", new Date());

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void testGetSiteDevicesCountByTimePeriod_success() throws IOException {
		SearchResponse<Map> searchResponse = mock(SearchResponse.class);
		HitsMetadata<Map> hitsMetadata = mock(HitsMetadata.class);
		Hit<Map> hit1 = mock(Hit.class);
		Hit<Map> hit2 = mock(Hit.class);

		when(mockClient.search(any(SearchRequest.class), eq(Map.class))).thenReturn(searchResponse);
		when(searchResponse.hits()).thenReturn(hitsMetadata);
		when(hitsMetadata.hits()).thenReturn(Arrays.asList(hit1, hit2));

		int result = component.getSiteDevicesCountByTimePeriod("customer1", "site1", new Date());

		assertEquals(2, result);
	}

	@Test
	void testGetSiteDevicesCountByTimePeriod_ioException() throws IOException {
		when(mockClient.search(any(SearchRequest.class), eq(Map.class))).thenThrow(new IOException("Test error"));

		int result = component.getSiteDevicesCountByTimePeriod("customer1", "site1", new Date());

		assertEquals(-1, result);
	}

	@Test
	void testSearchLogs_success() throws IOException {
		SearchResponse<LogEntry> searchResponse = mock(SearchResponse.class);
		when(mockClient.search(any(SearchRequest.class), eq(LogEntry.class))).thenReturn(searchResponse);

		SearchJSON searchJSON = new SearchJSON();
		searchJSON.setStartDate(System.currentTimeMillis());
		searchJSON.setEndDate(System.currentTimeMillis());

		SearchResponse<LogEntry> result = component.searchLogs(searchJSON);

		assertNotNull(result);
		assertEquals(searchResponse, result);
		verify(mockClient).search(any(SearchRequest.class), eq(LogEntry.class));
	}

	@Test
	void testSearchLogs_ioException() throws IOException {
		when(mockClient.search(any(SearchRequest.class), eq(LogEntry.class))).thenThrow(new IOException("Test error"));

		SearchJSON searchJSON = new SearchJSON();
		searchJSON.setStartDate(System.currentTimeMillis());
		searchJSON.setEndDate(System.currentTimeMillis());

		SearchResponse<LogEntry> result = component.searchLogs(searchJSON);

		assertNull(result);
	}

	@Test
	void testSearch_success() throws IOException {
		SearchResponse<DeviceData> searchResponse = mock(SearchResponse.class);
		when(mockClient.search(any(SearchRequest.class), eq(DeviceData.class))).thenReturn(searchResponse);

		SearchJSON searchJSON = new SearchJSON();
		searchJSON.setCustomerId("customer1");
		searchJSON.setDeviceId("device1");
		searchJSON.setStartDate(System.currentTimeMillis());
		searchJSON.setEndDate(System.currentTimeMillis());
		searchJSON.setType(OpenSearchConstants.TIME_SERIES_SEARCH_TYPE);
		searchJSON.setBucketSize("1h");
		searchJSON.setTimeZone("UTC");

		SearchResponse<DeviceData> result = component.search(searchJSON);

		assertNotNull(result);
		assertEquals(searchResponse, result);
		verify(mockClient).search(any(SearchRequest.class), eq(DeviceData.class));
	}

	@Test
	void testSearch_ioException() throws IOException {
		when(mockClient.search(any(SearchRequest.class), eq(DeviceData.class)))
				.thenThrow(new IOException("Test error"));

		SearchJSON searchJSON = new SearchJSON();
		searchJSON.setCustomerId("customer1");
		searchJSON.setType(OpenSearchConstants.TIME_SERIES_SEARCH_TYPE);
		searchJSON.setStartDate(System.currentTimeMillis());
		searchJSON.setEndDate(System.currentTimeMillis());

		SearchResponse<DeviceData> result = component.search(searchJSON);

		assertNull(result);
	}

	@Test
	void testUpdateByQuery_success() throws IOException {
		UpdateByQueryResponse updateResponse = mock(UpdateByQueryResponse.class);
		when(mockClient.updateByQuery(any(UpdateByQueryRequest.class))).thenReturn(updateResponse);

		SearchJSON searchJSON = new SearchJSON();
		searchJSON.setCustomerId("customer1");
		searchJSON.setDeviceId("device1");
		searchJSON.setStartDate(System.currentTimeMillis());
		searchJSON.setEndDate(System.currentTimeMillis());

		UpdateByQueryResponse result = component.updateByQuery(searchJSON, "fieldName", "newValue");

		assertNotNull(result);
		assertEquals(updateResponse, result);
		verify(mockClient).updateByQuery(any(UpdateByQueryRequest.class));
	}

	@Test
	void testUpdateByQuery_ioException() throws IOException {
		when(mockClient.updateByQuery(any(UpdateByQueryRequest.class))).thenThrow(new IOException("Test error"));

		SearchJSON searchJSON = new SearchJSON();
		searchJSON.setCustomerId("customer1");
		searchJSON.setStartDate(System.currentTimeMillis());
		searchJSON.setEndDate(System.currentTimeMillis());

		UpdateByQueryResponse result = component.updateByQuery(searchJSON, "fieldName", "newValue");

		assertNull(result);
	}

	@Test
	void testDeleteByQuery_success() throws IOException {
		DeleteByQueryResponse deleteResponse = mock(DeleteByQueryResponse.class);
		when(mockClient.deleteByQuery(any(DeleteByQueryRequest.class))).thenReturn(deleteResponse);

		SearchJSON searchJSON = new SearchJSON();
		searchJSON.setCustomerId("customer1");
		searchJSON.setDeviceId("device1");
		searchJSON.setStartDate(System.currentTimeMillis());
		searchJSON.setEndDate(System.currentTimeMillis());

		DeleteByQueryResponse result = component.deleteByQuery(searchJSON);

		assertNotNull(result);
		assertEquals(deleteResponse, result);
		verify(mockClient).deleteByQuery(any(DeleteByQueryRequest.class));
	}

	@Test
	void testDeleteByQuery_ioException() throws IOException {
		when(mockClient.deleteByQuery(any(DeleteByQueryRequest.class))).thenThrow(new IOException("Test error"));

		SearchJSON searchJSON = new SearchJSON();
		searchJSON.setCustomerId("customer1");
		searchJSON.setStartDate(System.currentTimeMillis());
		searchJSON.setEndDate(System.currentTimeMillis());

		DeleteByQueryResponse result = component.deleteByQuery(searchJSON);

		assertNull(result);
	}

	@Test
	void testGetAverageEnergyConsumedPerDay() throws IOException {
		SearchResponse<DeviceData> searchResponse = mock(SearchResponse.class);
		Map<String, Aggregate> aggregations = new HashMap<>();
		Aggregate aggregate2 = mock(Aggregate.class);
		DateHistogramAggregate dateHistogram = mock(DateHistogramAggregate.class);
		DateHistogramBucket bucket1 = mock(DateHistogramBucket.class);
		DateHistogramBucket bucket2 = mock(DateHistogramBucket.class);

		Map<String, Aggregate> bucket1Aggs = new HashMap<>();
		Aggregate sumAggregate1 = mock(Aggregate.class);
		SumAggregate sum1 = mock(SumAggregate.class);
		when(sum1.value()).thenReturn(100.0);
		when(sumAggregate1.sum()).thenReturn(sum1);
		bucket1Aggs.put("1", sumAggregate1);

		Map<String, Aggregate> bucket2Aggs = new HashMap<>();
		Aggregate sumAggregate2 = mock(Aggregate.class);
		SumAggregate sum2 = mock(SumAggregate.class);
		when(sum2.value()).thenReturn(200.0);
		when(sumAggregate2.sum()).thenReturn(sum2);
		bucket2Aggs.put("1", sumAggregate2);

		when(bucket1.aggregations()).thenReturn(bucket1Aggs);
		when(bucket2.aggregations()).thenReturn(bucket2Aggs);

		when(dateHistogram.buckets())
				.thenReturn(org.opensearch.client.opensearch._types.aggregations.Buckets.of(
						b -> b.array(Arrays.asList(bucket1, bucket2))));
		when(aggregate2.dateHistogram()).thenReturn(dateHistogram);
		aggregations.put("2", aggregate2);

		when(searchResponse.aggregations()).thenReturn(aggregations);
		when(mockClient.search(any(SearchRequest.class), eq(DeviceData.class))).thenReturn(searchResponse);

		SearchJSON searchJSON = new SearchJSON();
		searchJSON.setCustomerId("customer1");
		searchJSON.setSiteId("site1");
		searchJSON.setTimeZone("UTC");

		double result = component.getAverageEnergyConsumedPerDay(searchJSON);

		assertEquals(150.0, result, 0.01);
	}

	@Test
	void testGetWeatherFacets_success() throws IOException {
		SearchResponse<Map> searchResponse = mock(SearchResponse.class);
		Map<String, Aggregate> aggregations = new HashMap<>();
		Aggregate aggregate = mock(Aggregate.class);
		StringTermsAggregate termsAggregate = mock(StringTermsAggregate.class);
		StringTermsBucket bucket1 = mock(StringTermsBucket.class);
		StringTermsBucket bucket2 = mock(StringTermsBucket.class);

		when(termsAggregate.buckets())
				.thenReturn(org.opensearch.client.opensearch._types.aggregations.Buckets.of(
						b -> b.array(Arrays.asList(bucket1, bucket2))));
		when(aggregate.sterms()).thenReturn(termsAggregate);
		aggregations.put("terms", aggregate);
		when(searchResponse.aggregations()).thenReturn(aggregations);
		when(mockClient.search(any(SearchRequest.class), eq(Map.class))).thenReturn(searchResponse);

		List<StringTermsBucket> result = component.getWeatherFacets();

		assertNotNull(result);
		assertEquals(2, result.size());
	}

	@Test
	void testGetDevicesFacet_success() throws IOException {
		SearchResponse<Map> searchResponse = mock(SearchResponse.class);
		Map<String, Aggregate> aggregations = new HashMap<>();
		Aggregate aggregate = mock(Aggregate.class);
		StringTermsAggregate termsAggregate = mock(StringTermsAggregate.class);
		StringTermsBucket bucket1 = mock(StringTermsBucket.class);

		when(termsAggregate.buckets())
				.thenReturn(org.opensearch.client.opensearch._types.aggregations.Buckets.of(
						b -> b.array(Collections.singletonList(bucket1))));
		when(aggregate.sterms()).thenReturn(termsAggregate);
		aggregations.put("terms", aggregate);
		when(searchResponse.aggregations()).thenReturn(aggregations);
		when(mockClient.search(any(SearchRequest.class), eq(Map.class))).thenReturn(searchResponse);

		SearchJSON searchJSON = new SearchJSON();
		searchJSON.setCustomerId("customer1");
		searchJSON.setStartDate(System.currentTimeMillis());
		searchJSON.setEndDate(System.currentTimeMillis());

		List<StringTermsBucket> result = component.getDevicesFacet(searchJSON);

		assertNotNull(result);
		assertEquals(1, result.size());
	}

	@Test
	void testGetRecentDeviceData_success() throws IOException {
		SearchResponse<DeviceData> searchResponse = mock(SearchResponse.class);
		HitsMetadata<DeviceData> hitsMetadata = mock(HitsMetadata.class);
		Hit<DeviceData> hit1 = mock(Hit.class);
		Hit<DeviceData> hit2 = mock(Hit.class);
		DeviceData data1 = new DeviceData();
		DeviceData data2 = new DeviceData();

		when(mockClient.search(any(SearchRequest.class), eq(DeviceData.class))).thenReturn(searchResponse);
		when(searchResponse.hits()).thenReturn(hitsMetadata);
		when(hitsMetadata.hits()).thenReturn(Arrays.asList(hit1, hit2));
		when(hit1.source()).thenReturn(data1);
		when(hit2.source()).thenReturn(data2);

		List<DeviceData> result = component.getRecentDeviceData("customer1", "device1", TimeConstants.HOUR);

		assertNotNull(result);
		assertEquals(2, result.size());
		assertEquals(data1, result.get(0));
		assertEquals(data2, result.get(1));
	}

	@Test
	void testGetMaxTotalEnergyConsumed_success() throws IOException {
		SearchResponse<DeviceData> searchResponse = mock(SearchResponse.class);
		Map<String, Aggregate> aggregations = new HashMap<>();
		Aggregate maxAggregate = mock(Aggregate.class);
		MaxAggregate max = mock(MaxAggregate.class);

		when(max.value()).thenReturn(500.0);
		when(maxAggregate._kind()).thenReturn(Aggregate.Kind.Max);
		when(maxAggregate.max()).thenReturn(max);
		aggregations.put("max", maxAggregate);

		when(searchResponse.aggregations()).thenReturn(aggregations);
		when(mockClient.search(any(SearchRequest.class), eq(DeviceData.class))).thenReturn(searchResponse);

		float result = component.getMaxTotalEnergyConsumed("customer1", "device1", TimeConstants.HOUR);

		assertEquals(500.0f, result, 0.01);
	}

	@Test
	void testGetMaxTotalEnergyConsumed_nullAggregate() throws IOException {
		SearchResponse<DeviceData> searchResponse = mock(SearchResponse.class);
		Map<String, Aggregate> aggregations = new HashMap<>();

		when(searchResponse.aggregations()).thenReturn(aggregations);
		when(mockClient.search(any(SearchRequest.class), eq(DeviceData.class))).thenReturn(searchResponse);

		float result = component.getMaxTotalEnergyConsumed("customer1", "device1", TimeConstants.HOUR);

		assertEquals(0f, result, 0.01);
	}

	@Test
	void testIsOpenSearchAvailable_ioException() throws IOException {
		when(mockClient.ping()).thenThrow(new IOException("Test error"));

		boolean result = component.isOpenSearchAvailable();

		assertFalse(result);
	}
}
