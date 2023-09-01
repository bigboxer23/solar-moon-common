package com.bigboxer23.solar_moon.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

/** */
@Data
@DynamoDbBean
@Schema(
		description = "data object representing a solar energy device",
		requiredProperties = {"id", "clientId"})
public class Device {
	public static final String NAME_INDEX = "name-clientId-index";

	public static final String DEVICE_NAME_INDEX = "deviceName-clientId-index";

	public static final String DEVICE_KEY_INDEX = "deviceKey-index";

	public static final String CLIENT_INDEX = "clientId-index";

	public static final String TABLE_NAME = "devices";

	@Schema(description = "(internal) id of the device")
	private String id;

	@Schema(description = "client id associated with device")
	private String clientId;

	@Schema(description = "Devices display name")
	private String name;

	@Schema(
			description = "http url to access the device's xml data",
			example = "http://127.0.0.1/setup/devicexml.cgi?ADDRESS=10&TYPE=DATA")
	private String address;

	@Schema(description = "virtual site device's name associated with the device")
	private String site;

	@Schema(description = "username to access device")
	private String user;

	@Schema(description = "password to access the device")
	private String password;

	@Schema(description = "type of device")
	private String type;

	@Schema(description = "device's self reported name")
	private String deviceName;

	@Schema(description = "Does the device use subtraction mode for roll up", example = "true|false")
	private boolean subtraction;

	@Schema(description = "key to use externally when sending the service device specific data")
	private String deviceKey;

	@Schema(description = "Is this device virtual (site)?", example = "true|false")
	private boolean virtual;

	public Device() {}

	public Device(String id, String clientId) {
		setClientId(clientId);
		setId(id);
	}

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

	@DynamoDbSecondaryPartitionKey(indexNames = CLIENT_INDEX)
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
