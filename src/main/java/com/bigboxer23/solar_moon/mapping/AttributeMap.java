package com.bigboxer23.solar_moon.mapping;

import com.bigboxer23.solar_moon.data.AuditableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/** */
@DynamoDbBean
@Data
@EqualsAndHashCode(callSuper = true)
public class AttributeMap extends AuditableEntity {
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
