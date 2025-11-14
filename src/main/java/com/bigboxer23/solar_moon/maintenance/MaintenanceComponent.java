package com.bigboxer23.solar_moon.maintenance;

import com.bigboxer23.solar_moon.IComponentRegistry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MaintenanceComponent {
	private MaintenanceRepository repository;

	protected MaintenanceRepository getRepository() {
		if (repository == null) {
			repository = new DynamoDbMaintenanceRepository();
		}
		return repository;
	}

	public boolean isInMaintenanceMode() {
		return getRepository()
				.findMaintenanceMode()
				.map(MaintenanceMode::isInMaintenanceMode)
				.orElse(false);
	}

	public void enableMaintenanceMode(boolean isEnable) {
		if (isEnable) {
			getRepository().enableMaintenanceMode();
		} else {
			getRepository().disableMaintenanceMode();
		}
	}

	public void cleanupOldLogs() {
		IComponentRegistry.OSComponent.deleteOldLogs();
	}
}
