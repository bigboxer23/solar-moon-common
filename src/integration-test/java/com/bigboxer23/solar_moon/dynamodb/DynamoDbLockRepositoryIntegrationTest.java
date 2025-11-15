package com.bigboxer23.solar_moon.dynamodb;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.data.DynamoDBLock;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class DynamoDbLockRepositoryIntegrationTest {

	private static DynamoDbLockRepository lockRepository;

	private static final String TEST_LOCK_KEY = "integration-test-lock";
	private static final String TEST_OWNER = "test-owner";
	private static final String TEST_LEASE_DURATION = "15";

	@BeforeAll
	public static void beforeAll() {
		lockRepository = new DynamoDbLockRepository();
		lockRepository.createTable();
	}

	@AfterAll
	public static void afterAll() {
		Optional<DynamoDBLock> lock = lockRepository.findByKey(TEST_LOCK_KEY);
		lock.ifPresent(lockRepository::delete);
	}

	@Test
	public void testAddLock() {
		DynamoDBLock lock = new DynamoDBLock();
		lock.setKey(TEST_LOCK_KEY);
		lock.setOwnerName(TEST_OWNER);
		lock.setLeaseDuration(TEST_LEASE_DURATION);

		DynamoDBLock savedLock = lockRepository.add(lock);

		assertNotNull(savedLock);
		assertTrue(savedLock.getCreatedAt() > 0);
		assertTrue(savedLock.getUpdatedAt() > 0);
		assertEquals(TEST_LOCK_KEY, savedLock.getKey());
		assertEquals(TEST_OWNER, savedLock.getOwnerName());
		assertEquals(TEST_LEASE_DURATION, savedLock.getLeaseDuration());
	}

	@Test
	public void testFindByKey() {
		DynamoDBLock lock = new DynamoDBLock();
		lock.setKey(TEST_LOCK_KEY);
		lock.setOwnerName(TEST_OWNER);
		lock.setLeaseDuration(TEST_LEASE_DURATION);
		lockRepository.add(lock);

		Optional<DynamoDBLock> foundLock = lockRepository.findByKey(TEST_LOCK_KEY);

		assertTrue(foundLock.isPresent());
		assertEquals(TEST_LOCK_KEY, foundLock.get().getKey());
		assertEquals(TEST_OWNER, foundLock.get().getOwnerName());
	}

	@Test
	public void testUpdateLock() {
		DynamoDBLock lock = new DynamoDBLock();
		lock.setKey(TEST_LOCK_KEY);
		lock.setOwnerName(TEST_OWNER);
		lock.setLeaseDuration(TEST_LEASE_DURATION);
		lockRepository.add(lock);

		lock.setOwnerName("updated-owner");
		Optional<DynamoDBLock> updatedLock = lockRepository.update(lock);

		assertTrue(updatedLock.isPresent());
		assertEquals("updated-owner", updatedLock.get().getOwnerName());
		assertTrue(updatedLock.get().getUpdatedAt() > updatedLock.get().getCreatedAt());
	}

	@Test
	public void testDeleteLock() {
		DynamoDBLock lock = new DynamoDBLock();
		lock.setKey(TEST_LOCK_KEY);
		lock.setOwnerName(TEST_OWNER);
		lock.setLeaseDuration(TEST_LEASE_DURATION);
		lockRepository.add(lock);

		Optional<DynamoDBLock> foundLock = lockRepository.findByKey(TEST_LOCK_KEY);
		assertTrue(foundLock.isPresent());

		lockRepository.delete(foundLock.get());

		Optional<DynamoDBLock> deletedLock = lockRepository.findByKey(TEST_LOCK_KEY);
		assertFalse(deletedLock.isPresent());
	}

	@Test
	public void testFindByKey_withNonExistentKey_returnsEmpty() {
		Optional<DynamoDBLock> lock = lockRepository.findByKey("non-existent-key");

		assertFalse(lock.isPresent());
	}
}
