package com.bigboxer23.solar_moon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

/** */
public abstract class AbstractDynamodbComponent<T> {
	protected static final Logger logger = LoggerFactory.getLogger(AbstractDynamodbComponent.class);

	private DynamoDbEnhancedClient client;

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
