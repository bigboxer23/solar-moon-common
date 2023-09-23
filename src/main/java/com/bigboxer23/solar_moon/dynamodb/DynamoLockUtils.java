package com.bigboxer23.solar_moon.dynamodb;

import static com.bigboxer23.solar_moon.dynamodb.DynamoDBLockComponent.LOCK_TABLE;

import com.amazonaws.services.dynamodbv2.AcquireLockOptions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBLockClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBLockClientOptions;
import com.amazonaws.services.dynamodbv2.model.LockCurrentlyUnavailableException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/** */
public class DynamoLockUtils {
	private static final Logger logger = LoggerFactory.getLogger(DynamoLockUtils.class);

	public static void doLockedCommand(String key, String deviceName, Runnable command) {
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
							logger.info("Got lock " + key + " for device " + deviceName);
							command.run();
							client.releaseLock(lock);
						});
			} catch (LockCurrentlyUnavailableException e) {
				logger.info("Can't get lock " + key + " for device " + deviceName);
			}
		} catch (IOException | InterruptedException e) {
			logger.warn("doLockedCommand", e);
		}
	}
}
