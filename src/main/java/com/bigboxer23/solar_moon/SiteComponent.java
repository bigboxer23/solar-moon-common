package com.bigboxer23.solar_moon;

import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import com.bigboxer23.solar_moon.dynamodb.DynamoLockUtils;
import com.bigboxer23.solar_moon.open_search.OpenSearchComponent;
import com.bigboxer23.solar_moon.open_search.OpenSearchUtils;
import java.util.*;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Component to stash all the logic related to aggregating virtual site devices */
// @Component
public class SiteComponent {

	private static final Logger logger = LoggerFactory.getLogger(SiteComponent.class);

	private final OpenSearchComponent openSearch;

	private final DeviceComponent component;

	public SiteComponent(OpenSearchComponent component, DeviceComponent deviceComponent) {
		openSearch = component;
		this.component = deviceComponent;
	}

	public void handleSite(DeviceData device) {
		if (!shouldAddSiteDevice(device)) {
			return;
		}
		logger.info("Trying to aquire lock " + device.getName());
		DynamoLockUtils.doLockedCommand(
				device.getSite() + "-" + device.getDate().getTime(), device.getName(), () -> {
					Device site = component.getDevicesBySite(device.getCustomerId(), device.getSite()).stream()
							.filter(Device::isVirtual)
							.findAny()
							.orElse(null);
					if (site == null) {
						logger.warn("cannot find site " + device.getCustomerId() + ":" + device.getSite());
						return;
					}
					List<DeviceData> siteDevices = openSearch.getDevicesForSiteByTimePeriod(
							device.getCustomerId(), device.getSite(), device.getDate());
					DeviceData siteDevice = new DeviceData(device.getSite(), device.getSite(), device.getCustomerId());
					siteDevice.setIsVirtual();

					float totalEnergyConsumed = getPushedDeviceValues(siteDevices, site, DeviceData::getEnergyConsumed);
					if (totalEnergyConsumed > -1) {
						siteDevice.setEnergyConsumed(
								Math.max(0, siteDevice.getTotalEnergyConsumed()) + totalEnergyConsumed);
					}
					float totalRealPower = getPushedDeviceValues(siteDevices, site, DeviceData::getTotalRealPower);
					if (totalRealPower > -1) {
						siteDevice.setTotalRealPower(Math.max(0, siteDevice.getTotalRealPower()) + totalRealPower);
					}
					logger.info("adding virtual device " + device.getSite() + " : " + device.getDate());
					openSearch.logData(device.getDate(), Collections.singletonList(siteDevice));
					OpenSearchUtils.waitForIndexing();
				});
	}

	private boolean shouldAddSiteDevice(DeviceData device) {
		OpenSearchUtils.waitForIndexing();
		int deviceCount = component
				.getDevicesBySite(device.getCustomerId(), device.getSite())
				.size();
		int openSearchDeviceCount =
				openSearch.getSiteDevicesCountByTimePeriod(device.getCustomerId(), device.getSite(), device.getDate());
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
