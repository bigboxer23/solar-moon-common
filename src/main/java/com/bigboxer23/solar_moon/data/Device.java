package com.bigboxer23.solar_moon.data;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

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

	@DynamoDbPartitionKey
	public String getId() {
		return id;
	}

	public boolean isPushedDevice() {
		return getPassword() == null && getUser() == null;
	}
}
