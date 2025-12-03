package com.bigboxer23.solar_moon.mapping;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

@ExtendWith(MockitoExtension.class)
public class DynamoDbMappingRepositoryTest {

	@Mock
	private DynamoDbTable<AttributeMap> mockTable;

	@Mock
	private PageIterable<AttributeMap> mockPageIterable;

	@Mock
	private SdkIterable<AttributeMap> mockSdkIterable;

	private TestableDynamoDbMappingRepository repository;

	private static final String CUSTOMER_ID = "customer-123";
	private static final String MAPPING_NAME = "mapping-test";
	private static final String ATTRIBUTE = "attribute-value";

	private static class TestableDynamoDbMappingRepository extends DynamoDbMappingRepository {
		private final DynamoDbTable<AttributeMap> table;

		public TestableDynamoDbMappingRepository(DynamoDbTable<AttributeMap> table) {
			this.table = table;
		}

		@Override
		protected DynamoDbTable<AttributeMap> getTable() {
			return table;
		}
	}

	@BeforeEach
	void setUp() {
		repository = new TestableDynamoDbMappingRepository(mockTable);
	}

	@Test
	void testFindByCustomerId_withExistingMappings_returnsList() {
		List<AttributeMap> expectedMappings = List.of(createTestAttributeMap());
		when(mockTable.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.items()).thenReturn(mockSdkIterable);
		when(mockSdkIterable.stream()).thenReturn(expectedMappings.stream());

		List<AttributeMap> result = repository.findByCustomerId(CUSTOMER_ID);

		assertEquals(1, result.size());
		assertEquals(expectedMappings.getFirst(), result.getFirst());
		verify(mockTable).query(any(QueryConditional.class));
	}

	@Test
	void testFindByCustomerId_withNoMappings_returnsEmptyList() {
		when(mockTable.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.items()).thenReturn(mockSdkIterable);
		when(mockSdkIterable.stream()).thenReturn(java.util.stream.Stream.empty());

		List<AttributeMap> result = repository.findByCustomerId(CUSTOMER_ID);

		assertTrue(result.isEmpty());
	}

	@Test
	void testFindByCustomerId_withMultiplePages_returnsAllMappings() {
		AttributeMap mapping1 = createTestAttributeMap();
		AttributeMap mapping2 = new AttributeMap(CUSTOMER_ID, "attribute2", "mapping2");
		List<AttributeMap> allMappings = List.of(mapping1, mapping2);

		when(mockTable.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.items()).thenReturn(mockSdkIterable);
		when(mockSdkIterable.stream()).thenReturn(allMappings.stream());

		List<AttributeMap> result = repository.findByCustomerId(CUSTOMER_ID);

		assertEquals(2, result.size());
	}

	@Test
	void testFindByCustomerIdAndMappingName_withExistingMapping_returnsMapping() {
		AttributeMap expectedMapping = createTestAttributeMap();
		when(mockTable.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.items()).thenReturn(mockSdkIterable);
		when(mockSdkIterable.stream()).thenReturn(java.util.stream.Stream.of(expectedMapping));

		Optional<AttributeMap> result = repository.findByCustomerIdAndMappingName(CUSTOMER_ID, MAPPING_NAME);

		assertTrue(result.isPresent());
		assertEquals(expectedMapping, result.get());
		verify(mockTable).query(any(QueryConditional.class));
	}

	@Test
	void testFindByCustomerIdAndMappingName_withNoMapping_returnsEmpty() {
		when(mockTable.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.items()).thenReturn(mockSdkIterable);
		when(mockSdkIterable.stream()).thenReturn(java.util.stream.Stream.empty());

		Optional<AttributeMap> result = repository.findByCustomerIdAndMappingName(CUSTOMER_ID, MAPPING_NAME);

		assertFalse(result.isPresent());
	}

	@Test
	void testFindByCustomerIdAndMappingName_withEmptyItemsList_returnsEmpty() {
		when(mockTable.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.items()).thenReturn(mockSdkIterable);
		when(mockSdkIterable.stream()).thenReturn(java.util.stream.Stream.empty());

		Optional<AttributeMap> result = repository.findByCustomerIdAndMappingName(CUSTOMER_ID, MAPPING_NAME);

		assertFalse(result.isPresent());
	}

	@Test
	void testDeleteByCustomerIdAndMappingName_deletesMapping() {
		repository.deleteByCustomerIdAndMappingName(CUSTOMER_ID, MAPPING_NAME);

		verify(mockTable).deleteItem(any(Consumer.class));
	}

	@Test
	void testDeleteByCustomerId_withMultipleMappings_deletesAll() {
		AttributeMap mapping1 = createTestAttributeMap();
		AttributeMap mapping2 = new AttributeMap(CUSTOMER_ID, "attribute2", "mapping2");
		List<AttributeMap> mappings = List.of(mapping1, mapping2);

		when(mockTable.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.items()).thenReturn(mockSdkIterable);
		when(mockSdkIterable.stream()).thenReturn(mappings.stream());

		repository.deleteByCustomerId(CUSTOMER_ID);

		verify(mockTable, times(2)).deleteItem(any(Consumer.class));
	}

	@Test
	void testDeleteByCustomerId_withNoMappings_deletesNothing() {
		when(mockTable.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.items()).thenReturn(mockSdkIterable);
		when(mockSdkIterable.stream()).thenReturn(java.util.stream.Stream.empty());

		repository.deleteByCustomerId(CUSTOMER_ID);

		verify(mockTable, never()).deleteItem(any(Consumer.class));
	}

	@Test
	void testGetTableName_returnsCorrectTableName() {
		assertEquals("mappings", repository.getTableName());
	}

	@Test
	void testGetObjectClass_returnsCorrectClass() {
		assertEquals(AttributeMap.class, repository.getObjectClass());
	}

	private AttributeMap createTestAttributeMap() {
		return new AttributeMap(CUSTOMER_ID, ATTRIBUTE, MAPPING_NAME);
	}
}
