package com.bigboxer23.solar_moon.mapping;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/** */
@DynamoDbBean
@Data
public class AttributeMap {
	private String customerId;

	private String mappingName;

	private String attribute;

	public AttributeMap() {}

	public AttributeMap(String customerId, String attribute, String mappingName) {
		setCustomerId(customerId);
		setAttribute(attribute);
		setMappingName(mappingName);
	}

	@DynamoDbPartitionKey
	public String getCustomerId() {
		return customerId;
	}

	@DynamoDbSortKey
	public String getMappingName() {
		return mappingName;
	}
}
