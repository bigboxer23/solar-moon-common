package com.bigboxer23.solar_moon.device;

import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.dynamodb.AuditableAbstractDynamodbRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.utils.StringUtils;

/**
 * DynamoDB implementation of DeviceRepository. Handles all data persistence operations for Device
 * entities.
 */
@Slf4j
public class DynamoDbDeviceRepository extends AuditableAbstractDynamodbRepository<Device> implements DeviceRepository {

	@Override
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

	@Override
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

	@Override
	public Optional<Device> findDeviceByDeviceKey(String deviceKey) {
		if (StringUtils.isBlank(deviceKey)) {
			return Optional.empty();
		}
		return getTable()
				.index(Device.DEVICE_KEY_INDEX)
				.query(QueryConditional.keyEqualTo(builder -> builder.partitionValue(deviceKey)))
				.stream()
				.findFirst()
				.flatMap(page -> page.items().stream().findFirst());
	}

	@Override
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

	@Override
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

	@Override
	public List<Device> getDevices(boolean isVirtual) {
		log.info("Fetching " + (isVirtual ? "" : "non-") + "virtual devices");
		return getTable()
				.index(Device.VIRTUAL_INDEX)
				.query(QueryConditional.keyEqualTo(builder -> builder.partitionValue(isVirtual + "")))
				.stream()
				.flatMap(page -> page.items().stream())
				.collect(Collectors.toList());
	}

	@Override
	public List<Device> getSites() {
		log.info("Fetching site devices");
		return getTable()
				.index(Device.IS_SITE_INDEX)
				.query(QueryConditional.keyEqualTo(builder -> builder.partitionValue("1")))
				.stream()
				.flatMap(page -> page.items().stream())
				.collect(Collectors.toList());
	}

	@Override
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

	@Override
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

	@Override
	public Optional<Device> findDeviceById(String id, String customerId) {
		if (StringUtils.isBlank(id) || StringUtils.isBlank(customerId)) {
			return Optional.empty();
		}
		return Optional.ofNullable(getTable().getItem(new Device(id, customerId)));
	}

	@Override
	public void delete(Device device) {
		if (device != null) {
			getTable().deleteItem(device);
		}
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
