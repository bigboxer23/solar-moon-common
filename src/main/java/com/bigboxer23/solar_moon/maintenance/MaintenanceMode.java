package com.bigboxer23.solar_moon.maintenance;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/** */
@Data
@DynamoDbBean
public class MaintenanceMode {
	private String maintenanceMode;

	private boolean isInMaintenanceMode;

	public MaintenanceMode() {
		maintenanceMode = "y";
		isInMaintenanceMode = true;
	}
	@DynamoDbPartitionKey
	public String getMaintenanceMode() {
		return maintenanceMode;
	}
}
