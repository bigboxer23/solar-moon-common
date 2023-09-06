package com.bigboxer23.solar_moon;

import com.bigboxer23.solar_moon.data.Device;
import java.util.Collections;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
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

	public Stream<Page<Device>> getDevices(String clientId) {
		if (clientId == null || clientId.isEmpty()) {
			return Stream.of(Page.create(Collections.emptyList()));
		}
		return getTable()
				.index(Device.CLIENT_INDEX)
				.query(QueryConditional.keyEqualTo(builder -> builder.partitionValue(clientId)))
				.stream();
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
