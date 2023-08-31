package com.bigboxer23.solar_moon.data;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

/** */
@Data
@DynamoDbBean
public class Device {
	private String id;

	private String clientId;

	private String name;

	private String address;

	private String site;

	private String user;

	private String password;

	private String type;

	private String deviceName;

	private boolean subtraction;

	@DynamoDbSecondaryPartitionKey(indexNames = "name-clientId-index")
	public String getName() {
		return name;
	}
	@DynamoDbSecondaryPartitionKey(indexNames = "deviceName-clientId-index")
	public String getDeviceName() {
		return deviceName;
	}
	@DynamoDbPartitionKey
	public String getId() {
		return id;
	}

	@DynamoDbSecondarySortKey(indexNames = {"name-clientId-index", "deviceName-clientId-index"})
	@DynamoDbSortKey
	public String getClientId() {
		return clientId;
	}
	public boolean isPushedDevice() {
		return getPassword() == null && getUser() == null;
	}
}
