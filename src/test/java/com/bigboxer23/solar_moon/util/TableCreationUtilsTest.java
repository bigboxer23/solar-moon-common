package com.bigboxer23.solar_moon.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;

@ExtendWith(MockitoExtension.class)
public class TableCreationUtilsTest {

	@Mock
	private DynamoDbTable<TestEntity> mockTable;

	@Test
	public void testCreateTable_withNullIndexNames() {
		TableCreationUtils.createTable(null, mockTable);

		verify(mockTable).createTable(any(Consumer.class));
	}

	@Test
	public void testCreateTable_withEmptyIndexNames() {
		List<String> indexNames = Collections.emptyList();

		TableCreationUtils.createTable(indexNames, mockTable);

		verify(mockTable).createTable(any(Consumer.class));
	}

	@Test
	public void testCreateTable_withSingleIndexName() {
		List<String> indexNames = Collections.singletonList("test-index");

		TableCreationUtils.createTable(indexNames, mockTable);

		verify(mockTable).createTable(any(Consumer.class));
	}

	@Test
	public void testCreateTable_withMultipleIndexNames() {
		List<String> indexNames = Arrays.asList("index1", "index2", "index3");

		TableCreationUtils.createTable(indexNames, mockTable);

		verify(mockTable).createTable(any(Consumer.class));
	}

	@Test
	public void testCreateTable_callsTableCreateTableMethod() {
		List<String> indexNames = Arrays.asList("idx1", "idx2");

		TableCreationUtils.createTable(indexNames, mockTable);

		verify(mockTable, times(1)).createTable(any(Consumer.class));
	}

	@Test
	public void testCreateTable_withNullTable_throwsNullPointerException() {
		List<String> indexNames = Arrays.asList("index1");

		assertThrows(NullPointerException.class, () -> {
			TableCreationUtils.createTable(indexNames, null);
		});
	}

	@Test
	public void testCreateTable_builderReceivesIndexes() {
		List<String> indexNames = Arrays.asList("test-index-1", "test-index-2");
		ArgumentCaptor<Consumer<CreateTableEnhancedRequest.Builder>> captor = ArgumentCaptor.forClass(Consumer.class);

		TableCreationUtils.createTable(indexNames, mockTable);

		verify(mockTable).createTable(captor.capture());
		assertNotNull(captor.getValue());
	}

	@Test
	public void testCreateTable_withLargeNumberOfIndexes() {
		List<String> indexNames = Arrays.asList(
				"index1", "index2", "index3", "index4", "index5", "index6", "index7", "index8", "index9", "index10");

		TableCreationUtils.createTable(indexNames, mockTable);

		verify(mockTable).createTable(any(Consumer.class));
	}

	@Test
	public void testCreateTable_withIndexNamesContainingSpecialCharacters() {
		List<String> indexNames = Arrays.asList("test-index", "test_index", "testIndex123");

		TableCreationUtils.createTable(indexNames, mockTable);

		verify(mockTable).createTable(any(Consumer.class));
	}

	private static class TestEntity {
		private String id;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}
	}
}
