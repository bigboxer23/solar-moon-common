package com.bigboxer23.solar_moon.data;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

/** */
@Data
@DynamoDbBean
public class DeviceUpdateData {
	public static final String IDENTITY_UPDATE_INDEX = "identity-update-index";

	public static final String INDENTITY = "1";

	private String deviceId;

	private long lastUpdate;

	private String identity = INDENTITY;

	public DeviceUpdateData() {}

	public DeviceUpdateData(String id, long time) {
		setDeviceId(id);
		setLastUpdate(time);
	}

	@DynamoDbPartitionKey
	public String getDeviceId() {
		return deviceId;
	}

	@DynamoDbSecondarySortKey(indexNames = {IDENTITY_UPDATE_INDEX})
	public long getLastUpdate() {
		return lastUpdate;
	}

	@DynamoDbSecondaryPartitionKey(indexNames = IDENTITY_UPDATE_INDEX)
	public String getIdentity() {
		return identity;
	}
}
