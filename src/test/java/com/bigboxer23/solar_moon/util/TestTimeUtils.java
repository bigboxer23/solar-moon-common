package com.bigboxer23.solar_moon.util;

import static org.junit.jupiter.api.Assertions.*;

import java.time.zone.ZoneRulesException;
import java.util.Date;
import org.junit.jupiter.api.Test;

/** */
public class TestTimeUtils {
	@Test
	public void get15mRoundedDate() {
		Date rounded = TimeUtils.get15mRoundedDate();
		assertTrue(rounded.getMinutes() == 0 || rounded.getMinutes() == 15 || rounded.getMinutes() == 45);
	}

	/** Assumes to run in CST */
	@Test
	public void getStartOfDay() {
		assertEquals(23, TimeUtils.getStartOfDay("America/New_York").getHours());
		assertEquals(2, TimeUtils.getStartOfDay("America/Los_Angeles").getHours());
		assertEquals(0, TimeUtils.getStartOfDay("America/Chicago").getHours());
	}

	@Test
	public void getFormattedZonedTime() {
		long sampleTime = 1707265737918L;
		assertEquals("Feb 6, 24 7:28 PM", TimeUtils.getFormattedZonedTime(sampleTime, "America/New_York"));
		assertEquals("Feb 6, 24 4:28 PM", TimeUtils.getFormattedZonedTime(sampleTime, "America/Los_Angeles"));
		assertEquals("Feb 6, 24 6:28 PM", TimeUtils.getFormattedZonedTime(sampleTime, "America/Chicago"));
		try {
			TimeUtils.getFormattedZonedTime(System.currentTimeMillis(), "America/New_Zork");
			fail();
		} catch (ZoneRulesException zre) {

		}
	}
}
