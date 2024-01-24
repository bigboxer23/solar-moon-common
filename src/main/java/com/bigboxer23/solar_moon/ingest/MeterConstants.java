package com.bigboxer23.solar_moon.ingest;

/** */
public interface MeterConstants {
	String FILE_DATA = "LOGFILEUPLOAD";
	String MODE_PATH = "/DAS/mode";

	String DATE_PATH = "/DAS/devices/device/records/record/time";

	String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss z";

	String DATE_PATTERN_UTC = "yyyy-MM-dd HH:mm:ss";

	String ZONE = "zone";

	String POINT_PATH = "/DAS/devices/device/records/record/point";
	String DEVICE_NAME_PATH = "/DAS/devices/device/name";

	String ERROR_PATH = "/DAS/devices/device/records/record/error";

	String XML_SUCCESS_RESPONSE = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><DAS><result>SUCCESS</result></DAS>";

	String XML_FAILURE_RESPONSE = "FAILURE";

	String TOTAL_ENG_CONS = "Total Energy Consumption";
	String ENG_CONS = "Energy Consumed";
	String TOTAL_REAL_POWER = "Total Real Power";
	String DEVICE_NAME = "device-name";

	String DEVICE_ID = "device-id";

	String AVG_CURRENT = "Average Current";
	String AVG_VOLT = "Average Voltage (L-N)";
	String TOTAL_PF = "Total (System) Power Factor";

	String VIRTUAL = "Virtual";

	String IS_SITE = "isSite";

	String DAYLIGHT = "Daylight";

	String SITE = "site";
	String TEMPERATURE = "temperature";

	String UV_INDEX = "uvIndex";

	String WEATHER_SUMMARY = "weatherSummary";

	String CUSTOMER_ID_ATTRIBUTE = "customer-id";
}
