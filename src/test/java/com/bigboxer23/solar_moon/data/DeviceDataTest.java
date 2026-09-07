package com.bigboxer23.solar_moon.data;

import static com.bigboxer23.solar_moon.ingest.MeterConstants.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;
import org.junit.jupiter.api.Test;

public class DeviceDataTest {

	private static final String A_SITE = "site-123";
	private static final String A_CUSTOMER = "customer-123";
	private static final String A_DEVICE = "device-123";

	@Test
	void testCreateEmpty_setsZeroedProductionValues() {
		Date timestamp = new Date();

		DeviceData data = DeviceData.createEmpty(A_SITE, A_CUSTOMER, A_DEVICE, timestamp);

		assertEquals(A_SITE, data.getSiteId());
		assertEquals(A_CUSTOMER, data.getCustomerId());
		assertEquals(A_DEVICE, data.getDeviceId());
		assertEquals(timestamp, data.getDate());
		assertEquals(1, data.getPowerFactor());
		assertEquals(0, data.getAverageCurrent());
		assertEquals(0, data.getAverageVoltage());
		assertEquals(0, data.getTotalEnergyConsumed());
		assertEquals(0, data.getTotalRealPower());
	}

	@Test
	void testCopyConstructor_copiesEveryField() {
		DeviceData original = createFullyPopulated();

		DeviceData copy = new DeviceData(original);

		assertEquals(original.getSiteId(), copy.getSiteId());
		assertEquals(original.getCustomerId(), copy.getCustomerId());
		assertEquals(original.getDeviceId(), copy.getDeviceId());
		assertEquals(original.getTotalRealPower(), copy.getTotalRealPower());
		assertEquals(original.getEnergyConsumed(), copy.getEnergyConsumed());
		assertEquals(original.getPowerFactor(), copy.getPowerFactor());
		assertEquals(original.getAverageVoltage(), copy.getAverageVoltage());
		assertEquals(original.getAverageCurrent(), copy.getAverageCurrent());
		assertEquals(original.getTotalEnergyConsumed(), copy.getTotalEnergyConsumed());
		assertEquals(original.getDate(), copy.getDate());
		assertEquals(original.isVirtual(), copy.isVirtual());
		assertEquals(original.isSite(), copy.isSite());
		assertEquals(original.isDaylight(), copy.isDaylight());
		assertEquals(original.getWeatherSummary(), copy.getWeatherSummary());
		assertEquals(original.getTemperature(), copy.getTemperature());
		assertEquals(original.getVisibility(), copy.getVisibility());
		assertEquals(original.getCloudCover(), copy.getCloudCover());
		assertEquals(original.getUVIndex(), copy.getUVIndex());
		assertEquals(original.getPrecipitationIntensity(), copy.getPrecipitationIntensity());
		assertEquals(original.getInformationalError(), copy.getInformationalError());
		assertEquals(original.getCriticalError(), copy.getCriticalError());
		assertEquals(original.getCriticalErrorString(), copy.getCriticalErrorString());
		assertEquals(original.getInformationalErrorString(), copy.getInformationalErrorString());
	}

	@Test
	void testCopyConstructor_producesIndependentInstance() {
		DeviceData original = createFullyPopulated();

		DeviceData copy = new DeviceData(original);
		copy.setTotalRealPower(999f);

		assertEquals(12.5f, original.getTotalRealPower());
	}

	@Test
	void testAddAttribute_totalRealPower() {
		DeviceData data = new DeviceData();
		data.addAttribute(TOTAL_REAL_POWER, 12.5d);
		assertEquals(12.5f, data.getTotalRealPower());
	}

	@Test
	void testAddAttribute_totalEnergyConsumed() {
		DeviceData data = new DeviceData();
		data.addAttribute(TOTAL_ENG_CONS, 100.25d);
		assertEquals(100.25f, data.getTotalEnergyConsumed());
	}

	@Test
	void testAddAttribute_energyConsumed() {
		DeviceData data = new DeviceData();
		data.addAttribute(ENG_CONS, 3.5d);
		assertEquals(3.5f, data.getEnergyConsumed());
	}

	@Test
	void testAddAttribute_virtual() {
		DeviceData data = new DeviceData();
		data.addAttribute(VIRTUAL, true);
		assertTrue(data.isVirtual());
	}

	@Test
	void testAddAttribute_isSite() {
		DeviceData data = new DeviceData();
		data.addAttribute(IS_SITE, true);
		assertTrue(data.isSite());
	}

	@Test
	void testAddAttribute_daylight() {
		DeviceData data = new DeviceData();
		data.addAttribute(DAYLIGHT, true);
		assertTrue(data.isDaylight());
	}

	@Test
	void testAddAttribute_temperature() {
		DeviceData data = new DeviceData();
		data.addAttribute(TEMPERATURE, 72.5d);
		assertEquals(72.5f, data.getTemperature());
	}

	@Test
	void testAddAttribute_uvIndex() {
		DeviceData data = new DeviceData();
		data.addAttribute(UV_INDEX, 4.0d);
		assertEquals(4.0f, data.getUVIndex());
	}

	@Test
	void testAddAttribute_precipitationIntensity() {
		DeviceData data = new DeviceData();
		data.addAttribute(PRECIPITATION_INTENSITY, 0.75d);
		assertEquals(0.75f, data.getPrecipitationIntensity());
	}

	@Test
	void testAddAttribute_precipIntensityAliasMapsToSameField() {
		DeviceData data = new DeviceData();
		data.addAttribute(PRECIP_INTENSITY, 0.25d);
		assertEquals(0.25f, data.getPrecipitationIntensity());
	}

	@Test
	void testAddAttribute_weatherSummary() {
		DeviceData data = new DeviceData();
		data.addAttribute(WEATHER_SUMMARY, "Rain");
		assertEquals("Rain", data.getWeatherSummary());
	}

	@Test
	void testAddAttribute_weatherIcon() {
		DeviceData data = new DeviceData();
		data.addAttribute(WEATHER_ICON, "rain-icon");
		assertEquals("rain-icon", data.getIcon());
	}

	@Test
	void testAddAttribute_siteId() {
		DeviceData data = new DeviceData();
		data.addAttribute(SITE_ID, A_SITE);
		assertEquals(A_SITE, data.getSiteId());
	}

	@Test
	void testAddAttribute_customerId() {
		DeviceData data = new DeviceData();
		data.addAttribute(CUSTOMER_ID_ATTRIBUTE, A_CUSTOMER);
		assertEquals(A_CUSTOMER, data.getCustomerId());
	}

	@Test
	void testAddAttribute_deviceId() {
		DeviceData data = new DeviceData();
		data.addAttribute(DEVICE_ID, A_DEVICE);
		assertEquals(A_DEVICE, data.getDeviceId());
	}

	@Test
	void testAddAttribute_averageVoltage() {
		DeviceData data = new DeviceData();
		data.addAttribute(AVG_VOLT, 240.5d);
		assertEquals(240.5f, data.getAverageVoltage());
	}

	@Test
	void testAddAttribute_averageCurrent() {
		DeviceData data = new DeviceData();
		data.addAttribute(AVG_CURRENT, 15.25d);
		assertEquals(15.25f, data.getAverageCurrent());
	}

	@Test
	void testAddAttribute_powerFactor() {
		DeviceData data = new DeviceData();
		data.addAttribute(TOTAL_PF, 0.95d);
		assertEquals(0.95f, data.getPowerFactor());
	}

	@Test
	void testAddAttribute_visibility() {
		DeviceData data = new DeviceData();
		data.addAttribute(VISIBILITY, 10.0d);
		assertEquals(10.0f, data.getVisibility());
	}

	@Test
	void testAddAttribute_cloudCover() {
		DeviceData data = new DeviceData();
		data.addAttribute(CLOUD_COVER, 0.4d);
		assertEquals(0.4f, data.getCloudCover());
	}

	@Test
	void testAddAttribute_criticalErrorRoundsToInt() {
		DeviceData data = new DeviceData();
		data.addAttribute(CRITICAL_ERROR, 12.6d);
		assertEquals(13, data.getCriticalError());
	}

	@Test
	void testAddAttribute_informationalErrorRoundsToInt() {
		DeviceData data = new DeviceData();
		data.addAttribute(INFORMATIONAL_ERROR, 7.4d);
		assertEquals(7, data.getInformationalError());
	}

	@Test
	void testAddAttribute_criticalErrorString() {
		DeviceData data = new DeviceData();
		data.addAttribute(CRITICAL_ERROR_STRING, "critical failure");
		assertEquals("critical failure", data.getCriticalErrorString());
	}

	@Test
	void testAddAttribute_informationalErrorString() {
		DeviceData data = new DeviceData();
		data.addAttribute(INFORMATIONAL_ERROR_STRING, "informational");
		assertEquals("informational", data.getInformationalErrorString());
	}

	@Test
	void testAddAttribute_withUnknownKey_throws() {
		DeviceData data = new DeviceData();

		RuntimeException exception =
				assertThrows(RuntimeException.class, () -> data.addAttribute("not a real field", 1.0d));

		assertTrue(exception.getMessage().contains("not a real field"));
	}

	@Test
	void testAddAttribute_withIntegerValue_convertsToFloat() {
		DeviceData data = new DeviceData();
		data.addAttribute(TOTAL_REAL_POWER, 42);
		assertEquals(42f, data.getTotalRealPower());
	}

	@Test
	void testAddAttribute_withFloatValue_isPreserved() {
		DeviceData data = new DeviceData();
		data.addAttribute(TOTAL_REAL_POWER, 42.5f);
		assertEquals(42.5f, data.getTotalRealPower());
	}

	@Test
	void testAddAttribute_withNullValue_becomesNegativeOne() {
		DeviceData data = new DeviceData();
		data.addAttribute(TOTAL_REAL_POWER, null);
		assertEquals(-1f, data.getTotalRealPower());
	}

	@Test
	void testIsValid_withCompletePhysicalReading_returnsTrue() {
		assertTrue(createValidPhysicalReading().isValid());
	}

	@Test
	void testIsValid_withBlankSiteId_returnsFalse() {
		DeviceData data = createValidPhysicalReading();
		data.setSiteId("");
		assertFalse(data.isValid());
	}

	@Test
	void testIsValid_withBlankCustomerId_returnsFalse() {
		DeviceData data = createValidPhysicalReading();
		data.setCustomerId("");
		assertFalse(data.isValid());
	}

	@Test
	void testIsValid_withBlankDeviceId_returnsFalse() {
		DeviceData data = createValidPhysicalReading();
		data.setDeviceId("");
		assertFalse(data.isValid());
	}

	@Test
	void testIsValid_withNullDate_returnsFalse() {
		DeviceData data = createValidPhysicalReading();
		data.setDate(null);
		assertFalse(data.isValid());
	}

	@Test
	void testIsValid_virtualDeviceSkipsReadingChecks() {
		DeviceData data = new DeviceData(A_SITE, A_CUSTOMER, A_DEVICE);
		data.setDate(new Date());
		data.setVirtual(true);

		assertTrue(data.isValid());
	}

	@Test
	void testIsValid_withMissingVoltage_returnsFalse() {
		DeviceData data = createValidPhysicalReading();
		data.setAverageVoltage(-1);
		assertFalse(data.isValid());
	}

	@Test
	void testIsValid_withMissingCurrent_returnsFalse() {
		DeviceData data = createValidPhysicalReading();
		data.setAverageCurrent(-1);
		assertFalse(data.isValid());
	}

	@Test
	void testIsValid_withMissingPowerFactor_returnsFalse() {
		DeviceData data = createValidPhysicalReading();
		data.setPowerFactor(-1);
		assertFalse(data.isValid());
	}

	@Test
	void testIsValid_withNegativePowerFactor_returnsTrue() {
		DeviceData data = createValidPhysicalReading();
		data.setPowerFactor(-0.5f);
		assertTrue(data.isValid());
	}

	@Test
	void testIsValid_withMissingTotalRealPower_returnsFalse() {
		DeviceData data = createValidPhysicalReading();
		data.setTotalRealPower(-1);
		assertFalse(data.isValid());
	}

	@Test
	void testIsValid_withMissingTotalEnergyConsumed_returnsFalse() {
		DeviceData data = createValidPhysicalReading();
		data.setTotalEnergyConsumed(-1);
		assertFalse(data.isValid());
	}

	private DeviceData createValidPhysicalReading() {
		DeviceData data = new DeviceData(A_SITE, A_CUSTOMER, A_DEVICE);
		data.setDate(new Date());
		data.setAverageVoltage(240f);
		data.setAverageCurrent(15f);
		data.setPowerFactor(0.95f);
		data.setTotalRealPower(12.5f);
		data.setTotalEnergyConsumed(1000f);
		return data;
	}

	private DeviceData createFullyPopulated() {
		DeviceData data = createValidPhysicalReading();
		data.setEnergyConsumed(5f);
		data.setVirtual(true);
		data.setSite(true);
		data.setDaylight(true);
		data.setWeatherSummary("Clear");
		data.setTemperature(70f);
		data.setVisibility(10f);
		data.setCloudCover(0.2f);
		data.setUVIndex(3f);
		data.setPrecipitationIntensity(0.1f);
		data.setInformationalError(2);
		data.setCriticalError(3);
		data.setCriticalErrorString("critical");
		data.setInformationalErrorString("informational");
		return data;
	}
}
