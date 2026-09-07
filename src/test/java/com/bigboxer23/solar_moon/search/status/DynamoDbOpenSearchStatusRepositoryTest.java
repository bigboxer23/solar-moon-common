package com.bigboxer23.solar_moon.search.status;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

@ExtendWith(MockitoExtension.class)
public class DynamoDbOpenSearchStatusRepositoryTest {

	@Mock
	private DynamoDbTable<OpenSearchStatus> mockTable;

	@Mock
	private PageIterable<OpenSearchStatus> mockPageIterable;

	@Mock
	private Page<OpenSearchStatus> mockPage;

	private TestableRepository repository;

	private class TestableRepository extends DynamoDbOpenSearchStatusRepository {
		@Override
		protected DynamoDbTable<OpenSearchStatus> getTable() {
			return mockTable;
		}
	}

	@BeforeEach
	void setUp() {
		repository = new TestableRepository();
	}

	@Test
	void testStoreFailure_writesStatusRow() {
		repository.storeFailure();

		verify(mockTable)
				.updateItem(ArgumentMatchers.<Consumer<UpdateItemEnhancedRequest.Builder<OpenSearchStatus>>>any());
	}

	@Test
	void testHasFailureWithinLastThirtyMinutes_whenRecentFailureExists_returnsTrue() {
		when(mockTable.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(Stream.of(mockPage));
		when(mockPage.items()).thenReturn(List.of(new OpenSearchStatus(System.currentTimeMillis())));

		assertTrue(repository.hasFailureWithinLastThirtyMinutes());
	}

	@Test
	void testHasFailureWithinLastThirtyMinutes_withNoPages_returnsFalse() {
		when(mockTable.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(Stream.empty());

		assertFalse(repository.hasFailureWithinLastThirtyMinutes());
	}

	@Test
	void testHasFailureWithinLastThirtyMinutes_withEmptyPage_returnsFalse() {
		when(mockTable.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(Stream.of(mockPage));
		when(mockPage.items()).thenReturn(Collections.emptyList());

		assertFalse(repository.hasFailureWithinLastThirtyMinutes());
	}

	@Test
	void testGetMostRecentStatus_returnsNewestRow() {
		OpenSearchStatus status = new OpenSearchStatus(System.currentTimeMillis());
		when(mockTable.query(ArgumentMatchers.<Consumer<QueryEnhancedRequest.Builder>>any()))
				.thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(Stream.of(mockPage));
		when(mockPage.items()).thenReturn(List.of(status));

		assertSame(status, repository.getMostRecentStatus().orElseThrow());
	}

	@Test
	void testGetMostRecentStatus_withNoRows_returnsEmpty() {
		when(mockTable.query(ArgumentMatchers.<Consumer<QueryEnhancedRequest.Builder>>any()))
				.thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(Stream.empty());

		assertTrue(repository.getMostRecentStatus().isEmpty());
	}

	@Test
	void testTableName() {
		assertEquals("openSearchStatus", repository.getTableName());
	}

	@Test
	void testObjectClass() {
		assertEquals(OpenSearchStatus.class, repository.getObjectClass());
	}
}
