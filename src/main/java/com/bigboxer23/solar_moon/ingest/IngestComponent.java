package com.bigboxer23.solar_moon.ingest;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.util.TokenGenerator;
import com.bigboxer23.solar_moon.web.TransactionUtil;
import java.util.*;
import org.opensearch.client.ResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.utils.StringUtils;

/** Class to read data from the generation meter web interface */
// @Component
public class IngestComponent implements MeterConstants {

	private static final Logger logger = LoggerFactory.getLogger(IngestComponent.class);

	public DeviceData handleDevice(Device device, DeviceData deviceData) throws ResponseException {
		if (deviceData == null) {
			logger.info("deviceData was not valid, not handling.");
			return null;
		}
		if (device == null) {
			logger.info("device was not valid, not handling.");
			return null;
		}
		IComponentRegistry.alarmComponent.resolveActiveAlarms(deviceData);
		Optional<Device> site =
				IComponentRegistry.deviceComponent.findDeviceById(device.getSiteId(), device.getClientId());
		if (device.isDeviceSite()) {
			deviceData.setIsSite();
		}
		calculateTotalEnergyConsumed(deviceData);
		IComponentRegistry.locationComponent.addLocationData(deviceData, site.orElse(null));
		IComponentRegistry.weatherComponent.addWeatherData(deviceData, site.orElse(null));
		IComponentRegistry.OSComponent.logData(
				deviceData.getDate() != null ? deviceData.getDate() : new Date(),
				Collections.singletonList(deviceData));

		IComponentRegistry.virtualDeviceComponent.handleVirtualDevice(deviceData);
		return deviceData;
	}

	/**
	 * Calculate the difference of power consumed since the last run. Add a new field with the
	 * difference
	 */
	private void calculateTotalEnergyConsumed(DeviceData deviceData) {
		if (deviceData.getDeviceId() == null) {
			logger.info("Can't calc total energy w/o device id");
			return;
		}
		logger.debug("calculating total energy consumed. " + deviceData.getDeviceId());
		float totalEnergyConsumption = deviceData.getTotalEnergyConsumed();
		if (totalEnergyConsumption < 0) {
			return;
		}
		Float previousTotalEnergyConsumed =
				IComponentRegistry.OSComponent.getTotalEnergyConsumed(deviceData.getDeviceId());
		if (previousTotalEnergyConsumed != null) {
			deviceData.setEnergyConsumed(totalEnergyConsumption - previousTotalEnergyConsumed);
		}
	}

	public Device findDeviceFromDeviceName(String customerId, String deviceName) {
		if (StringUtils.isEmpty(customerId) || StringUtils.isEmpty(deviceName)) {
			logger.warn("customer id or device name is null, can't find");
			return null;
		}
		logger.debug("finding device from device name/customer id " + deviceName + " " + customerId);
		Device device = IComponentRegistry.deviceComponent
				.findDeviceByDeviceName(customerId, deviceName)
				.orElseGet(() -> {
					logger.warn("New device found, " + deviceName);
					return IComponentRegistry.deviceComponent.addDevice(
							new Device(TokenGenerator.generateNewToken(), customerId, deviceName));
				});
		if (device != null) {
			TransactionUtil.addDeviceId(device.getId());
		}
		return device;
	}
}
