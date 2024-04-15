package com.bigboxer23.solar_moon.device;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.dynamodb.DynamoLockUtils;
import com.bigboxer23.solar_moon.search.OpenSearchUtils;
import com.bigboxer23.solar_moon.web.TransactionUtil;
import java.util.*;
import java.util.function.Function;
import org.opensearch.client.ResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Component to stash all the logic related to aggregating virtual site devices */
public class VirtualDeviceComponent {
	private static final Logger logger = LoggerFactory.getLogger(VirtualDeviceComponent.class);

	public void handleVirtualDevice(DeviceData device) {
		if (!shouldAddVirtualDevice(device)) {
			return;
		}
		logger.info("Trying to aquire lock");
		DynamoLockUtils.doLockedCommand(
				device.getSiteId() + "-" + device.getDate().getTime(), device.getDeviceId(), () -> {
					Device virtualDevice =
							IComponentRegistry.deviceComponent
									.getDevicesBySiteId(device.getCustomerId(), device.getSiteId())
									.stream()
									.filter(Device::isVirtual)
									.findAny()
									.orElse(null);
					if (virtualDevice == null) {
						logger.warn("cannot find virtualDevice " + device.getCustomerId() + ":" + device.getSiteId());
						return;
					}
					TransactionUtil.addDeviceId(virtualDevice.getId());
					List<DeviceData> siteDevices = IComponentRegistry.OSComponent.getDevicesForSiteByTimePeriod(
							device.getCustomerId(), device.getSiteId(), device.getDate());
					DeviceData virtualDeviceData = new DeviceData(
							virtualDevice.getSiteId(), virtualDevice.getClientId(), virtualDevice.getId());
					virtualDeviceData.setIsVirtual();
					if (virtualDevice.isDeviceSite()) {
						virtualDeviceData.setIsSite();
					}
					virtualDeviceData.setDate(device.getDate());
					virtualDeviceData.setEnergyConsumed(Math.max(
							0, getPushedDeviceValues(siteDevices, virtualDevice, DeviceData::getEnergyConsumed)));
					virtualDeviceData.setTotalRealPower(Math.max(
							0, getPushedDeviceValues(siteDevices, virtualDevice, DeviceData::getTotalRealPower)));
					virtualDeviceData.setTotalEnergyConsumed(Math.max(
							0, getPushedDeviceValues(siteDevices, virtualDevice, DeviceData::getTotalEnergyConsumed)));
					IComponentRegistry.locationComponent.addLocationData(virtualDeviceData, virtualDevice);
					IComponentRegistry.weatherComponent.addWeatherData(virtualDeviceData, virtualDevice);
					logger.info("updating virtual device: " + device.getDate());
					try {
						IComponentRegistry.OSComponent.logData(
								virtualDeviceData.getDate(), Collections.singletonList(virtualDeviceData));
						OpenSearchUtils.waitForIndexing();
					} catch (ResponseException e) {
						logger.error("handleVirtualDevice", e);
					}
				});
	}

	private boolean shouldAddVirtualDevice(DeviceData device) {
		if (DeviceComponent.NO_SITE.equals(device.getSiteId())) {
			return false;
		}
		List<Device> devices =
				IComponentRegistry.deviceComponent.getDevicesBySiteId(device.getCustomerId(), device.getSiteId());
		if (devices.stream().noneMatch(Device::isVirtual)) {
			return false;
		}
		OpenSearchUtils.waitForIndexing();
		int openSearchDeviceCount = IComponentRegistry.OSComponent.getSiteDevicesCountByTimePeriod(
				device.getCustomerId(), device.getSiteId(), device.getDate());
		if (devices.size() - 1 != openSearchDeviceCount) {
			logger.debug("not calculating site "
					+ device.getSiteId()
					+ ". Only "
					+ openSearchDeviceCount
					+ " devices have written data out of "
					+ devices.size()
					+ ".");
			return false;
		}
		return true;
	}

	/**
	 * Query OpenSearch for the most recent data (within the ${scheduler-time} window) because this
	 * data is pushed to us from the devices
	 *
	 * @param servers
	 * @param site
	 * @param mapper
	 * @return
	 */
	private float getPushedDeviceValues(List<DeviceData> devices, Device site, Function<DeviceData, Float> mapper) {
		return devices.stream()
				.map(mapper)
				.filter(energy -> energy >= 0)
				.reduce((val1, val2) -> site.isSubtraction() ? Math.abs(val1 - val2) : val1 + val2)
				.orElse(-1f);
	}
}
