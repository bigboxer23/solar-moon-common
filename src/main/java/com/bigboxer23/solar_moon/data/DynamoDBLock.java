package com.bigboxer23.solar_moon.data;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/** */
@Data
@DynamoDbBean
public class DynamoDBLock {
	private String key;

	private String leaseDuration;

	private String ownerName;

	private String recordVersionNumber;

	@DynamoDbPartitionKey
	public String getKey() {
		return key;
	}
}
