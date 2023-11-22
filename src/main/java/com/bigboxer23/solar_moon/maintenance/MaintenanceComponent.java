package com.bigboxer23.solar_moon.maintenance;

import com.bigboxer23.solar_moon.AbstractDynamodbComponent;
import java.util.Optional;

/** */
public class MaintenanceComponent extends AbstractDynamodbComponent<MaintenanceMode> {
	public boolean isInMaintenanceMode() {
		return Optional.ofNullable(getTable().getItem(new MaintenanceMode()))
				.map(MaintenanceMode::isInMaintenanceMode)
				.orElse(false);
	}

	public void enableMaintenanceMode(boolean isEnable) {
		if (isEnable) {
			getTable().updateItem(builder -> builder.item(new MaintenanceMode()));
		} else {
			getTable().deleteItem(new MaintenanceMode());
		}
	}

	@Override
	protected String getTableName() {
		return "maintenanceMode";
	}

	@Override
	protected Class<MaintenanceMode> getObjectClass() {
		return MaintenanceMode.class;
	}
}
