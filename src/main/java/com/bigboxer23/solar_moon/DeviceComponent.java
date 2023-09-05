package com.bigboxer23.solar_moon;

import com.bigboxer23.solar_moon.data.Device;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

/** */
@Component
public class DeviceComponent {
	protected static final Logger logger = LoggerFactory.getLogger(DeviceComponent.class);

	private DynamoDbEnhancedClient client;

	private DynamoDbEnhancedClient getClient() {
		if (client == null) {
			client = DynamoDbEnhancedClient.create();
		}
		return client;
	}

	protected DynamoDbTable<Device> getDeviceTable() {
		return getClient().table(Device.TABLE_NAME, TableSchema.fromBean(Device.class));
	}

	public Device findDeviceByDeviceKey(String deviceKey) {
		return getDeviceTable()
				.index(Device.DEVICE_KEY_INDEX)
				.query(QueryConditional.keyEqualTo(builder -> builder.partitionValue(deviceKey)))
				.stream()
				.findFirst()
				.flatMap(page -> page.items().stream().findFirst())
				.orElse(null);
	}

	public Stream<Page<Device>> getDevices(String clientId) {
		return getDeviceTable()
				.index(Device.CLIENT_INDEX)
				.query(QueryConditional.keyEqualTo(builder -> builder.partitionValue(clientId)))
				.stream();
	}
}
