package com.bigboxer23.solar_moon;

import com.bigboxer23.solar_moon.data.Device;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

/** */
// @Component
public class DeviceComponent extends AbstractDynamodbComponent<Device> {
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
		return getTable()
				.index(Device.VIRTUAL_INDEX)
				.query(QueryConditional.keyEqualTo(builder -> builder.partitionValue(isVirtual + "")))
				.stream()
				.flatMap(page -> page.items().stream())
				.collect(Collectors.toList());
	}

	public List<Device> getDevices(String customerId) {
		if (customerId == null || customerId.isEmpty()) {
			return Collections.emptyList();
		}
		return getTable()
				.index(Device.CLIENT_INDEX)
				.query(QueryConditional.keyEqualTo(builder -> builder.partitionValue(customerId)))
				.stream()
				.flatMap(page -> page.items().stream())
				.collect(Collectors.toList());
	}

	public Device getDevice(String id, String customerId) {
		if (id == null || customerId == null || id.isBlank() || customerId.isBlank()) {
			return null;
		}
		return getTable().getItem(new Device(id, customerId));
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
