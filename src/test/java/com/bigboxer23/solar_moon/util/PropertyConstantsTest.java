package com.bigboxer23.solar_moon.util;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.Test;

public class PropertyConstantsTest {

	@Test
	public void testConstructor_throwsUnsupportedOperationException() {
		assertThrows(InvocationTargetException.class, () -> {
			Constructor<PropertyConstants> constructor = PropertyConstants.class.getDeclaredConstructor();
			constructor.setAccessible(true);
			constructor.newInstance();
		});
	}

	@Test
	public void testConstructor_actualExceptionIsUnsupportedOperationException() {
		try {
			Constructor<PropertyConstants> constructor = PropertyConstants.class.getDeclaredConstructor();
			constructor.setAccessible(true);
			constructor.newInstance();
			fail("Expected InvocationTargetException to be thrown");
		} catch (InvocationTargetException e) {
			assertTrue(e.getCause() instanceof UnsupportedOperationException);
			assertEquals("Utility class cannot be instantiated", e.getCause().getMessage());
		} catch (Exception e) {
			fail("Unexpected exception: " + e.getClass().getName());
		}
	}

	@Test
	public void testAwsRegionConstant() {
		assertEquals("aws.region", PropertyConstants.AWS_REGION);
	}

	@Test
	public void testAwsLocationIndexConstant() {
		assertEquals("aws.location.index", PropertyConstants.AWS_LOCATION_INDEX);
	}

	@Test
	public void testFtpS3BucketConstant() {
		assertEquals("ftp.s3.bucket", PropertyConstants.FTP_S3_BUCKET);
	}

	@Test
	public void testOpenSearchUrlConstant() {
		assertEquals("opensearch.url", PropertyConstants.OPENSEARCH_URL);
	}

	@Test
	public void testOpenSearchUserConstant() {
		assertEquals("opensearch.user", PropertyConstants.OPENSEARCH_USER);
	}

	@Test
	public void testOpenSearchPasswordConstant() {
		assertEquals("opensearch.pw", PropertyConstants.OPENSEARCH_PASSWORD);
	}

	@Test
	public void testEmailerInfoConstant() {
		assertEquals("emailer.info", PropertyConstants.EMAILER_INFO);
	}

	@Test
	public void testEmailerSupportConstant() {
		assertEquals("emailer.support", PropertyConstants.EMAILER_SUPPORT);
	}

	@Test
	public void testRecipientOverrideConstant() {
		assertEquals("recipient_override", PropertyConstants.RECIPIENT_OVERRIDE);
	}

	@Test
	public void testAdditionalRecipientConstant() {
		assertEquals("additional_recipient", PropertyConstants.ADDITIONAL_RECIPIENT);
	}

	@Test
	public void testPirateWeatherApiConstant() {
		assertEquals("pirate.weather.api", PropertyConstants.PIRATE_WEATHER_API);
	}

	@Test
	public void testOpenWeatherMapApiConstant() {
		assertEquals("openweathermap.api", PropertyConstants.OPENWEATHERMAP_API);
	}

	@Test
	public void testAllConstantsAreNonNull() {
		assertNotNull(PropertyConstants.AWS_REGION);
		assertNotNull(PropertyConstants.AWS_LOCATION_INDEX);
		assertNotNull(PropertyConstants.FTP_S3_BUCKET);
		assertNotNull(PropertyConstants.OPENSEARCH_URL);
		assertNotNull(PropertyConstants.OPENSEARCH_USER);
		assertNotNull(PropertyConstants.OPENSEARCH_PASSWORD);
		assertNotNull(PropertyConstants.EMAILER_INFO);
		assertNotNull(PropertyConstants.EMAILER_SUPPORT);
		assertNotNull(PropertyConstants.RECIPIENT_OVERRIDE);
		assertNotNull(PropertyConstants.ADDITIONAL_RECIPIENT);
		assertNotNull(PropertyConstants.PIRATE_WEATHER_API);
		assertNotNull(PropertyConstants.OPENWEATHERMAP_API);
	}

	@Test
	public void testAllConstantsAreNonEmpty() {
		assertFalse(PropertyConstants.AWS_REGION.isEmpty());
		assertFalse(PropertyConstants.AWS_LOCATION_INDEX.isEmpty());
		assertFalse(PropertyConstants.FTP_S3_BUCKET.isEmpty());
		assertFalse(PropertyConstants.OPENSEARCH_URL.isEmpty());
		assertFalse(PropertyConstants.OPENSEARCH_USER.isEmpty());
		assertFalse(PropertyConstants.OPENSEARCH_PASSWORD.isEmpty());
		assertFalse(PropertyConstants.EMAILER_INFO.isEmpty());
		assertFalse(PropertyConstants.EMAILER_SUPPORT.isEmpty());
		assertFalse(PropertyConstants.RECIPIENT_OVERRIDE.isEmpty());
		assertFalse(PropertyConstants.ADDITIONAL_RECIPIENT.isEmpty());
		assertFalse(PropertyConstants.PIRATE_WEATHER_API.isEmpty());
		assertFalse(PropertyConstants.OPENWEATHERMAP_API.isEmpty());
	}
}
