package com.bigboxer23.solar_moon.device;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.Subscription;
import com.bigboxer23.solar_moon.dynamodb.AuditableAbstractDynamodbComponent;
import com.bigboxer23.solar_moon.subscription.SubscriptionComponent;
import com.bigboxer23.solar_moon.web.TransactionUtil;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.utils.StringUtils;

/** */
@Slf4j
public class DeviceComponent extends AuditableAbstractDynamodbComponent<Device>
{

	public static final String NO_SITE = "No Site"; // TODO:set as site id instead of name (or also as name)

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

	public List<Device> getDevicesBySiteId(String customerId, String siteId) {
		if (customerId == null || siteId == null || siteId.isBlank() || customerId.isBlank()) {
			return Collections.emptyList();
		}
		return getTable()
				.index(Device.SITEID_INDEX)
				.query(QueryConditional.keyEqualTo(
						builder -> builder.partitionValue(siteId).sortValue(customerId)))
				.stream()
				.flatMap(page -> page.items().stream())
				.collect(Collectors.toList());
	}

	@Deprecated
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
		log.info("Fetching " + (isVirtual ? "" : "non-") + "virtual devices");
		return getTable()
				.index(Device.VIRTUAL_INDEX)
				.query(QueryConditional.keyEqualTo(builder -> builder.partitionValue(isVirtual + "")))
				.stream()
				.flatMap(page -> page.items().stream())
				.collect(Collectors.toList());
	}

	public List<Device> getSites() {
		log.info("Fetching site devices");
		return getTable()
				.index(Device.IS_SITE_INDEX)
				.query(QueryConditional.keyEqualTo(builder -> builder.partitionValue("1")))
				.stream()
				.flatMap(page -> page.items().stream())
				.collect(Collectors.toList());
	}

	public List<Device> getDevicesForCustomerId(String customerId) {
		if (StringUtils.isBlank(customerId)) {
			return Collections.emptyList();
		}
		log.debug("Fetching all devices");
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

	public Optional<Device> findDeviceById(String id, String customerId) {
		if (StringUtils.isBlank(id) || StringUtils.isBlank(customerId)) {
			return Optional.empty();
		}
		return Optional.ofNullable(getTable().getItem(new Device(id, customerId)));
	}

	public boolean isValidAdd(Device device) {
		return !StringUtils.isBlank(device.getClientId())
				&& !StringUtils.isBlank(device.getDeviceName())
				&& !StringUtils.isBlank(device.getId());
	}

	private void maybeUpdateLocationData(Device device) {
		// If site, maybe fetch new location, update children
		if (device.isDeviceSite()
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
						getDevicesBySiteId(device.getClientId(), device.getSiteId()).stream()
								.filter(d -> !d.isDeviceSite())
								.forEach(d -> {
									d.setLatitude(device.getLatitude());
									d.setLongitude(device.getLongitude());
									updateDevice(d);
								});
					});
		}
		// If device, remove or set lat/long based on site status
		if (!device.isDeviceSite()) {
			Optional<Device> dbDevice = findDeviceById(device.getId(), device.getClientId());
			if (device.getSiteId() == null || NO_SITE.equalsIgnoreCase(device.getSiteId())) {
				device.setLatitude(-1);
				device.setLongitude(-1);
			} else if ((dbDevice.isEmpty() && device.getSiteId() != null)
					|| (dbDevice.isPresent()
							&& !device.getSiteId()
									.equalsIgnoreCase(dbDevice.get().getSiteId()))) {
				findDeviceById(device.getSiteId(), device.getClientId()).ifPresent(site -> {
					device.setLatitude(site.getLatitude());
					device.setLongitude(site.getLongitude());
				});
			}
		}
	}

	public Device addDevice(Device device) {
		if (!IComponentRegistry.subscriptionComponent.canAddAnotherDevice(device.getClientId())) {
			log.warn("Cannot add new device, not enough devices in license: "
					+ (IComponentRegistry.subscriptionComponent
									.getSubscription(device.getClientId())
									.map(Subscription::getPacks)
									.orElse(0)
							* SubscriptionComponent.DEVICES_PER_SUBSCRIPTION)
					+ ":"
					+ getDevicesForCustomerId(device.getClientId()).size());
			return null;
		}
		if (findDeviceById(device.getId(), device.getClientId()).isPresent()) {
			log.warn(device.getClientId() + ":" + device.getId() + " exists, not putting into db.");
			return null;
		}
		if (findDeviceByDeviceName(device.getClientId(), device.getDeviceName()).isPresent()) {
			log.warn(device.getClientId() + ":" + device.getDeviceName() + " exists, cannot add a matching device");
			return null;
		}
		maybeUpdateLocationData(device);
		if (device.getSite().equalsIgnoreCase(device.getDeviceName()) && device.getSiteId() == null) {
			device.setSiteId(device.getId());
		}
		logAction("add", device.getId(), device.getSiteId());
		return add(device);
	}

	public boolean isValidUpdate(Device device) {
		return !StringUtils.isBlank(device.getClientId())
				&& !StringUtils.isBlank(device.getDeviceName())
				&& !StringUtils.isBlank(device.getId());
	}

	public Optional<Device> updateDevice(Device device) {
		logAction("update", device.getId(), device.getSiteId());
		Optional<Device> dbDevice = findDeviceByDeviceName(device.getClientId(), device.getDeviceName());
		if (dbDevice.isPresent() && !dbDevice.get().getId().equals(device.getId())) {
			log.warn(device.getClientId() + ":" + device.getDeviceName() + " exists, cannot update matching device");
			return Optional.empty();
		}
		if (device.isDeviceSite()) {
			Device site = findDeviceById(device.getId(), device.getClientId()).get();
			if (!site.getName().equals(device.getName())) {
				getDevicesBySiteId(site.getClientId(), site.getSiteId()).forEach(childDevice -> {
					childDevice.setSite(device.getName());
					updateDevice(childDevice);
				});
			}
		}
		// TODO: could more efficiently update child devices?
		maybeUpdateLocationData(device);
		return update(device);
	}

	public void deleteDevice(String id, String customerId) {
		Optional<Device> device = findDeviceById(id, customerId);
		if (device.isEmpty()) {
			return;
		}
		logAction("delete " + device.get().getDisplayName(), id, device.get().getSiteId());
		if (device.get().isDeviceSite()) {
			getDevicesBySiteId(customerId, device.get().getId()).forEach(childDevice -> {
				childDevice.setSite(NO_SITE);
				childDevice.setSiteId(NO_SITE);
				updateDevice(childDevice);
			});
		}
		getTable().deleteItem(device.get());
		IComponentRegistry.deviceUpdateComponent.delete(device.get().getId());
		IComponentRegistry.alarmComponent.deleteAlarmByDeviceId(
				device.get().getClientId(), device.get().getId());
	}

	public void deleteDevicesByCustomerId(String customerId) {
		getDevicesForCustomerId(customerId).forEach(device -> deleteDevice(device.getId(), customerId));
	}

	public void logAction(String action, String id, String siteId) {
		TransactionUtil.addDeviceId(id, siteId);
		log.info("device " + action);
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
