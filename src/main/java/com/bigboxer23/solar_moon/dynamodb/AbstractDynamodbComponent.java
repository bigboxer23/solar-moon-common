package com.bigboxer23.solar_moon.dynamodb;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

/** */
public abstract class AbstractDynamodbComponent<T> {
	private static DynamoDbEnhancedClient client;

	private DynamoDbEnhancedClient getClient() {
		if (client == null) {
			client = DynamoDbEnhancedClient.create();
		}
		return client;
	}

	protected DynamoDbTable<T> getTable() {
		return getClient().table(getTableName(), TableSchema.fromBean(getObjectClass()));
	}

	protected abstract String getTableName();

	protected abstract Class<T> getObjectClass();
}
