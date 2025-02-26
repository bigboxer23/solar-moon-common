package com.bigboxer23.solar_moon.dynamodb;

import static com.bigboxer23.solar_moon.dynamodb.DynamoDBLockComponent.LOCK_TABLE;

import com.amazonaws.services.dynamodbv2.AcquireLockOptions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBLockClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBLockClientOptions;
import com.amazonaws.services.dynamodbv2.model.LockCurrentlyUnavailableException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/** */
@Slf4j
public class DynamoLockUtils {
	public static void doLockedCommand(String key, Runnable command) {
		try (AmazonDynamoDBLockClient client = new AmazonDynamoDBLockClient(
				AmazonDynamoDBLockClientOptions.builder(DynamoDbClient.builder().build(), LOCK_TABLE)
						.withTimeUnit(TimeUnit.SECONDS)
						.withLeaseDuration(15L)
						.withCreateHeartbeatBackgroundThread(false)
						.build())) {
			try {
				client.tryAcquireLock(AcquireLockOptions.builder(key)
								.withShouldSkipBlockingWait(true)
								.build())
						.ifPresent(lock -> {
							log.debug("Got lock " + key);
							command.run();
							client.releaseLock(lock);
						});
			} catch (LockCurrentlyUnavailableException e) {
				log.info("Can't get lock " + key);
			}
		} catch (IOException | InterruptedException e) {
			log.warn("doLockedCommand", e);
		}
	}
}
