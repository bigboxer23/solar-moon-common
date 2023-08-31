package com.bigboxer23.solar_moon.data;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

/** */
@Data
@DynamoDbBean
public class Device {
	public static final String NAME_INDEX = "name-clientId-index";

	public static final String DEVICE_NAME_INDEX = "deviceName-clientId-index";

	public static final String DEVICE_KEY_INDEX = "deviceKey-index";

	public static final String TABLE_NAME = "devices";

	public static final String VIRTUAL_TABLE_NAME = "virtualDevices";

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

	private String deviceKey;

	@DynamoDbSecondaryPartitionKey(indexNames = NAME_INDEX)
	public String getName() {
		return name;
	}

	@DynamoDbSecondaryPartitionKey(indexNames = DEVICE_NAME_INDEX)
	public String getDeviceName() {
		return deviceName;
	}

	@DynamoDbPartitionKey
	public String getId() {
		return id;
	}

	@DynamoDbSecondarySortKey(indexNames = {NAME_INDEX, DEVICE_NAME_INDEX})
	@DynamoDbSortKey
	public String getClientId() {
		return clientId;
	}

	@DynamoDbSecondaryPartitionKey(indexNames = DEVICE_KEY_INDEX)
	public String getDeviceKey() {
		return deviceKey;
	}

	public boolean isPushedDevice() {
		return getPassword() == null && getUser() == null;
	}
}
