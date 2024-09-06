package com.bigboxer23.solar_moon.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

/** */
@Data
@DynamoDbBean
@Schema(
		description = "data object representing an inverter's data linked to another collection device",
		requiredProperties = {"id", "customerId"})
public class LinkedDevice {
	@Schema(description = "id of the linked device")
	String id;

	@Schema(description = "customer id associated with device")
	String customerId;

	@Schema(description = "Content of critical alarm")
	int criticalAlarm = -1;

	@Schema(description = "Content of informative alarm")
	int informativeAlarm = -1;

	@Schema(description = "date data was retrieved from")
	long date;

	public LinkedDevice() {}

	public LinkedDevice(String id, String customerId) {
		setId(id);
		setCustomerId(customerId);
	}

	public LinkedDevice(String id, String customerId, int criticalAlarm, int informativeAlarm, long date) {
		this(id, customerId);
		setCriticalAlarm(criticalAlarm);
		setInformativeAlarm(informativeAlarm);
		setDate(date);
	}

	@DynamoDbPartitionKey
	public String getId() {
		return id;
	}

	@DynamoDbSortKey
	public String getCustomerId() {
		return customerId;
	}
}
