package com.bigboxer23.solar_moon.location;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bigboxer23.solar_moon.IComponentRegistry;
import org.junit.jupiter.api.Test;

/** */
public class TestLocationComponent implements IComponentRegistry {
	@Test
	public void getLatLongFromCity() {
		assertTrue(
				locationComponent.getLatLongFromText("Minneapolis", "MN", "USA").isPresent());
	}
}
