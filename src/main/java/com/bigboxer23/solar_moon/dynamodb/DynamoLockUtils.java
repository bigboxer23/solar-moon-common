package com.bigboxer23.solar_moon.dynamodb;

import static com.bigboxer23.solar_moon.dynamodb.DynamoDBLockComponent.LOCK_TABLE;

import com.amazonaws.services.dynamodbv2.AcquireLockOptions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBLockClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBLockClientOptions;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/** */
public class DynamoLockUtils {
	private static final Logger logger = LoggerFactory.getLogger(DynamoLockUtils.class);

	public static void doLockedCommand(String key, Runnable command) {
		try (AmazonDynamoDBLockClient client = new AmazonDynamoDBLockClient(
				AmazonDynamoDBLockClientOptions.builder(DynamoDbClient.builder().build(), LOCK_TABLE)
						.withTimeUnit(TimeUnit.SECONDS)
						.withLeaseDuration(15L)
						.withCreateHeartbeatBackgroundThread(false)
						.build())) {
			client.tryAcquireLock(AcquireLockOptions.builder(key)
							.withShouldSkipBlockingWait(false)
							.build())
					.ifPresent(lock -> {
						command.run();
						client.releaseLock(lock);
					});
		} catch (IOException | InterruptedException e) {
			logger.warn("doLockedCommand", e);
		}
	}
}
