package com.bigboxer23.solar_moon.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

/** */
@DynamoDbBean
@Schema(
		description = "data object representing a customer",
		requiredProperties = {"email", "customerId", "accessKey"})
@Data
public class Customer {
	public static final String ACCESS_KEY_INDEX = "accessKey-index";

	public static final String CUSTOMER_ID_INDEX = "customerId-index";

	public Customer() {}

	public Customer(String customerId) {
		setCustomerId(customerId);
	}

	public Customer(String customerId, String email, String accessKey, String name) {
		this(customerId);
		setEmail(email);
		setAccessKey(accessKey);
		setName(name);
	}

	@Schema(description = "email of customer")
	private String email;

	@Schema(description = "access id to allow for public usage")
	private String accessKey;

	@Schema(description = "(internal) id of the customer")
	private String customerId;

	private String name;

	private String address1;

	private String address2;

	private String city;

	private String country;

	private String state;

	private String zip;

	private boolean admin = false;

	private boolean active = true;

	@DynamoDbPartitionKey
	public String getEmail() {
		return email;
	}

	@DynamoDbSecondaryPartitionKey(indexNames = ACCESS_KEY_INDEX)
	public String getAccessKey() {
		return accessKey;
	}

	@DynamoDbSecondaryPartitionKey(indexNames = CUSTOMER_ID_INDEX)
	public String getCustomerId() {
		return customerId;
	}
}
