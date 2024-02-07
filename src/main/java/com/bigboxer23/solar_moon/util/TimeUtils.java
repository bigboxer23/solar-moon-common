package com.bigboxer23.solar_moon.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/** */
public class TimeUtils implements TimeConstants {
	/**
	 * round to nearest 15m interval
	 *
	 * @return
	 */
	public static Date get15mRoundedDate() {
		LocalDateTime now = LocalDateTime.now();
		return Date.from(now.truncatedTo(ChronoUnit.MINUTES)
				.withMinute(now.getMinute() / 15 * 15)
				.atZone(ZoneId.systemDefault())
				.toInstant());
	}

	public static Date getStartOfDay(String timeZone) {
		ZoneId zoneId = ZoneId.of(timeZone);
		LocalDate now = LocalDate.now(zoneId);
		ZonedDateTime zdtStart = now.atStartOfDay(zoneId);
		return Date.from(zdtStart.toInstant());
	}

	public static String getFormattedZonedTime(long epochMs, String timeZone) {
		ZoneId zoneId = ZoneId.of(timeZone);
		Instant i = Instant.ofEpochSecond(epochMs / 1000);
		ZonedDateTime z = ZonedDateTime.ofInstant(i, zoneId);
		return z.format(DateTimeFormatter.ofPattern("MMM d, yy h:mm a"));
	}
}
