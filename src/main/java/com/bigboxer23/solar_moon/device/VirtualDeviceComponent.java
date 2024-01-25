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
		logger.info("Trying to aquire lock " + device.getName());
		DynamoLockUtils.doLockedCommand(
				device.getSite() + "-" + device.getDate().getTime(), device.getName(), () -> {
					Device site =
							IComponentRegistry.deviceComponent
									.getDevicesBySite(device.getCustomerId(), device.getSite())
									.stream()
									.filter(Device::isVirtual)
									.findAny()
									.orElse(null);
					/*Device site = deviceComponent
					.findDeviceByName(device.getClientId(), device.getSite())
					.orElse(null)*/
					// TODO:replace with this, should be more efficient than full scan
					if (site == null) {
						logger.warn("cannot find site " + device.getCustomerId() + ":" + device.getSite());
						return;
					}
					List<DeviceData> siteDevices = IComponentRegistry.OSComponent.getDevicesForSiteByTimePeriod(
							device.getCustomerId(), device.getSite(), device.getDate());
					DeviceData virtualDevice =
							new DeviceData(site.getSite(), site.getName(), site.getClientId(), site.getId());
					virtualDevice.setIsVirtual();
					if (site.isDeviceSite()) {
						virtualDevice.setIsSite();
					}
					virtualDevice.setDate(device.getDate());
					float totalEnergyConsumed = getPushedDeviceValues(siteDevices, site, DeviceData::getEnergyConsumed);
					if (totalEnergyConsumed > -1) {
						virtualDevice.setEnergyConsumed(
								Math.max(0, virtualDevice.getTotalEnergyConsumed()) + totalEnergyConsumed);
					}
					float totalRealPower = getPushedDeviceValues(siteDevices, site, DeviceData::getTotalRealPower);
					if (totalRealPower > -1) {
						virtualDevice.setTotalRealPower(
								Math.max(0, virtualDevice.getTotalRealPower()) + totalRealPower);
					}
					IComponentRegistry.locationComponent.addLocationData(virtualDevice, site);
					IComponentRegistry.weatherComponent.addWeatherData(virtualDevice, site);
					logger.info("adding virtual device " + device.getSite() + " : " + device.getDate());
					try {
						IComponentRegistry.OSComponent.logData(
								virtualDevice.getDate(), Collections.singletonList(virtualDevice));
						OpenSearchUtils.waitForIndexing();
					} catch (ResponseException e) {
						logger.error("handleVirtualDevice", e);
					}
				});
	}

	private boolean shouldAddVirtualDevice(DeviceData device) {
		if (DeviceComponent.NO_SITE.equals(device.getSite())) {
			return false;
		}
		OpenSearchUtils.waitForIndexing();
		int deviceCount = IComponentRegistry.deviceComponent
				.getDevicesBySite(device.getCustomerId(), device.getSite())
				.size();
		int openSearchDeviceCount = IComponentRegistry.OSComponent.getSiteDevicesCountByTimePeriod(
				device.getCustomerId(), device.getSite(), device.getDate());
		if (deviceCount - 1 != openSearchDeviceCount) {
			logger.debug("not calculating site "
					+ device.getSite()
					+ ". Only "
					+ openSearchDeviceCount
					+ " devices have written data out of "
					+ deviceCount
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
