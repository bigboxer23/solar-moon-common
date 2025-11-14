package com.bigboxer23.solar_moon.maintenance;

import com.bigboxer23.solar_moon.dynamodb.AbstractDynamodbComponent;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DynamoDbMaintenanceRepository extends AbstractDynamodbComponent<MaintenanceMode>
		implements MaintenanceRepository {

	@Override
	public Optional<MaintenanceMode> findMaintenanceMode() {
		return Optional.ofNullable(getTable().getItem(new MaintenanceMode()));
	}

	@Override
	public void enableMaintenanceMode() {
		getTable().updateItem(builder -> builder.item(new MaintenanceMode()));
	}

	@Override
	public void disableMaintenanceMode() {
		getTable().deleteItem(new MaintenanceMode());
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
