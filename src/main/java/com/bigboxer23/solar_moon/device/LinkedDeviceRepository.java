package com.bigboxer23.solar_moon.device;

import com.bigboxer23.solar_moon.data.LinkedDevice;
import java.util.Optional;

public interface LinkedDeviceRepository {

	LinkedDevice add(LinkedDevice linkedDevice);

	Optional<LinkedDevice> update(LinkedDevice linkedDevice);

	void delete(String serialNumber, String customerId);

	void deleteByCustomerId(String customerId);

	Optional<LinkedDevice> findBySerialNumber(String serialNumber, String customerId);
}
