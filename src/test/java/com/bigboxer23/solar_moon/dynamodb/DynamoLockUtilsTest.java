package com.bigboxer23.solar_moon.dynamodb;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.amazonaws.services.dynamodbv2.AcquireLockOptions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBLockClient;
import com.amazonaws.services.dynamodbv2.LockItem;
import com.amazonaws.services.dynamodbv2.model.LockCurrentlyUnavailableException;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DynamoLockUtilsTest {

	@Mock
	private AmazonDynamoDBLockClient mockClient;

	@Mock
	private LockItem mockLock;

	private static final String KEY = "virtual-device-lock";

	private class TestableDynamoLockUtils extends DynamoLockUtils {
		@Override
		protected AmazonDynamoDBLockClient getLockClient() {
			return mockClient;
		}
	}

	@Test
	void testExecuteLocked_whenLockAcquired_runsCommandAndReleasesLock() throws Exception {
		AtomicBoolean ran = new AtomicBoolean(false);
		when(mockClient.tryAcquireLock(any(AcquireLockOptions.class))).thenReturn(Optional.of(mockLock));

		new TestableDynamoLockUtils().executeLocked(KEY, () -> ran.set(true));

		assertTrue(ran.get());
		verify(mockClient).releaseLock(mockLock);
		verify(mockClient).close();
	}

	@Test
	void testExecuteLocked_whenLockNotAvailable_doesNotRunCommand() throws Exception {
		AtomicBoolean ran = new AtomicBoolean(false);
		when(mockClient.tryAcquireLock(any(AcquireLockOptions.class))).thenReturn(Optional.empty());

		new TestableDynamoLockUtils().executeLocked(KEY, () -> ran.set(true));

		assertFalse(ran.get());
		verify(mockClient, never()).releaseLock(any(LockItem.class));
		verify(mockClient).close();
	}

	@Test
	void testExecuteLocked_whenLockHeldElsewhere_doesNotRunCommandOrThrow() throws Exception {
		AtomicBoolean ran = new AtomicBoolean(false);
		when(mockClient.tryAcquireLock(any(AcquireLockOptions.class)))
				.thenThrow(new LockCurrentlyUnavailableException("held"));

		assertDoesNotThrow(() -> new TestableDynamoLockUtils().executeLocked(KEY, () -> ran.set(true)));

		assertFalse(ran.get());
		verify(mockClient, never()).releaseLock(any(LockItem.class));
	}

	@Test
	void testExecuteLocked_whenInterrupted_doesNotRunCommandOrThrow() throws Exception {
		AtomicBoolean ran = new AtomicBoolean(false);
		when(mockClient.tryAcquireLock(any(AcquireLockOptions.class))).thenThrow(new InterruptedException("stopped"));

		assertDoesNotThrow(() -> new TestableDynamoLockUtils().executeLocked(KEY, () -> ran.set(true)));

		assertFalse(ran.get());
	}

	@Test
	void testExecuteLocked_whenClientCloseFails_doesNotThrow() throws Exception {
		when(mockClient.tryAcquireLock(any(AcquireLockOptions.class))).thenReturn(Optional.of(mockLock));
		doThrow(new IOException("close failed")).when(mockClient).close();

		assertDoesNotThrow(() -> new TestableDynamoLockUtils().executeLocked(KEY, () -> {}));
	}

	@Test
	void testExecuteLocked_propagatesCommandFailureAfterAcquiringLock() throws Exception {
		when(mockClient.tryAcquireLock(any(AcquireLockOptions.class))).thenReturn(Optional.of(mockLock));

		assertThrows(IllegalStateException.class, () -> new TestableDynamoLockUtils().executeLocked(KEY, () -> {
			throw new IllegalStateException("command blew up");
		}));

		verify(mockClient, never()).releaseLock(any(LockItem.class));
	}
}
