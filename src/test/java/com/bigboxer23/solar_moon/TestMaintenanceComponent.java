package com.bigboxer23.solar_moon;

import com.bigboxer23.solar_moon.maintenance.MaintenanceComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
public class TestMaintenanceComponent implements IComponentRegistry
{

	@BeforeEach
	public void before() {
		maintenanceComponent.enableMaintenanceMode(false);
	}
	@Test
	public void isInMaintenanceMode() {
		assertFalse(maintenanceComponent.isInMaintenanceMode());
		maintenanceComponent.enableMaintenanceMode(true);
		assertTrue(maintenanceComponent.isInMaintenanceMode());
		maintenanceComponent.enableMaintenanceMode(false);
		assertFalse(maintenanceComponent.isInMaintenanceMode());
	}
}
