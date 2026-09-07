package com.bigboxer23.solar_moon.weather;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
public class DynamoDbWeatherRepositoryTest {

	@Mock
	private DynamoDbTable<StoredWeatherData> mockTable;

	@Mock
	private PageIterable<StoredWeatherData> mockPageIterable;

	@Mock
	private Page<StoredWeatherData> mockPage;

	private TestableRepository repository;

	private static final double LATITUDE = 37.7749;
	private static final double LONGITUDE = -122.4194;

	private class TestableRepository extends DynamoDbWeatherRepository {
		@Override
		protected DynamoDbTable<StoredWeatherData> getTable() {
			return mockTable;
		}
	}

	@BeforeEach
	void setUp() {
		repository = new TestableRepository();
	}

	@Test
	void testFindByLatitudeLongitude_returnsStoredData() {
		StoredWeatherData stored = new StoredWeatherData(LATITUDE, LONGITUDE, "{}", 1000L);
		when(mockTable.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(Stream.of(mockPage));
		when(mockPage.items()).thenReturn(List.of(stored));

		assertSame(
				stored, repository.findByLatitudeLongitude(LATITUDE, LONGITUDE).orElseThrow());
	}

	@Test
	void testFindByLatitudeLongitude_withNoPages_returnsEmpty() {
		when(mockTable.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(Stream.empty());

		assertTrue(repository.findByLatitudeLongitude(LATITUDE, LONGITUDE).isEmpty());
	}

	@Test
	void testFindByLatitudeLongitude_withEmptyPage_returnsEmpty() {
		when(mockTable.query(any(QueryConditional.class))).thenReturn(mockPageIterable);
		when(mockPageIterable.stream()).thenReturn(Stream.of(mockPage));
		when(mockPage.items()).thenReturn(Collections.emptyList());

		assertTrue(repository.findByLatitudeLongitude(LATITUDE, LONGITUDE).isEmpty());
	}

	@Test
	void testTableName() {
		assertEquals("weather", repository.getTableName());
	}

	@Test
	void testObjectClass() {
		assertEquals(StoredWeatherData.class, repository.getObjectClass());
	}

	@Test
	void testAdd_stampsCreationAndPersists() {
		StoredWeatherData stored = new StoredWeatherData(LATITUDE, LONGITUDE, "{}", 1000L);

		assertSame(stored, repository.add(stored));

		verify(mockTable).putItem(stored);
	}
}
