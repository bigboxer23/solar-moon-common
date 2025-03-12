package com.bigboxer23.solar_moon.util;

import static com.bigboxer23.solar_moon.TestConstants.CUSTOMER_ID;
import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.TestUtils;
import com.bigboxer23.solar_moon.alarm.IAlarmConstants;
import java.time.zone.ZoneRulesException;
import java.util.Date;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

/** */
public class TestTimeUtils {
	@AfterAll
	public static void afterAll() {
		TestUtils.nukeCustomerId(CUSTOMER_ID);
	}

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

	@Test
	public void formatUnixTimestampsInString() {
		TestUtils.setupSite();

		String message = IAlarmConstants.NO_DATA_RECENTLY + "1725887724503";
		String formattedMessage = IAlarmConstants.NO_DATA_RECENTLY + "Sep 9, 24 8:15 AM";
		String formattedMessageNYC = IAlarmConstants.NO_DATA_RECENTLY + "Sep 9, 24 9:15 AM";
		String formattedMessageLA = IAlarmConstants.NO_DATA_RECENTLY + "Sep 9, 24 6:15 AM";

		assertNull(TimeUtils.formatUnixTimestampsInString(null, null, null));
		assertEquals("t", TimeUtils.formatUnixTimestampsInString("t", null, null));
		assertEquals("t", TimeUtils.formatUnixTimestampsInString("t", "fake", null));
		assertEquals("t", TimeUtils.formatUnixTimestampsInString("t", null, "fake"));
		assertEquals(message, TimeUtils.formatUnixTimestampsInString(message, null, null));
		assertEquals(formattedMessage, TimeUtils.formatUnixTimestampsInString(message, null, CUSTOMER_ID));
		assertEquals(
				formattedMessage,
				TimeUtils.formatUnixTimestampsInString(
						message, TestUtils.getDevice().getId(), CUSTOMER_ID));

		TestUtils.getSite().setLatitude(-1);
		TestUtils.getSite().setLongitude(-1);
		TestUtils.getSite().setCity("New York");
		TestUtils.getSite().setState("New York");
		IComponentRegistry.deviceComponent.updateDevice(TestUtils.getSite());
		IComponentRegistry.customerComponent
				.findCustomerByCustomerId(CUSTOMER_ID)
				.ifPresent(c -> {
					c.setDefaultTimezone("America/Los_Angeles");
					IComponentRegistry.customerComponent.updateCustomer(c);
				});
		assertEquals(
				formattedMessageNYC,
				TimeUtils.formatUnixTimestampsInString(
						message, TestUtils.getDevice().getId(), CUSTOMER_ID));

		TestUtils.getSite().setLatitude(-1);
		TestUtils.getSite().setLongitude(-1);
		TestUtils.getSite().setCity(null);
		TestUtils.getSite().setState(null);
		TestUtils.getSite().setCountry(null);
		IComponentRegistry.deviceComponent.updateDevice(TestUtils.getSite());
		assertEquals(
				formattedMessageLA,
				TimeUtils.formatUnixTimestampsInString(
						message, TestUtils.getDevice().getId(), CUSTOMER_ID));
	}
}
