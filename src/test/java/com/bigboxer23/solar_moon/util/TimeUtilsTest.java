package com.bigboxer23.solar_moon.util;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.Date;
import org.junit.jupiter.api.Test;

public class TimeUtilsTest {

	@Test
	public void testGet15mRoundedDate_roundsToNearestQuarterHour() {
		Date result = TimeUtils.get15mRoundedDate();

		assertNotNull(result);
		LocalDateTime dateTime = LocalDateTime.ofInstant(result.toInstant(), ZoneId.systemDefault());
		int minutes = dateTime.getMinute();
		assertTrue(minutes == 0 || minutes == 15 || minutes == 30 || minutes == 45);
		assertEquals(0, dateTime.getSecond());
	}

	@Test
	public void testGet15mRoundedDate_hasZeroSeconds() {
		Date result = TimeUtils.get15mRoundedDate();

		LocalDateTime dateTime = LocalDateTime.ofInstant(result.toInstant(), ZoneId.systemDefault());

		assertEquals(0, dateTime.getSecond());
	}

	@Test
	public void testGet15mRoundedDate_isNotNull() {
		Date result = TimeUtils.get15mRoundedDate();

		assertNotNull(result);
	}

	@Test
	public void testGet15mRoundedDate_isInPastOrPresent() {
		Date result = TimeUtils.get15mRoundedDate();

		assertFalse(result.after(new Date()));
	}

	@Test
	public void testGetStartOfDay_withUTC() {
		Date result = TimeUtils.getStartOfDay("UTC");

		assertNotNull(result);
		LocalDateTime dateTime = LocalDateTime.ofInstant(result.toInstant(), ZoneId.of("UTC"));
		assertEquals(0, dateTime.getHour());
		assertEquals(0, dateTime.getMinute());
		assertEquals(0, dateTime.getSecond());
	}

	@Test
	public void testGetStartOfDay_withNewYorkTimeZone() {
		Date result = TimeUtils.getStartOfDay("America/New_York");

		assertNotNull(result);
		ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(result.toInstant(), ZoneId.of("America/New_York"));
		assertEquals(0, zonedDateTime.getHour());
		assertEquals(0, zonedDateTime.getMinute());
		assertEquals(0, zonedDateTime.getSecond());
	}

	@Test
	public void testGetStartOfDay_withTokyoTimeZone() {
		Date result = TimeUtils.getStartOfDay("Asia/Tokyo");

		assertNotNull(result);
		ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(result.toInstant(), ZoneId.of("Asia/Tokyo"));
		assertEquals(0, zonedDateTime.getHour());
		assertEquals(0, zonedDateTime.getMinute());
	}

	@Test
	public void testGetStartOfDay_withLondonTimeZone() {
		Date result = TimeUtils.getStartOfDay("Europe/London");

		assertNotNull(result);
		ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(result.toInstant(), ZoneId.of("Europe/London"));
		assertEquals(0, zonedDateTime.getHour());
	}

	@Test
	public void testGetStartOfDay_isBeforeOrEqualToNow() {
		Date result = TimeUtils.getStartOfDay("UTC");

		assertFalse(result.after(new Date()));
	}

	@Test
	public void testGetFormattedZonedTime_withKnownTimestamp() {
		long epochMs = 1700000000000L;
		String timeZone = "UTC";

		String result = TimeUtils.getFormattedZonedTime(epochMs, timeZone);

		assertNotNull(result);
		assertTrue(result.contains("Nov"));
		assertTrue(result.contains("23"));
	}

	@Test
	public void testGetFormattedZonedTime_withNewYorkTimeZone() {
		long epochMs = 1700000000000L;
		String timeZone = "America/New_York";

		String result = TimeUtils.getFormattedZonedTime(epochMs, timeZone);

		assertNotNull(result);
		assertFalse(result.isEmpty());
	}

	@Test
	public void testGetFormattedZonedTime_containsAmOrPm() {
		long epochMs = 1700000000000L;
		String timeZone = "UTC";

		String result = TimeUtils.getFormattedZonedTime(epochMs, timeZone);

		assertTrue(result.toLowerCase().contains("am") || result.toLowerCase().contains("pm"));
	}

	@Test
	public void testGetFormattedZonedTime_withZeroTimestamp() {
		long epochMs = 0L;
		String timeZone = "UTC";

		String result = TimeUtils.getFormattedZonedTime(epochMs, timeZone);

		assertNotNull(result);
		assertTrue(result.contains("Jan"));
		assertTrue(result.contains("1970") || result.contains("70"));
	}

	@Test
	public void testGetFormattedZonedTime_withRecentTimestamp() {
		long epochMs = System.currentTimeMillis();
		String timeZone = "UTC";

		String result = TimeUtils.getFormattedZonedTime(epochMs, timeZone);

		assertNotNull(result);
		assertFalse(result.isEmpty());
		assertTrue(result.matches(".*\\d+.*"));
	}

	@Test
	public void testFormatUnixTimestampsInString_withNullMessage() {
		String result = TimeUtils.formatUnixTimestampsInString(null, "deviceId", "customerId");

		assertNull(result);
	}

	@Test
	public void testFormatUnixTimestampsInString_withEmptyMessage() {
		String result = TimeUtils.formatUnixTimestampsInString("", "deviceId", "customerId");

		assertEquals("", result);
	}

	@Test
	public void testFormatUnixTimestampsInString_withBlankMessage() {
		String result = TimeUtils.formatUnixTimestampsInString("   ", "deviceId", "customerId");

		assertEquals("   ", result);
	}

	@Test
	public void testFormatUnixTimestampsInString_withNullDeviceIdAndCustomerId() {
		String result = TimeUtils.formatUnixTimestampsInString("test message", null, null);

		assertEquals("test message", result);
	}

	@Test
	public void testFormatUnixTimestampsInString_withBlankDeviceId() {
		String result = TimeUtils.formatUnixTimestampsInString("test message", "", "customerId");

		assertEquals("test message", result);
	}

	@Test
	public void testFormatUnixTimestampsInString_withBlankCustomerId() {
		String result = TimeUtils.formatUnixTimestampsInString("test message", "deviceId", "");

		assertEquals("test message", result);
	}

	@Test
	public void testFormatUnixTimestampsInString_withNoTimestamp() {
		String result = TimeUtils.formatUnixTimestampsInString("message with no timestamp", "deviceId", "customerId");

		assertEquals("message with no timestamp", result);
	}

	@Test
	public void testFormatUnixTimestampsInString_withShortTimestamp() {
		String result = TimeUtils.formatUnixTimestampsInString("timestamp: 123456", "deviceId", "customerId");

		assertEquals("timestamp: 123456", result);
	}

	@Test
	public void testGetTimeZone_withNullDevice() {
		var result = TimeUtils.getTimeZone(null);

		assertTrue(result.isEmpty());
	}
}
