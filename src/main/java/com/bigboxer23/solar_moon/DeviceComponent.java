package com.bigboxer23.solar_moon;

import com.bigboxer23.solar_moon.data.Device;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

/** */
@Component
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

	public List<Device> getDevices(String clientId) {
		if (clientId == null || clientId.isEmpty()) {
			return Collections.emptyList();
		}
		return getTable()
				.index(Device.CLIENT_INDEX)
				.query(QueryConditional.keyEqualTo(builder -> builder.partitionValue(clientId)))
				.stream()
				.flatMap(page -> page.items().stream())
				.collect(Collectors.toList());
	}

	public Device getDevice(String id, String clientId) {
		if (id == null || clientId == null || id.isBlank() || clientId.isBlank()) {
			return null;
		}
		return getTable().getItem(new Device(id, clientId));
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
