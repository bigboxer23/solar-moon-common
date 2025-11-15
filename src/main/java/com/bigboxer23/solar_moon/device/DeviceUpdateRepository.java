package com.bigboxer23.solar_moon.device;

import com.bigboxer23.solar_moon.data.DeviceUpdateData;
import java.util.Optional;

public interface DeviceUpdateRepository {

	DeviceUpdateData update(DeviceUpdateData deviceUpdate);

	void delete(String deviceId);

	Optional<Long> findLastUpdateByDeviceId(String deviceId);

	Iterable<DeviceUpdateData> findAll();

	Iterable<DeviceUpdateData> findByTimeRangeLessThan(long olderThan);
}
