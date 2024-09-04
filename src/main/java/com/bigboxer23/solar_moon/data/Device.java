package com.bigboxer23.solar_moon.data;

import com.bigboxer23.solar_moon.device.DeviceComponent;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Optional;
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

	public static final String SITE_INDEX = "site-customerId-index";

	public static final String SITEID_INDEX = "siteId-customerId-index";

	public static final String VIRTUAL_INDEX = "virtual-index";

	public static final String IS_SITE_INDEX = "isSite-index";

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

	private String siteId;

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

	@Schema(description = "String representation of virtual for dynamodb index", example = "true|false")
	private String virtualIndex;

	@Schema(description = "", example = " | 1")
	private String isSite;

	@Schema(
			description = "Should this device be considered active for the purpose of alerting?",
			example = "true|false")
	private boolean disabled;

	@Schema(description = "Should notifications be sent if alert state is detected?", example = "true|false")
	private boolean notificationsDisabled;

	private String mock;

	private String city;

	private String state;

	private String country;

	private double latitude = -1;

	private double longitude = -1;

	private String serialNumber;

	public Device() {
		setVirtual(false);
	}

	public Device(String id, String clientId) {
		this();
		setClientId(clientId);
		setId(id);
	}

	public Device(String id, String clientId, String deviceName) {
		this();
		setClientId(clientId);
		setId(id);
		setDeviceName(deviceName);
		setName(deviceName);
		setSite(DeviceComponent.NO_SITE);
		setSiteId(DeviceComponent.NO_SITE);
	}

	public boolean isPushedDevice() {
		return (getPassword() == null || getPassword().isBlank())
				&& (getUser() == null || getUser().isBlank());
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
	@DynamoDbSecondarySortKey(indexNames = {NAME_INDEX, DEVICE_NAME_INDEX, SITE_INDEX, SITEID_INDEX})
	@DynamoDbSortKey
	public String getClientId() {
		return clientId;
	}

	@DynamoDbSecondaryPartitionKey(indexNames = DEVICE_KEY_INDEX)
	public String getDeviceKey() {
		return deviceKey;
	}

	@DynamoDbSecondaryPartitionKey(indexNames = SITE_INDEX)
	public String getSite() {
		return site;
	}

	@DynamoDbSecondaryPartitionKey(indexNames = SITEID_INDEX)
	public String getSiteId() {
		return siteId;
	}

	@DynamoDbSecondaryPartitionKey(indexNames = VIRTUAL_INDEX)
	public String getVirtualIndex() {
		return virtualIndex;
	}

	@DynamoDbSecondaryPartitionKey(indexNames = IS_SITE_INDEX)
	public String getIsSite() {
		return isSite;
	}

	public boolean isDeviceSite() {
		return "1".equalsIgnoreCase(getIsSite());
	}

	public void setVirtual(boolean isVirtual) {
		virtual = isVirtual;
		virtualIndex = isVirtual + "";
	}

	public String getDisplayName() {
		return Optional.ofNullable(getName()).orElse(getDeviceName());
	}
}
