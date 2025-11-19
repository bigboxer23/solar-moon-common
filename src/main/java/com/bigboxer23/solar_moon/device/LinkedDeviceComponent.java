package com.bigboxer23.solar_moon.device;

import com.bigboxer23.solar_moon.alarm.ISolectriaConstants;
import com.bigboxer23.solar_moon.alarm.SolectriaErrorOracle;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.data.LinkedDevice;
import com.bigboxer23.solar_moon.util.TimeConstants;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.utils.StringUtils;

/** */
@Slf4j
public class LinkedDeviceComponent {

	private LinkedDeviceRepository repository;

	protected LinkedDeviceRepository getRepository() {
		if (repository == null) {
			repository = new DynamoDbLinkedDeviceRepository();
		}
		return repository;
	}

	public void update(LinkedDevice device) {
		if (device == null || StringUtils.isBlank(device.getId()) || StringUtils.isBlank(device.getCustomerId())) {
			log.warn("invalid linked device, not updating");
			return;
		}
		getRepository().update(device);
	}

	public void delete(String serialNumber, String customerId) {
		getRepository().delete(serialNumber, customerId);
	}

	public void deleteByCustomerId(String customerId) {
		getRepository().deleteByCustomerId(customerId);
	}

	public Optional<LinkedDevice> queryBySerialNumber(String serialNumber, String customerId) {
		return getRepository().findBySerialNumber(serialNumber, customerId);
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
