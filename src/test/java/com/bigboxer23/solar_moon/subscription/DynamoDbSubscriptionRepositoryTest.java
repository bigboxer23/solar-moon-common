package com.bigboxer23.solar_moon.subscription;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.data.Subscription;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

@ExtendWith(MockitoExtension.class)
public class DynamoDbSubscriptionRepositoryTest {

	@Mock
	private DynamoDbTable<Subscription> mockTable;

	@Mock
	private PageIterable<Subscription> mockPageIterable;

	@Mock
	private Page<Subscription> mockPage;

	private TestableRepository repository;

	private static final String CUSTOMER_ID = "customer-123";

	private class TestableRepository extends DynamoDbSubscriptionRepository {
		@Override
		protected DynamoDbTable<Subscription> getTable() {
			return mockTable;
		}
	}

	@BeforeEach
	void setUp() {
		repository = new TestableRepository();
	}

	@Test
	void testFindByCustomerId_returnsSubscription() {
		Subscription subscription = new Subscription(CUSTOMER_ID, 2, 0);
		when(mockTable.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(Stream.of(mockPage));
		when(mockPage.items()).thenReturn(List.of(subscription));

		assertSame(subscription, repository.findByCustomerId(CUSTOMER_ID).orElseThrow());
	}

	@Test
	void testFindByCustomerId_withBlankCustomerId_doesNotQuery() {
		assertTrue(repository.findByCustomerId("").isEmpty());

		verifyNoInteractions(mockTable);
	}

	@Test
	void testFindByCustomerId_withNullCustomerId_doesNotQuery() {
		assertTrue(repository.findByCustomerId(null).isEmpty());

		verifyNoInteractions(mockTable);
	}

	@Test
	void testFindByCustomerId_withNoPages_returnsEmpty() {
		when(mockTable.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(Stream.empty());

		assertTrue(repository.findByCustomerId(CUSTOMER_ID).isEmpty());
	}

	@Test
	void testFindByCustomerId_withEmptyPage_returnsEmpty() {
		when(mockTable.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(Stream.of(mockPage));
		when(mockPage.items()).thenReturn(Collections.emptyList());

		assertTrue(repository.findByCustomerId(CUSTOMER_ID).isEmpty());
	}

	@Test
	void testDelete_removesSubscription() {
		Subscription subscription = new Subscription(CUSTOMER_ID, 2, 0);

		repository.delete(subscription);

		verify(mockTable).deleteItem(subscription);
	}

	@Test
	void testTableName() {
		assertEquals("subscription", repository.getTableName());
	}

	@Test
	void testObjectClass() {
		assertEquals(Subscription.class, repository.getObjectClass());
	}
}
