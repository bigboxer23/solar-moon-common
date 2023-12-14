package com.bigboxer23.solar_moon.util;

/** */
public interface TimeConstants {

	long SECOND = 1000;
	long ONE_MINUTE = 60 * SECOND;

	long FIFTEEN_MINUTES = ONE_MINUTE * 15;

	long THIRTY_MINUTES = FIFTEEN_MINUTES * 2;

	long FORTY_FIVE_MINUTES = FIFTEEN_MINUTES * 3;

	long HOUR = THIRTY_MINUTES * 2;

	long DAY = HOUR * 24;

	long WEEK = DAY * 7;

	long THIRTY_DAYS = DAY * 30;

	long YEAR = DAY * 365;
}
