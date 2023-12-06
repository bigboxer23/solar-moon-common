package com.bigboxer23.solar_moon.device;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.DeviceUpdateData;
import com.bigboxer23.solar_moon.dynamodb.AbstractDynamodbComponent;
import java.util.stream.Collectors;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.utils.StringUtils;

/** */
public class DeviceUpdateComponent extends AbstractDynamodbComponent<DeviceUpdateData> {

	public void update(String deviceId) {
		update(deviceId, System.currentTimeMillis());
	}

	//Only for tests
	public void update(String deviceId, long time) {
		if (StringUtils.isBlank(deviceId)) {
			logger.warn("invalid device id, not updating");
			return;
		}
		getTable().updateItem(builder -> builder.item(new DeviceUpdateData(deviceId, time)));
	}

	public void delete(String deviceId) {
		getTable()
				.query(QueryConditional.keyEqualTo(builder -> builder.partitionValue(deviceId)))
				.items()
				.forEach(d -> getTable()
						.deleteItem(b -> b.key(
								Key.builder().partitionValue(d.getDeviceId()).build())));
	}

	public long queryByDeviceId(String deviceId) {
		return getTable()
				.query(QueryConditional.keyEqualTo(builder -> builder.partitionValue(deviceId)))
				.items()
				.stream()
				.map(DeviceUpdateData::getLastUpdate)
				.findAny()
				.orElse(-1L);
	}

	public void deleteByCustomerId(String customerId) {
		IComponentRegistry.deviceComponent.getDevicesForCustomerId(customerId).forEach(d -> delete(d.getId()));
	}

	public Iterable<DeviceUpdateData> getDevices() {
		return getTable().scan().items();
	}

	public Iterable<DeviceUpdateData> queryByTimeRange(long olderThan) {
		return getTable()
				.index(DeviceUpdateData.IDENTITY_UPDATE_INDEX)
				.query(QueryConditional.sortLessThan(builder ->
						builder.partitionValue(DeviceUpdateData.INDENTITY).sortValue(olderThan)))
				.stream()
				.flatMap(page -> page.items().stream())
				.collect(Collectors.toList());
	}

	@Override
	protected String getTableName() {
		return "device_updates";
	}

	@Override
	protected Class<DeviceUpdateData> getObjectClass() {
		return DeviceUpdateData.class;
	}
}
