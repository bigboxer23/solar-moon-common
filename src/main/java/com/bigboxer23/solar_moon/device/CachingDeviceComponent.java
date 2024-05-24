package com.bigboxer23.solar_moon.device;

import com.bigboxer23.solar_moon.data.Device;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.utils.StringUtils;

/** */
public class CachingDeviceComponent extends DeviceComponent {
	private final LoadingCache<String, Optional<Device>> deviceCache = CacheBuilder.newBuilder()
			.maximumSize(1000)
			.expireAfterWrite(10, TimeUnit.SECONDS)
			.build(new CacheLoader<>() {
				public Optional<Device> load(String key) {
					return CachingDeviceComponent.super.findDeviceById(
							key.substring(key.indexOf(":") + 1), key.substring(0, key.indexOf(":")));
				}
			});

	private final LoadingCache<String, List<Device>> devicesBySiteIdCache = CacheBuilder.newBuilder()
			.maximumSize(10)
			.expireAfterWrite(10, TimeUnit.SECONDS)
			.build(new CacheLoader<>() {
				public List<Device> load(String key) {
					return CachingDeviceComponent.super.getDevicesBySiteId(
							key.substring(0, key.indexOf(":")), key.substring(key.indexOf(":") + 1));
				}
			});

	@Override
	public List<Device> getDevicesBySiteId(String customerId, String siteId) {
		if (customerId == null || siteId == null || siteId.isBlank() || customerId.isBlank()) {
			return Collections.emptyList();
		}
		return devicesBySiteIdCache.getUnchecked(customerId + ":" + siteId);
	}

	@Override
	public Optional<Device> findDeviceById(String id, String customerId) {
		if (StringUtils.isBlank(id) || StringUtils.isBlank(customerId)) {
			return Optional.empty();
		}
		return deviceCache.getUnchecked(customerId + ":" + id);
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
}
