package com.bigboxer23.solar_moon.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

/** */
@Data
@DynamoDbBean
@Schema(
		description = "data object representing an alarm state thrown by device data",
		requiredProperties = {"alarmId", "customerId"})
public class Alarm {

	public Alarm() {}

	public Alarm(String alarmId, String customerId) {
		setAlarmId(alarmId);
		setCustomerId(customerId);
	}

	public static final String CUSTOMER_INDEX = "customer-index";

	public static final String SITE_CUSTOMER_INDEX = "site-customer-index";

	public static final String SITE_STATE_INDEX = "site-state-index";

	public static final String DEVICE_STATE_INDEX = "device-state-index";

	public static final String STATE_CUSTOMER_INDEX = "state-customer-index";

	public static final String DEVICE_CUSTOMER_INDEX = "device-customer-index";

	private String alarmId;

	private String deviceId;

	private String siteId;

	private String customerId;

	private long startDate;

	private long lastUpdate;

	private long endDate;

	private String message;

	private int state;

	@DynamoDbPartitionKey
	public String getAlarmId() {
		return alarmId;
	}

	@DynamoDbSecondaryPartitionKey(indexNames = {DEVICE_STATE_INDEX, DEVICE_CUSTOMER_INDEX})
	public String getDeviceId() {
		return deviceId;
	}

	@DynamoDbSecondaryPartitionKey(indexNames = CUSTOMER_INDEX)
	@DynamoDbSecondarySortKey(indexNames = {SITE_CUSTOMER_INDEX, STATE_CUSTOMER_INDEX, DEVICE_CUSTOMER_INDEX})
	@DynamoDbSortKey
	public String getCustomerId() {
		return customerId;
	}

	@DynamoDbSecondaryPartitionKey(indexNames = {SITE_CUSTOMER_INDEX, SITE_STATE_INDEX})
	public String getSiteId() {
		return siteId;
	}

	@DynamoDbSecondaryPartitionKey(indexNames = STATE_CUSTOMER_INDEX)
	@DynamoDbSecondarySortKey(indexNames = {SITE_STATE_INDEX, DEVICE_STATE_INDEX})
	public int getState() {
		return state;
	}
}
