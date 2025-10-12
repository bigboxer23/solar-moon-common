package com.bigboxer23.solar_moon.util;

/**
 * Centralized constants for property keys used throughout the application. These keys are used with
 * PropertyUtils.getProperty() to retrieve configuration values.
 */
public final class PropertyConstants {

	private PropertyConstants() {
		throw new UnsupportedOperationException("Utility class cannot be instantiated");
	}

	public static final String AWS_REGION = "aws.region";

	/** AWS Location Service index name for geocoding */
	public static final String AWS_LOCATION_INDEX = "aws.location.index";

	/** S3 bucket name for FTP ingestion storage */
	public static final String FTP_S3_BUCKET = "ftp.s3.bucket";

	/** OpenSearch cluster URL */
	public static final String OPENSEARCH_URL = "opensearch.url";

	/** OpenSearch username for authentication */
	public static final String OPENSEARCH_USER = "opensearch.user";

	/** OpenSearch password for authentication */
	public static final String OPENSEARCH_PASSWORD = "opensearch.pw";

	/** Email sender address for notifications */
	public static final String EMAILER_INFO = "emailer.info";

	/** Support email address for support-related notifications */
	public static final String EMAILER_SUPPORT = "emailer.support";

	/** Override recipient for all emails (typically used in dev/test environments) */
	public static final String RECIPIENT_OVERRIDE = "recipient_override";

	/** Additional recipient to include on all emails (for monitoring purposes) */
	public static final String ADDITIONAL_RECIPIENT = "additional_recipient";

	/** PirateWeather API key for weather data */
	public static final String PIRATE_WEATHER_API = "pirate.weather.api";

	/** OpenWeatherMap API key (legacy weather provider) */
	public static final String OPENWEATHERMAP_API = "openweathermap.api";
}
