package com.bigboxer23.solar_moon.device;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.dynamodb.AbstractDynamodbComponent;
import com.bigboxer23.solar_moon.subscription.SubscriptionComponent;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.bigboxer23.solar_moon.web.TransactionUtil;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.utils.StringUtils;

/** */
public class DeviceComponent extends AbstractDynamodbComponent<Device> {

	private final SubscriptionComponent subscriptionComponent;

	public DeviceComponent(SubscriptionComponent subscriptionComponent) {
		this.subscriptionComponent = subscriptionComponent;
	}

	public static final String NO_SITE = "No Site";

	public Optional<Device> findDeviceByDeviceName(String customerId, String deviceName) {
		if (StringUtils.isBlank(deviceName) || StringUtils.isBlank(customerId)) {
			return Optional.empty();
		}
		return getTable()
				.index(Device.DEVICE_NAME_INDEX)
				.query(QueryConditional.keyEqualTo(
						builder -> builder.partitionValue(deviceName).sortValue(customerId)))
				.stream()
				.findFirst()
				.flatMap(page -> page.items().stream().findFirst());
	}

	public Optional<Device> findDeviceByName(String customerId, String name) {
		if (StringUtils.isBlank(name) || StringUtils.isBlank(customerId)) {
			return Optional.empty();
		}
		return getTable()
				.index(Device.NAME_INDEX)
				.query(QueryConditional.keyEqualTo(
						builder -> builder.partitionValue(name).sortValue(customerId)))
				.stream()
				.findFirst()
				.flatMap(page -> page.items().stream().findFirst());
	}

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

	public List<Device> getDevicesForCustomerId(String customerId) {
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

	public Optional<Device> findDeviceById(String id) {
		if (StringUtils.isBlank(id)) {
			return Optional.empty();
		}
		return getTable()
				.query(QueryConditional.keyEqualTo(
						Key.builder().partitionValue(id).build()))
				.items()
				.stream()
				.findFirst();
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

	private void maybeUpdateLocationData(Device device) {
		if (device.isVirtual()
				&& device.getLongitude() == -1
				&& device.getLatitude() == -1
				&& !StringUtils.isBlank(device.getCity())
				&& !StringUtils.isBlank(device.getState())
				&& !StringUtils.isBlank(device.getCountry())) {
			IComponentRegistry.locationComponent
					.getLatLongFromText(device.getCity(), device.getState(), device.getCountry())
					.ifPresent(location -> {
						List<Double> points = location.place().geometry().point();
						device.setLatitude(points.get(1));
						device.setLongitude(points.get(0));
					});
		}
	}

	public boolean addDevice(Device device) {
		if (subscriptionComponent.getSubscriptionPacks(device.getClientId()) * 20
				<= getDevicesForCustomerId(device.getClientId()).size()) {
			logger.warn("Cannot add new device, not enough devices in license: "
					+ (subscriptionComponent.getSubscriptionPacks(device.getClientId()) * 20)
					+ ":"
					+ getDevicesForCustomerId(device.getClientId()).size());
			return false;
		}
		if (getDevice(device.getId(), device.getClientId()) != null) {
			logger.warn(device.getClientId() + ":" + device.getId() + " exists, not putting into db.");
			return false;
		}
		if (findDeviceByDeviceName(device.getClientId(), device.getDeviceName()).isPresent()) {
			logger.warn(device.getClientId() + ":" + device.getDeviceName() + " exists, cannot add a matching device");
			return false;
		}
		maybeUpdateLocationData(device);
		logAction("add", device.getId());
		getTable().putItem(device);
		return true;
	}

	public boolean isValidUpdate(Device device) {
		return !StringUtils.isBlank(device.getClientId())
				&& !StringUtils.isBlank(device.getDeviceName())
				&& !StringUtils.isBlank(device.getId());
	}

	public Optional<Device> updateDevice(Device device) {
		logAction("update", device.getId());
		Optional<Device> dbDevice = findDeviceByDeviceName(device.getClientId(), device.getDeviceName());
		if (dbDevice.isPresent() && !dbDevice.get().getId().equals(device.getId())) {
			logger.warn(device.getClientId() + ":" + device.getDeviceName() + " exists, cannot update matching device");
			return Optional.empty();
		}
		if (device.isVirtual()) {
			Device site = getDevice(device.getId(), device.getClientId());
			if (!site.getName().equals(device.getName())) {
				getDevicesBySite(site.getClientId(), site.getName()).forEach(childDevice -> {
					childDevice.setSite(device.getName());
					updateDevice(childDevice);
				});
			}
		}
		maybeUpdateLocationData(device);
		return Optional.ofNullable(getTable().updateItem(builder -> builder.item(device)));
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
		IComponentRegistry.deviceUpdateComponent.delete(device.getId());
		IComponentRegistry.alarmComponent.deleteAlarmByDeviceId(device.getClientId(), device.getId());
	}

	public void deleteDevicesByCustomerId(String customerId) {
		getDevicesForCustomerId(customerId).forEach(device -> deleteDevice(device.getId(), customerId));
	}

	public void logAction(String action, String id) {
		TransactionUtil.addDeviceId(id);
		logger.info("device " + action);
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
