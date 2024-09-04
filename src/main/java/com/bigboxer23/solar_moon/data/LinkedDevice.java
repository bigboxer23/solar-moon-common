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
	String criticalAlarm;

	@Schema(description = "Content of informative alarm")
	String informativeAlarm;

	@Schema(description = "date data was retrieved from")
	long date;

	public LinkedDevice() {}

	public LinkedDevice(String id, String customerId, String criticalAlarm, String informativeAlarm, long date) {
		setId(id);
		setCustomerId(customerId);
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
