package com.bigboxer23.solar_moon.maintenance;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MaintenanceComponentTest {

	@Mock
	private MaintenanceRepository mockRepository;

	private TestableMaintenanceComponent maintenanceComponent;

	private static class TestableMaintenanceComponent extends MaintenanceComponent {
		private final MaintenanceRepository repository;

		public TestableMaintenanceComponent(MaintenanceRepository repository) {
			this.repository = repository;
		}

		@Override
		protected MaintenanceRepository getRepository() {
			return repository;
		}
	}

	@BeforeEach
	void setUp() {
		maintenanceComponent = new TestableMaintenanceComponent(mockRepository);
	}

	@Test
	void testIsInMaintenanceMode_whenModeIsEnabled_returnsTrue() {
		MaintenanceMode mode = new MaintenanceMode();
		mode.setInMaintenanceMode(true);
		when(mockRepository.findMaintenanceMode()).thenReturn(Optional.of(mode));

		boolean result = maintenanceComponent.isInMaintenanceMode();

		assertTrue(result);
		verify(mockRepository).findMaintenanceMode();
	}

	@Test
	void testIsInMaintenanceMode_whenModeIsDisabled_returnsFalse() {
		MaintenanceMode mode = new MaintenanceMode();
		mode.setInMaintenanceMode(false);
		when(mockRepository.findMaintenanceMode()).thenReturn(Optional.of(mode));

		boolean result = maintenanceComponent.isInMaintenanceMode();

		assertFalse(result);
		verify(mockRepository).findMaintenanceMode();
	}

	@Test
	void testIsInMaintenanceMode_whenModeNotFound_returnsFalse() {
		when(mockRepository.findMaintenanceMode()).thenReturn(Optional.empty());

		boolean result = maintenanceComponent.isInMaintenanceMode();

		assertFalse(result);
		verify(mockRepository).findMaintenanceMode();
	}

	@Test
	void testEnableMaintenanceMode_whenEnableIsTrue_callsRepositoryEnable() {
		maintenanceComponent.enableMaintenanceMode(true);

		verify(mockRepository).enableMaintenanceMode();
		verify(mockRepository, never()).disableMaintenanceMode();
	}

	@Test
	void testEnableMaintenanceMode_whenEnableIsFalse_callsRepositoryDisable() {
		maintenanceComponent.enableMaintenanceMode(false);

		verify(mockRepository).disableMaintenanceMode();
		verify(mockRepository, never()).enableMaintenanceMode();
	}
}
