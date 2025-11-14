package com.bigboxer23.solar_moon.maintenance;

import java.util.Optional;

public interface MaintenanceRepository {
	Optional<MaintenanceMode> findMaintenanceMode();

	void enableMaintenanceMode();

	void disableMaintenanceMode();
}
