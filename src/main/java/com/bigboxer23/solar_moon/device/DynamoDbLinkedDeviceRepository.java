package com.bigboxer23.solar_moon.device;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.LinkedDevice;
import com.bigboxer23.solar_moon.dynamodb.AuditableAbstractDynamodbRepository;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.utils.StringUtils;

@Slf4j
public class DynamoDbLinkedDeviceRepository extends AuditableAbstractDynamodbRepository<LinkedDevice>
		implements LinkedDeviceRepository {

	@Override
	public void delete(String serialNumber, String customerId) {
		if (StringUtils.isBlank(serialNumber) || StringUtils.isBlank(customerId)) {
			log.warn("invalid delete query");
			return;
		}
		getTable()
				.deleteItem(builder -> builder.key(
						builder2 -> builder2.partitionValue(serialNumber).sortValue(customerId)));
	}

	@Override
	public void deleteByCustomerId(String customerId) {
		getDeviceComponent().getDevicesForCustomerId(customerId).stream()
				.filter(d -> !StringUtils.isEmpty(d.getSerialNumber()))
				.forEach(d -> delete(d.getSerialNumber(), d.getClientId()));
	}

	@Override
	public Optional<LinkedDevice> findBySerialNumber(String serialNumber, String customerId) {
		if (StringUtils.isBlank(serialNumber) || StringUtils.isBlank(customerId)) {
			log.warn("invalid query");
			return Optional.empty();
		}
		return getTable()
				.query(QueryConditional.keyEqualTo(
						builder -> builder.partitionValue(serialNumber).sortValue(customerId)))
				.items()
				.stream()
				.findAny();
	}

	@Override
	protected String getTableName() {
		return "linked_devices";
	}

	@Override
	protected Class<LinkedDevice> getObjectClass() {
		return LinkedDevice.class;
	}

	protected DeviceComponent getDeviceComponent() {
		return IComponentRegistry.deviceComponent;
	}
}
