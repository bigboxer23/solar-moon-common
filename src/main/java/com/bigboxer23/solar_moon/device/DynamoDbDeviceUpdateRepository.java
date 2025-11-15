package com.bigboxer23.solar_moon.device;

import com.bigboxer23.solar_moon.data.DeviceUpdateData;
import com.bigboxer23.solar_moon.dynamodb.AbstractDynamodbComponent;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

@Slf4j
public class DynamoDbDeviceUpdateRepository extends AbstractDynamodbComponent<DeviceUpdateData>
		implements DeviceUpdateRepository {

	@Override
	public DeviceUpdateData update(DeviceUpdateData deviceUpdate) {
		getTable().updateItem(builder -> builder.item(deviceUpdate));
		return deviceUpdate;
	}

	@Override
	public void delete(String deviceId) {
		getTable()
				.query(QueryConditional.keyEqualTo(builder -> builder.partitionValue(deviceId)))
				.items()
				.forEach(d -> getTable()
						.deleteItem(b -> b.key(
								Key.builder().partitionValue(d.getDeviceId()).build())));
	}

	@Override
	public Optional<Long> findLastUpdateByDeviceId(String deviceId) {
		return getTable()
				.query(QueryConditional.keyEqualTo(builder -> builder.partitionValue(deviceId)))
				.items()
				.stream()
				.map(DeviceUpdateData::getLastUpdate)
				.findAny();
	}

	@Override
	public Iterable<DeviceUpdateData> findAll() {
		return getTable().scan().items();
	}

	@Override
	public Iterable<DeviceUpdateData> findByTimeRangeLessThan(long olderThan) {
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
