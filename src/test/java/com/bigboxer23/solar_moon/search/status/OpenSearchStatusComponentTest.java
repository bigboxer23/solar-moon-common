package com.bigboxer23.solar_moon.search.status;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OpenSearchStatusComponentTest {

	@Mock
	private OpenSearchStatusRepository repository;

	private OpenSearchStatusComponent component;

	@BeforeEach
	public void setup() {
		component = new OpenSearchStatusComponent() {
			@Override
			protected OpenSearchStatusRepository getRepository() {
				return repository;
			}
		};
	}

	@Test
	public void testStoreFailure() {
		component.storeFailure();
		verify(repository, times(1)).storeFailure();
	}

	@Test
	public void testHasFailureWithinLastThirtyMinutes_true() {
		when(repository.hasFailureWithinLastThirtyMinutes()).thenReturn(true);
		assertTrue(component.hasFailureWithinLastThirtyMinutes());
		verify(repository, times(1)).hasFailureWithinLastThirtyMinutes();
	}

	@Test
	public void testHasFailureWithinLastThirtyMinutes_false() {
		when(repository.hasFailureWithinLastThirtyMinutes()).thenReturn(false);
		assertFalse(component.hasFailureWithinLastThirtyMinutes());
		verify(repository, times(1)).hasFailureWithinLastThirtyMinutes();
	}

	@Test
	public void testGetMostRecentStatus_present() {
		OpenSearchStatus status = new OpenSearchStatus(System.currentTimeMillis());
		when(repository.getMostRecentStatus()).thenReturn(Optional.of(status));

		Optional<OpenSearchStatus> result = component.getMostRecentStatus();

		assertTrue(result.isPresent());
		assertEquals(status, result.get());
		verify(repository, times(1)).getMostRecentStatus();
	}

	@Test
	public void testGetMostRecentStatus_empty() {
		when(repository.getMostRecentStatus()).thenReturn(Optional.empty());

		Optional<OpenSearchStatus> result = component.getMostRecentStatus();

		assertFalse(result.isPresent());
		verify(repository, times(1)).getMostRecentStatus();
	}
}
