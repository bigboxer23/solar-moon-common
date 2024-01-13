package com.bigboxer23.solar_moon.search.status;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/** */
@DynamoDbBean
@Data
public class OpenSearchStatus {

	public static final String IDENTITY = "1";

	private boolean isAvailable;

	private long time;

	private String identity = IDENTITY;

	public OpenSearchStatus() {}

	public OpenSearchStatus(long time) {
		setTime(time);
		setAvailable(false);
	}

	@DynamoDbSortKey
	public long getTime() {
		return time;
	}

	@DynamoDbPartitionKey
	public String getIdentity() {
		return identity;
	}
}
