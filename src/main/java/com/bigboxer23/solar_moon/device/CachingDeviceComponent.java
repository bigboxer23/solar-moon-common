package com.bigboxer23.solar_moon.device;

import com.bigboxer23.solar_moon.data.Device;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.utils.StringUtils;

/** */
public class CachingDeviceComponent extends DeviceComponent {
	private final LoadingCache<String, Optional<Device>> deviceCache = Caffeine.newBuilder()
			.maximumSize(1000)
			.expireAfterWrite(Duration.ofSeconds(10))
			.build(key -> CachingDeviceComponent.super.findDeviceById(
					key.substring(key.indexOf(":") + 1), key.substring(0, key.indexOf(":"))));

	private final LoadingCache<String, List<Device>> devicesBySiteIdCache = Caffeine.newBuilder()
			.maximumSize(10)
			.expireAfterWrite(Duration.ofSeconds(10))
			.build(key -> CachingDeviceComponent.super.getDevicesBySiteId(
					key.substring(0, key.indexOf(":")), key.substring(key.indexOf(":") + 1)));

	@Override
	public List<Device> getDevicesBySiteId(String customerId, String siteId) {
		if (customerId == null || siteId == null || siteId.isBlank() || customerId.isBlank()) {
			return Collections.emptyList();
		}
		return devicesBySiteIdCache.get(customerId + ":" + siteId);
	}

	@Override
	public Optional<Device> findDeviceById(String id, String customerId) {
		if (StringUtils.isBlank(id) || StringUtils.isBlank(customerId)) {
			return Optional.empty();
		}
		return deviceCache.get(customerId + ":" + id);
	}

	@Override
	public Device addDevice(Device device) {
		Device d = super.addDevice(device);
		invalidate(device.getClientId(), device.getId(), device.getSiteId());
		return d;
	}

	@Override
	public void deleteDevice(String id, String customerId) {
		String siteId = findDeviceById(id, customerId).map(Device::getSiteId).orElse(null);
		invalidate(customerId, id, siteId);
		super.deleteDevice(id, customerId);
		invalidate(customerId, id, siteId);
	}

	@Override
	public Optional<Device> updateDevice(Device device) {
		invalidate(device.getClientId(), device.getId(), device.getSiteId());
		Optional<Device> d = super.updateDevice(device);
		invalidate(device.getClientId(), device.getId(), device.getSiteId());
		return d;
	}

	private void invalidate(String customerId, String deviceId, String siteId) {
		deviceCache.invalidate(customerId + ":" + deviceId);
		devicesBySiteIdCache.invalidate(customerId + ":" + siteId);
	}

	@Override
	public void deleteDevicesByCustomerId(String customerId) {
		super.deleteDevicesByCustomerId(customerId);
		invalidateAllForCustomer(customerId);
	}

	public void invalidateAllForCustomer(String customerId) {
		deviceCache.invalidateAll();
		devicesBySiteIdCache.invalidateAll();
	}
}
