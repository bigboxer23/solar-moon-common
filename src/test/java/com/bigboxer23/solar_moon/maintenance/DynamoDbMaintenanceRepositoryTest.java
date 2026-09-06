package com.bigboxer23.solar_moon.maintenance;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

@ExtendWith(MockitoExtension.class)
public class DynamoDbMaintenanceRepositoryTest {

	@Mock
	private DynamoDbTable<MaintenanceMode> mockTable;

	private TestableRepository repository;

	private class TestableRepository extends DynamoDbMaintenanceRepository {
		@Override
		protected DynamoDbTable<MaintenanceMode> getTable() {
			return mockTable;
		}
	}

	@BeforeEach
	void setUp() {
		repository = new TestableRepository();
	}

	@Test
	void testFindMaintenanceMode_whenEnabled_returnsMode() {
		MaintenanceMode mode = new MaintenanceMode();
		when(mockTable.getItem(any(MaintenanceMode.class))).thenReturn(mode);

		assertSame(mode, repository.findMaintenanceMode().orElseThrow());
	}

	@Test
	void testFindMaintenanceMode_whenNotEnabled_returnsEmpty() {
		when(mockTable.getItem(any(MaintenanceMode.class))).thenReturn(null);

		assertTrue(repository.findMaintenanceMode().isEmpty());
	}

	@Test
	void testEnableMaintenanceMode_writesItem() {
		repository.enableMaintenanceMode();

		verify(mockTable)
				.updateItem(ArgumentMatchers
						.<Consumer<
										software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest
														.Builder<
												MaintenanceMode>>>
								any());
	}

	@Test
	void testDisableMaintenanceMode_deletesItem() {
		repository.disableMaintenanceMode();

		verify(mockTable).deleteItem(any(MaintenanceMode.class));
	}

	@Test
	void testTableName() {
		assertEquals("maintenanceMode", repository.getTableName());
	}

	@Test
	void testObjectClass() {
		assertEquals(MaintenanceMode.class, repository.getObjectClass());
	}
}
