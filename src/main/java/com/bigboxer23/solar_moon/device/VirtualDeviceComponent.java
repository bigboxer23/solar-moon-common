package com.bigboxer23.solar_moon.device;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.dynamodb.DynamoLockUtils;
import com.bigboxer23.solar_moon.location.LocationComponent;
import com.bigboxer23.solar_moon.search.OpenSearchComponent;
import com.bigboxer23.solar_moon.search.OpenSearchUtils;
import com.bigboxer23.solar_moon.weather.PirateWeatherComponent;
import com.bigboxer23.solar_moon.web.TransactionUtil;
import java.util.*;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.ResponseException;

/** Component to stash all the logic related to aggregating virtual site devices */
@Slf4j
public class VirtualDeviceComponent {
	protected DeviceComponent getDeviceComponent() {
		return IComponentRegistry.deviceComponent;
	}

	protected OpenSearchComponent getOSComponent() {
		return IComponentRegistry.OSComponent;
	}

	protected LocationComponent getLocationComponent() {
		return IComponentRegistry.locationComponent;
	}

	protected PirateWeatherComponent getWeatherComponent() {
		return IComponentRegistry.weatherComponent;
	}

	protected LinkedDeviceComponent getLinkedDeviceComponent() {
		return IComponentRegistry.linkedDeviceComponent;
	}

	public void handleVirtualDevice(DeviceData device) {
		if (!shouldAddVirtualDevice(device)) {
			return;
		}
		log.info("Trying to aquire lock");
		DynamoLockUtils.doLockedCommand(
				device.getSiteId() + "-" + device.getDate().getTime(), () -> {
					Device virtualDevice =
							getDeviceComponent().getDevicesBySiteId(device.getCustomerId(), device.getSiteId()).stream()
									.filter(Device::isVirtual)
									.findAny()
									.orElse(null);
					if (virtualDevice == null) {
						log.warn("cannot find virtualDevice " + device.getCustomerId() + ":" + device.getSiteId());
						return;
					}
					TransactionUtil.addDeviceId(virtualDevice.getId(), virtualDevice.getSiteId());
					List<DeviceData> siteDevices = getOSComponent()
							.getDevicesForSiteByTimePeriod(
									device.getCustomerId(), device.getSiteId(), device.getDate());
					DeviceData virtualDeviceData = new DeviceData(
							virtualDevice.getSiteId(), virtualDevice.getClientId(), virtualDevice.getId());
					virtualDeviceData.setVirtual(true);
					if (virtualDevice.isDeviceSite()) {
						virtualDeviceData.setSite(true);
					}
					virtualDeviceData.setDate(device.getDate());
					virtualDeviceData.setEnergyConsumed(Math.max(
							0, getPushedDeviceValues(siteDevices, virtualDevice, DeviceData::getEnergyConsumed)));
					virtualDeviceData.setTotalRealPower(Math.max(
							0, getPushedDeviceValues(siteDevices, virtualDevice, DeviceData::getTotalRealPower)));
					virtualDeviceData.setTotalEnergyConsumed(Math.max(
							0, getPushedDeviceValues(siteDevices, virtualDevice, DeviceData::getTotalEnergyConsumed)));
					getLocationComponent().addLocationData(virtualDeviceData, virtualDevice);
					getWeatherComponent().addWeatherData(virtualDeviceData, virtualDevice);
					getLinkedDeviceComponent().addLinkedDeviceDataVirtual(virtualDeviceData, siteDevices);
					log.info("updating virtual device: " + device.getDate());
					try {
						getOSComponent()
								.logData(virtualDeviceData.getDate(), Collections.singletonList(virtualDeviceData));
						OpenSearchUtils.waitForIndexing();
					} catch (ResponseException e) {
						log.error("handleVirtualDevice", e);
					}
				});
	}

	private boolean shouldAddVirtualDevice(DeviceData device) {
		if (DeviceComponent.NO_SITE.equals(device.getSiteId())) {
			return false;
		}
		List<Device> devices = getDeviceComponent().getDevicesBySiteId(device.getCustomerId(), device.getSiteId());
		if (devices.stream().noneMatch(Device::isVirtual)) {
			return false;
		}
		OpenSearchUtils.waitForIndexing();
		int openSearchDeviceCount = getOSComponent()
				.getSiteDevicesCountByTimePeriod(device.getCustomerId(), device.getSiteId(), device.getDate());
		if (devices.stream().filter(d -> !d.isDisabled()).toList().size() - 1 != openSearchDeviceCount) {
			log.debug("not calculating site "
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
