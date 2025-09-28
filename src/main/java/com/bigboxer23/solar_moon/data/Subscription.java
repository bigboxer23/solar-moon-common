package com.bigboxer23.solar_moon.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
@Schema(
		description = "data object representing a customer's subscription",
		requiredProperties = {"customerId", "packs"})
@Data
public class Subscription {
	@Schema(description = "(internal) id of the customer")
	private String customerId;

	@Schema(description = "Number of subscription packs customer has purchased")
	private int packs = 0;

	private long joinDate;

	private long manualSubscriptionDate;

	public Subscription() {}

	public Subscription(String customerId, int packs, long joinDate) {
		setCustomerId(customerId);
		setPacks(packs);
		setJoinDate(joinDate);
	}

	@DynamoDbPartitionKey
	public String getCustomerId() {
		return customerId;
	}
}
