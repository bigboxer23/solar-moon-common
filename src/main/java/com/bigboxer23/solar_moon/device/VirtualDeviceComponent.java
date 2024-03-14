package com.bigboxer23.solar_moon.device;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.dynamodb.DynamoLockUtils;
import com.bigboxer23.solar_moon.search.OpenSearchUtils;
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
		logger.info("Trying to aquire lock " + device.getDeviceId());
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
					List<DeviceData> siteDevices = IComponentRegistry.OSComponent.getDevicesForSiteByTimePeriod(
							device.getCustomerId(), device.getSiteId(), device.getDate());
					DeviceData virtualDeviceData = new DeviceData(
							virtualDevice.getSiteId(), virtualDevice.getClientId(), virtualDevice.getId());
					virtualDeviceData.setIsVirtual();
					if (virtualDevice.isDeviceSite()) {
						virtualDeviceData.setIsSite();
					}
					virtualDeviceData.setDate(device.getDate());
					float totalEnergyConsumed =
							getPushedDeviceValues(siteDevices, virtualDevice, DeviceData::getEnergyConsumed);
					if (totalEnergyConsumed > -1) {
						virtualDeviceData.setEnergyConsumed(
								Math.max(0, virtualDeviceData.getTotalEnergyConsumed()) + totalEnergyConsumed);
					}
					float totalRealPower =
							getPushedDeviceValues(siteDevices, virtualDevice, DeviceData::getTotalRealPower);
					if (totalRealPower > -1) {
						virtualDeviceData.setTotalRealPower(
								Math.max(0, virtualDeviceData.getTotalRealPower()) + totalRealPower);
					}
					IComponentRegistry.locationComponent.addLocationData(virtualDeviceData, virtualDevice);
					IComponentRegistry.weatherComponent.addWeatherData(virtualDeviceData, virtualDevice);
					logger.info("updating virtual device " + virtualDeviceData.getDeviceId() + " : " + device.getDate());
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
