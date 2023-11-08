package com.bigboxer23.solar_moon;

import com.bigboxer23.solar_moon.data.Device;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.utils.StringUtils;

/** */
// @Component
public class DeviceComponent extends AbstractDynamodbComponent<Device> {

	private SubscriptionComponent subscriptionComponent;

	public DeviceComponent(SubscriptionComponent subscriptionComponent) {
		this.subscriptionComponent = subscriptionComponent;
	}

	public static final String NO_SITE = "No Site";

	public Device findDeviceByDeviceKey(String deviceKey) {
		if (deviceKey == null || deviceKey.isEmpty()) {
			return null;
		}
		return getTable()
				.index(Device.DEVICE_KEY_INDEX)
				.query(QueryConditional.keyEqualTo(builder -> builder.partitionValue(deviceKey)))
				.stream()
				.findFirst()
				.flatMap(page -> page.items().stream().findFirst())
				.orElse(null);
	}

	public List<Device> getDevicesBySite(String customerId, String site) {
		if (customerId == null || site == null || site.isBlank() || customerId.isBlank()) {
			return Collections.emptyList();
		}
		return getTable()
				.index(Device.SITE_INDEX)
				.query(QueryConditional.keyEqualTo(
						builder -> builder.partitionValue(site).sortValue(customerId)))
				.stream()
				.flatMap(page -> page.items().stream())
				.collect(Collectors.toList());
	}

	public List<Device> getDevices(boolean isVirtual) {
		logger.info("Fetching " + (isVirtual ? "" : "non-") + "virtual devices");
		return getTable()
				.index(Device.VIRTUAL_INDEX)
				.query(QueryConditional.keyEqualTo(builder -> builder.partitionValue(isVirtual + "")))
				.stream()
				.flatMap(page -> page.items().stream())
				.collect(Collectors.toList());
	}

	public List<Device> getDevices(String customerId) {
		if (StringUtils.isBlank(customerId)) {
			return Collections.emptyList();
		}
		logger.debug("Fetching all devices");
		return getTable()
				.index(Device.CLIENT_INDEX)
				.query(QueryConditional.keyEqualTo(builder -> builder.partitionValue(customerId)))
				.stream()
				.flatMap(page -> page.items().stream())
				.collect(Collectors.toList());
	}

	public Device getDevice(String id, String customerId) {
		if (StringUtils.isBlank(id) || StringUtils.isBlank(customerId)) {
			return null;
		}
		logAction("get", id);
		return getTable().getItem(new Device(id, customerId));
	}

	public boolean isValidAdd(Device device) {
		return !StringUtils.isBlank(device.getClientId())
				&& !StringUtils.isBlank(device.getDeviceName())
				&& !StringUtils.isBlank(device.getId());
	}

	public boolean addDevice(Device device) {
		if (subscriptionComponent.getSubscriptionPacks(device.getClientId()) * 10
				<= getDevices(device.getClientId()).size()) {
			logger.warn("Cannot add new device, not enough devices in license: "
					+ (subscriptionComponent.getSubscriptionPacks(device.getClientId()) * 10)
					+ ":"
					+ getDevices(device.getClientId()).size());
			return false;
		}
		if (getDevice(device.getId(), device.getClientId()) != null) {
			logger.warn(device.getClientId() + ":" + device.getId() + " exists, not putting into db.");
			return false;
		}
		logAction("add", device.getId());
		getTable().putItem(device);
		return true;
	}

	public boolean isValidUpdate(Device device) {
		return !StringUtils.isBlank(device.getClientId())
				&& !StringUtils.isBlank(device.getDeviceName())
				&& !StringUtils.isBlank(device.getId());
	}

	public void updateDevice(Device device) {
		logAction("update", device.getId());
		if (device.isVirtual()) {
			Device site = getDevice(device.getId(), device.getClientId());
			if (!site.getName().equals(device.getName())) {
				getDevicesBySite(site.getClientId(), site.getName()).forEach(childDevice -> {
					childDevice.setSite(device.getName());
					updateDevice(childDevice);
				});
			}
		}
		getTable().updateItem(builder -> builder.item(device));
	}

	public void deleteDevice(String id, String customerId) {
		logAction("delete", id);
		Device device = getDevice(id, customerId);
		if (device.isVirtual()) {
			getDevicesBySite(customerId, device.getName()).forEach(childDevice -> {
				childDevice.setSite(NO_SITE);
				updateDevice(childDevice);
			});
		}
		getTable().deleteItem(device);
	}

	public void deleteDevicesByCustomerId(String customerId) {
		getDevices(customerId).forEach(device -> deleteDevice(device.getId(), customerId));
	}

	public void logAction(String action, String id) {
		logger.info(id + " device " + action);
	}

	@Override
	protected String getTableName() {
		return "devices";
	}

	@Override
	protected Class<Device> getObjectClass() {
		return Device.class;
	}
}
