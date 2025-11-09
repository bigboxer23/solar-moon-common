package com.bigboxer23.solar_moon.device;

import com.bigboxer23.solar_moon.data.Device;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Device data access operations. Provides abstraction layer between
 * business logic and data persistence.
 */
public interface DeviceRepository {

	Optional<Device> findDeviceByDeviceName(String customerId, String deviceName);

	Optional<Device> findDeviceByName(String customerId, String name);

	Optional<Device> findDeviceByDeviceKey(String deviceKey);

	List<Device> getDevicesBySiteId(String customerId, String siteId);

	@Deprecated
	List<Device> getDevicesBySite(String customerId, String site);

	List<Device> getDevices(boolean isVirtual);

	List<Device> getSites();

	List<Device> getDevicesForCustomerId(String customerId);

	Optional<Device> findDeviceById(String id);

	Optional<Device> findDeviceById(String id, String customerId);

	Device add(Device device);

	Optional<Device> update(Device device);

	void delete(Device device);
}
