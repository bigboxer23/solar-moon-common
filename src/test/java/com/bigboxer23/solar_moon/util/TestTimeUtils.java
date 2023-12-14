package com.bigboxer23.solar_moon.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
