package com.bigboxer23.solar_moon.device;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.alarm.ISolectriaConstants;
import com.bigboxer23.solar_moon.alarm.SolectriaErrorOracle;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.data.LinkedDevice;
import com.bigboxer23.solar_moon.dynamodb.AbstractDynamodbComponent;
import com.bigboxer23.solar_moon.util.TimeConstants;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.utils.StringUtils;

/** */
@Slf4j
public class LinkedDeviceComponent extends AbstractDynamodbComponent<LinkedDevice> {
	public void update(LinkedDevice device) {
		if (device == null || StringUtils.isBlank(device.getId()) || StringUtils.isBlank(device.getCustomerId())) {
			log.warn("invalid linked device, not updating");
			return;
		}
		getTable().updateItem(builder -> builder.item(device));
	}

	public void delete(String serialNumber, String customerId) {
		if (StringUtils.isBlank(serialNumber) || StringUtils.isBlank(customerId)) {
			log.warn("invalid delete query");
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

	public DeviceData addLinkedDeviceDataVirtual(DeviceData virtualDevice, List<DeviceData> childDevices) {
		if (virtualDevice == null || childDevices == null || childDevices.isEmpty()) {
			log.warn("invalid virtual device or child devices");
			return virtualDevice;
		}
		childDevices.forEach(d -> {
			if (d.getInformationalError() > -1) {
				if (virtualDevice.getInformationalError() == -1) {
					virtualDevice.setInformationalError(ISolectriaConstants.NOMINAL);
				}
				virtualDevice.setInformationalError(virtualDevice.getInformationalError() | d.getInformationalError());
			}
			if (d.getCriticalError() > -1) {
				if (virtualDevice.getCriticalError() == -1) {
					virtualDevice.setCriticalError(ISolectriaConstants.NOMINAL);
				}
				virtualDevice.setCriticalError(virtualDevice.getCriticalError() | d.getCriticalError());
			}
		});
		if (virtualDevice.getInformationalError() > -1) {
			virtualDevice.setInformationalErrorString(
					SolectriaErrorOracle.translateError(virtualDevice.getInformationalError(), false));
		}
		if (virtualDevice.getCriticalError() > -1) {
			virtualDevice.setCriticalErrorString(
					SolectriaErrorOracle.translateError(virtualDevice.getCriticalError(), true));
		}
		return virtualDevice;
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
