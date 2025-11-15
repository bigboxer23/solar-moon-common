package com.bigboxer23.solar_moon.dynamodb;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.data.DynamoDBLock;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

@ExtendWith(MockitoExtension.class)
public class DynamoDbLockRepositoryTest {

	@Mock
	private DynamoDbEnhancedClient mockClient;

	@Mock
	private DynamoDbTable<DynamoDBLock> mockTable;

	@Mock
	private PageIterable<DynamoDBLock> mockPageIterable;

	@Mock
	private Page<DynamoDBLock> mockPage;

	private TestableDynamoDbLockRepository lockRepository;

	private static final String TEST_KEY = "test-lock-key";

	private static class TestableDynamoDbLockRepository extends DynamoDbLockRepository {
		private final DynamoDbTable<DynamoDBLock> table;

		public TestableDynamoDbLockRepository(DynamoDbTable<DynamoDBLock> table) {
			this.table = table;
		}

		@Override
		protected DynamoDbTable<DynamoDBLock> getTable() {
			return table;
		}
	}

	@BeforeEach
	void setUp() {
		lockRepository = new TestableDynamoDbLockRepository(mockTable);
	}

	@Test
	void testFindByKey_withValidKey_returnsLock() {
		DynamoDBLock expectedLock = new DynamoDBLock();
		expectedLock.setKey(TEST_KEY);

		when(mockTable.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(java.util.stream.Stream.of(mockPage));
		when(mockPage.items()).thenReturn(List.of(expectedLock));

		Optional<DynamoDBLock> result = lockRepository.findByKey(TEST_KEY);

		assertTrue(result.isPresent());
		assertEquals(expectedLock, result.get());
		verify(mockTable).query(any(QueryConditional.class));
	}

	@Test
	void testFindByKey_withBlankKey_returnsEmpty() {
		Optional<DynamoDBLock> result = lockRepository.findByKey("");

		assertFalse(result.isPresent());
		verify(mockTable, never()).query(any(QueryConditional.class));
	}

	@Test
	void testFindByKey_withNullKey_returnsEmpty() {
		Optional<DynamoDBLock> result = lockRepository.findByKey(null);

		assertFalse(result.isPresent());
		verify(mockTable, never()).query(any(QueryConditional.class));
	}

	@Test
	void testFindByKey_whenNoLockExists_returnsEmpty() {
		when(mockTable.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(java.util.stream.Stream.of(mockPage));
		when(mockPage.items()).thenReturn(List.of());

		Optional<DynamoDBLock> result = lockRepository.findByKey(TEST_KEY);

		assertFalse(result.isPresent());
	}

	@Test
	void testAdd_setsCreatedAndUpdatedTimestamps() {
		DynamoDBLock lock = new DynamoDBLock();
		lock.setKey(TEST_KEY);

		lockRepository.add(lock);

		assertTrue(lock.getCreatedAt() > 0);
		assertTrue(lock.getUpdatedAt() > 0);
		verify(mockTable).putItem(lock);
	}

	@Test
	void testUpdate_setsUpdatedTimestamp() {
		DynamoDBLock lock = new DynamoDBLock();
		lock.setKey(TEST_KEY);
		long originalCreatedAt = System.currentTimeMillis();
		lock.setCreatedAt(originalCreatedAt);

		when(mockTable.updateItem(any(Consumer.class))).thenReturn(lock);

		Optional<DynamoDBLock> result = lockRepository.update(lock);

		assertTrue(result.isPresent());
		assertTrue(lock.getUpdatedAt() > 0);
		assertEquals(originalCreatedAt, lock.getCreatedAt());
		verify(mockTable).updateItem(any(Consumer.class));
	}

	@Test
	void testDelete_callsTableDeleteItem() {
		DynamoDBLock lock = new DynamoDBLock();
		lock.setKey(TEST_KEY);

		lockRepository.delete(lock);

		verify(mockTable).deleteItem(lock);
	}

	@Test
	void testGetTableName_returnsCorrectTableName() {
		assertEquals(DynamoDbLockRepository.LOCK_TABLE, lockRepository.getTableName());
	}

	@Test
	void testGetObjectClass_returnsCorrectClass() {
		assertEquals(DynamoDBLock.class, lockRepository.getObjectClass());
	}
}
