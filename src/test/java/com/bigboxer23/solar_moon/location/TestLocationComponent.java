package com.bigboxer23.solar_moon.location;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bigboxer23.solar_moon.IComponentRegistry;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.junit.jupiter.api.Test;

/** */
public class TestLocationComponent implements IComponentRegistry {
	@Test
	public void getLatLongFromCity() {
		assertTrue(
				locationComponent.getLatLongFromText("Minneapolis", "MN", "USA").isPresent());
	}

	@Test
	public void isDay() throws Exception {
		// GV, MN
		double latitude = 44.986;
		double longitude = -93.37777;

		LocalDateTime time = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).withHour(1);
		assertFalse(locationComponent.isDay(
				Date.from(time.atZone(ZoneId.of("America/Chicago")).toInstant()), latitude, longitude));

		time = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).withHour(12);
		assertTrue(locationComponent.isDay(
				Date.from(time.atZone(ZoneId.of("America/Chicago")).toInstant()), latitude, longitude));

		time = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).withHour(23);
		assertFalse(locationComponent.isDay(
				Date.from(time.atZone(ZoneId.of("America/Chicago")).toInstant()), latitude, longitude));
	}
}
