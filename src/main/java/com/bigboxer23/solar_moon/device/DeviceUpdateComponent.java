package com.bigboxer23.solar_moon.device;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.data.DeviceUpdateData;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.utils.StringUtils;

/** */
@Slf4j
public class DeviceUpdateComponent {

	private DeviceUpdateRepository repository;

	protected DeviceUpdateRepository getRepository() {
		if (repository == null) {
			repository = new DynamoDbDeviceUpdateRepository();
		}
		return repository;
	}

	public void update(String deviceId) {
		update(deviceId, System.currentTimeMillis());
	}

	public void update(String deviceId, long time) {
		if (StringUtils.isBlank(deviceId)) {
			log.warn("invalid device id, not updating");
			return;
		}
		getRepository().update(new DeviceUpdateData(deviceId, time));
	}

	public void delete(String deviceId) {
		getRepository().delete(deviceId);
	}

	public long queryByDeviceId(String deviceId) {
		return getRepository().findLastUpdateByDeviceId(deviceId).orElse(-1L);
	}

	public void deleteByCustomerId(String customerId) {
		IComponentRegistry.deviceComponent.getDevicesForCustomerId(customerId).forEach(d -> delete(d.getId()));
	}

	public Iterable<DeviceUpdateData> getDevices() {
		return getRepository().findAll();
	}

	public Iterable<DeviceUpdateData> queryByTimeRange(long olderThan) {
		return getRepository().findByTimeRangeLessThan(olderThan);
	}
}
