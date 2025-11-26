package com.bigboxer23.solar_moon.location;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.TestConstants;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.DeviceData;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LocationComponentTest implements TestConstants {

	private LocationComponent component;

	@BeforeEach
	public void setup() {
		component = new LocationComponent();
	}

	@Test
	public void testAddLocationData_nullData() {
		Device site = new Device();
		site.setLatitude(testLatitude);
		site.setLongitude(testLongitude);

		component.addLocationData(null, site);
	}

	@Test
	public void testAddLocationData_nullSite() {
		DeviceData data = new DeviceData();
		data.setDate(new Date());

		component.addLocationData(data, null);

		assertFalse(data.isDaylight());
	}

	@Test
	public void testAddLocationData_invalidLocation() {
		DeviceData data = new DeviceData();
		data.setDate(new Date());

		Device site = new Device();
		site.setLatitude(-1);
		site.setLongitude(-1);

		component.addLocationData(data, site);

		assertFalse(data.isDaylight());
	}

	@Test
	public void testAddLocationData_nullDate() {
		DeviceData data = new DeviceData();

		Device site = new Device();
		site.setLatitude(testLatitude);
		site.setLongitude(testLongitude);

		component.addLocationData(data, site);

		assertFalse(data.isDaylight());
	}

	@Test
	public void testAddLocationData_validDataDaytime() {
		LocalDateTime time = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).withHour(12);
		DeviceData data = new DeviceData();
		data.setDate(Date.from(time.atZone(ZoneId.of("America/Chicago")).toInstant()));

		Device site = new Device();
		site.setLatitude(testLatitude);
		site.setLongitude(testLongitude);

		component.addLocationData(data, site);

		assertTrue(data.isDaylight());
	}

	@Test
	public void testAddLocationData_validDataNighttime() {
		LocalDateTime time = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).withHour(1);
		DeviceData data = new DeviceData();
		data.setDate(Date.from(time.atZone(ZoneId.of("America/Chicago")).toInstant()));

		Device site = new Device();
		site.setLatitude(testLatitude);
		site.setLongitude(testLongitude);

		component.addLocationData(data, site);

		assertFalse(data.isDaylight());
	}

	@Test
	public void testIsDay_invalidLatLongNegativeOne() {
		LocalDateTime time = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).withHour(12);
		Optional<Boolean> result = component.isDay(
				Date.from(time.atZone(ZoneId.of("America/Chicago")).toInstant()), -1, -1);

		assertFalse(result.isPresent());
	}

	@Test
	public void testIsDay_invalidLatitudeTooHigh() {
		LocalDateTime time = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).withHour(12);
		Optional<Boolean> result = component.isDay(
				Date.from(time.atZone(ZoneId.of("America/Chicago")).toInstant()), 91, 0);

		assertFalse(result.isPresent());
	}

	@Test
	public void testIsDay_invalidLatitudeTooLow() {
		LocalDateTime time = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).withHour(12);
		Optional<Boolean> result = component.isDay(
				Date.from(time.atZone(ZoneId.of("America/Chicago")).toInstant()), -91, 0);

		assertFalse(result.isPresent());
	}

	@Test
	public void testIsDay_invalidLongitudeTooHigh() {
		LocalDateTime time = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).withHour(12);
		Optional<Boolean> result = component.isDay(
				Date.from(time.atZone(ZoneId.of("America/Chicago")).toInstant()), 0, 181);

		assertFalse(result.isPresent());
	}

	@Test
	public void testIsDay_invalidLongitudeTooLow() {
		LocalDateTime time = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).withHour(12);
		Optional<Boolean> result = component.isDay(
				Date.from(time.atZone(ZoneId.of("America/Chicago")).toInstant()), 0, -181);

		assertFalse(result.isPresent());
	}

	@Test
	public void testIsDay_daytimeNoon() {
		LocalDateTime time = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).withHour(12);
		Optional<Boolean> result = component.isDay(
				Date.from(time.atZone(ZoneId.of("America/Chicago")).toInstant()), testLatitude, testLongitude);

		assertTrue(result.isPresent());
		assertTrue(result.get());
	}

	@Test
	public void testIsDay_nighttime1AM() {
		LocalDateTime time = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).withHour(1);
		Optional<Boolean> result = component.isDay(
				Date.from(time.atZone(ZoneId.of("America/Chicago")).toInstant()), testLatitude, testLongitude);

		assertTrue(result.isPresent());
		assertFalse(result.get());
	}

	@Test
	public void testIsDay_nighttime11PM() {
		LocalDateTime time = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).withHour(23);
		Optional<Boolean> result = component.isDay(
				Date.from(time.atZone(ZoneId.of("America/Chicago")).toInstant()), testLatitude, testLongitude);

		assertTrue(result.isPresent());
		assertFalse(result.get());
	}

	@Test
	public void testIsDay_daytimeOneWeekAgo() {
		LocalDateTime time =
				LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).withHour(12).minusDays(7);
		Optional<Boolean> result = component.isDay(
				Date.from(time.atZone(ZoneId.of("America/Chicago")).toInstant()), testLatitude, testLongitude);

		assertTrue(result.isPresent());
		assertTrue(result.get());
	}

	@Test
	public void testIsDay_equatorAtNoon() {
		LocalDateTime time = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).withHour(12);
		Optional<Boolean> result =
				component.isDay(Date.from(time.atZone(ZoneId.of("UTC")).toInstant()), 0, 0);

		assertTrue(result.isPresent());
		assertTrue(result.get());
	}

	@Test
	public void testIsDay_northernLatitudeInSummer() {
		LocalDateTime time = LocalDateTime.of(2024, 6, 21, 12, 0);
		Optional<Boolean> result =
				component.isDay(Date.from(time.atZone(ZoneId.of("UTC")).toInstant()), 60, 0);

		assertTrue(result.isPresent());
		assertTrue(result.get());
	}

	@Test
	public void testGetLocalTimeString_validLocation() {
		Optional<LocalDateTime> result = component.getLocalTimeString(testLatitude, testLongitude);

		assertTrue(result.isPresent());
		assertNotNull(result.get());
	}

	@Test
	public void testGetLocalTimeString_invalidLocationNegativeOne() {
		Optional<LocalDateTime> result = component.getLocalTimeString(-1, -1);

		assertFalse(result.isPresent());
	}

	@Test
	public void testGetLocalTimeString_invalidLatitudeTooHigh() {
		Optional<LocalDateTime> result = component.getLocalTimeString(91, 0);

		assertFalse(result.isPresent());
	}

	@Test
	public void testGetLocalTimeString_invalidLongitudeTooHigh() {
		Optional<LocalDateTime> result = component.getLocalTimeString(0, 181);

		assertFalse(result.isPresent());
	}

	@Test
	public void testGetLocalTimeString_equator() {
		Optional<LocalDateTime> result = component.getLocalTimeString(0, 0);

		assertTrue(result.isPresent());
		assertNotNull(result.get());
	}

	@Test
	public void testGetLocalTimeString_northPole() {
		Optional<LocalDateTime> result = component.getLocalTimeString(89, 0);

		assertTrue(result.isPresent());
		assertNotNull(result.get());
	}

	@Test
	public void testGetLocalTimeZone_validLocation() {
		Optional<String> result = component.getLocalTimeZone(testLatitude, testLongitude);

		assertTrue(result.isPresent());
		assertEquals("America/Chicago", result.get());
	}

	@Test
	public void testGetLocalTimeZone_invalidLocationNegativeOne() {
		Optional<String> result = component.getLocalTimeZone(-1, -1);

		assertFalse(result.isPresent());
	}

	@Test
	public void testGetLocalTimeZone_invalidLatitudeTooHigh() {
		Optional<String> result = component.getLocalTimeZone(91, 0);

		assertFalse(result.isPresent());
	}

	@Test
	public void testGetLocalTimeZone_invalidLatitudeTooLow() {
		Optional<String> result = component.getLocalTimeZone(-91, 0);

		assertFalse(result.isPresent());
	}

	@Test
	public void testGetLocalTimeZone_invalidLongitudeTooHigh() {
		Optional<String> result = component.getLocalTimeZone(0, 181);

		assertFalse(result.isPresent());
	}

	@Test
	public void testGetLocalTimeZone_invalidLongitudeTooLow() {
		Optional<String> result = component.getLocalTimeZone(0, -181);

		assertFalse(result.isPresent());
	}

	@Test
	public void testGetLocalTimeZone_equator() {
		Optional<String> result = component.getLocalTimeZone(0, 0);

		assertTrue(result.isPresent());
		assertNotNull(result.get());
	}

	@Test
	public void testGetLocalTimeZone_northPole() {
		Optional<String> result = component.getLocalTimeZone(89, 2);

		assertTrue(result.isPresent());
		assertNotNull(result.get());
	}

	@Test
	public void testGetLocalTimeZone_tokyo() {
		Optional<String> result = component.getLocalTimeZone(35.6762, 139.6503);

		assertTrue(result.isPresent());
		assertEquals("Asia/Tokyo", result.get());
	}

	@Test
	public void testGetLocalTimeZone_london() {
		Optional<String> result = component.getLocalTimeZone(51.5074, -0.1278);

		assertTrue(result.isPresent());
		assertEquals("Europe/London", result.get());
	}

	@Test
	public void testGetLocalTimeZone_newYork() {
		Optional<String> result = component.getLocalTimeZone(40.7128, -74.0060);

		assertTrue(result.isPresent());
		assertEquals("America/New_York", result.get());
	}

	@Test
	public void testGetLocalTimeZone_sydney() {
		Optional<String> result = component.getLocalTimeZone(-33.8688, 151.2093);

		assertTrue(result.isPresent());
		assertEquals("Australia/Sydney", result.get());
	}
}
