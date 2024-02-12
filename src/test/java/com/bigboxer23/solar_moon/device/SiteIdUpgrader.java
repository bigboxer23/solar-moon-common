package com.bigboxer23.solar_moon.device;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.TestConstants;
import org.junit.jupiter.api.Test;

/** */
public class SiteIdUpgrader implements IComponentRegistry, TestConstants {
	public void siteNameToSiteId() {
		deviceComponent.getSites().forEach(site -> deviceComponent
				.getDevicesBySite(site.getClientId(), site.getDisplayName())
				.forEach(device -> {
					device.setSiteId(site.getId());
					deviceComponent.updateDevice(device);
				}));
		deviceComponent.getDevices(false).stream()
				.filter(device -> DeviceComponent.NO_SITE.equalsIgnoreCase(device.getSite()))
				.forEach(device -> {
					device.setSiteId(DeviceComponent.NO_SITE);
					deviceComponent.updateDevice(device);
				});
	}
}
