package com.bigboxer23.solar_moon.device;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.alarm.SolectriaErrorOracle;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.data.LinkedDevice;
import com.bigboxer23.solar_moon.dynamodb.AbstractDynamodbComponent;
import com.bigboxer23.solar_moon.util.TimeConstants;
import java.util.Optional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.utils.StringUtils;

/** */
public class LinkedDeviceComponent extends AbstractDynamodbComponent<LinkedDevice> {
	public void update(LinkedDevice device) {
		if (device == null || StringUtils.isBlank(device.getId()) || StringUtils.isBlank(device.getCustomerId())) {
			logger.warn("invalid linked device, not updating");
			return;
		}
		getTable().updateItem(builder -> builder.item(device));
	}

	public void delete(String serialNumber, String customerId) {
		if (StringUtils.isBlank(serialNumber) || StringUtils.isBlank(customerId)) {
			logger.warn("invalid delete query");
			return;
		}
		getTable()
				.deleteItem(builder -> builder.key(
						builder2 -> builder2.partitionValue(serialNumber).sortValue(customerId)));
	}

	public void deleteByCustomerId(String customerId) {
		IComponentRegistry.deviceComponent.getDevicesForCustomerId(customerId).stream()
				.filter(d -> !StringUtils.isEmpty(d.getSerialNumber()))
				.forEach(d -> delete(d.getSerialNumber(), d.getClientId()));
	}

	public Optional<LinkedDevice> queryBySerialNumber(String serialNumber, String customerId) {
		if (StringUtils.isBlank(serialNumber) || StringUtils.isBlank(customerId)) {
			logger.warn("invalid query");
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

	public void addLinkedDeviceData(Device device, DeviceData deviceData) {
		if (device == null
				|| deviceData == null
				|| StringUtils.isBlank(device.getSerialNumber())
				|| StringUtils.isBlank(device.getClientId())) {
			return;
		}
		Optional<LinkedDevice> linkedDevice = queryBySerialNumber(device.getSerialNumber(), deviceData.getCustomerId());
		if (linkedDevice.isEmpty()) {
			return;
		}
		if (linkedDevice.get().getDate() < System.currentTimeMillis() - TimeConstants.HOUR) {
			return;
		}
		if (linkedDevice.get().getCriticalAlarm() > 0) {
			deviceData.setCriticalError(linkedDevice.get().getCriticalAlarm());
			deviceData.setCriticalErrorString(
					SolectriaErrorOracle.translateError(linkedDevice.get().getCriticalAlarm(), true));
		}
		if (linkedDevice.get().getInformativeAlarm() > 0) {
			deviceData.setInformationalError(linkedDevice.get().getInformativeAlarm());
			deviceData.setInformationalErrorString(
					SolectriaErrorOracle.translateError(linkedDevice.get().getInformativeAlarm(), false));
		}
	}
}
