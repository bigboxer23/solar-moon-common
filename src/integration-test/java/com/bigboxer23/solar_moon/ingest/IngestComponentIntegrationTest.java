package com.bigboxer23.solar_moon.ingest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.TestConstants;
import com.bigboxer23.solar_moon.TestUtils;
import com.bigboxer23.solar_moon.data.Device;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

/** */
public class IngestComponentIntegrationTest implements IComponentRegistry, MeterConstants, TestConstants {
	@Test
	public void maybeCorrectForRollover() {
		assertEquals(
				OBVIOUS_ROLLOVER_MARGIN, generationComponent.maybeCorrectForRollover(10000, OBVIOUS_ROLLOVER_MARGIN));
		assertEquals(OBVIOUS_ROLLOVER_MARGIN, generationComponent.maybeCorrectForRollover(0, OBVIOUS_ROLLOVER_MARGIN));
		assertEquals(
				OBVIOUS_ROLLOVER_MARGIN, generationComponent.maybeCorrectForRollover(999, OBVIOUS_ROLLOVER_MARGIN));
		assertEquals(
				OBVIOUS_ROLLOVER_MARGIN, generationComponent.maybeCorrectForRollover(999, OBVIOUS_ROLLOVER_MARGIN));
		assertEquals(
				OBVIOUS_ROLLOVER + OBVIOUS_ROLLOVER_MARGIN,
				generationComponent.maybeCorrectForRollover(
						OBVIOUS_ROLLOVER, OBVIOUS_ROLLOVER + OBVIOUS_ROLLOVER_MARGIN));
		assertEquals(
				OBVIOUS_ROLLOVER_MARGIN,
				generationComponent.maybeCorrectForRollover(OBVIOUS_ROLLOVER - 1, OBVIOUS_ROLLOVER_MARGIN));
		assertEquals(
				OBVIOUS_ROLLOVER + OBVIOUS_ROLLOVER_MARGIN - 1,
				generationComponent.maybeCorrectForRollover(OBVIOUS_ROLLOVER - 1, OBVIOUS_ROLLOVER_MARGIN - 1));
		assertEquals(
				OBVIOUS_ROLLOVER + 10,
				generationComponent.maybeCorrectForRollover(OBVIOUS_ROLLOVER - (OBVIOUS_ROLLOVER_MARGIN - 1), 10));
		assertEquals(
				OBVIOUS_ROLLOVER + 10,
				generationComponent.maybeCorrectForRollover(OBVIOUS_ROLLOVER - (OBVIOUS_ROLLOVER_MARGIN - 1), 10));
		assertEquals(10, generationComponent.maybeCorrectForRollover(OBVIOUS_ROLLOVER - OBVIOUS_ROLLOVER_MARGIN, 10));
	}

	@AfterAll
	public static void cleanup() {
		TestUtils.nukeCustomerId(CUSTOMER_ID);
	}

	@Test
	public void testFindDeviceFromDeviceNameExactMatch() {
		TestUtils.setupCustomer();
		subscriptionComponent.updateSubscription(CUSTOMER_ID, 1);
		Device device = new Device("test-device-id", CUSTOMER_ID, "exact-device-name");
		deviceComponent.addDevice(device);

		Device found = generationComponent.findDeviceFromDeviceName(CUSTOMER_ID, "exact-device-name");
		assertNotNull(found);
		assertEquals("exact-device-name", found.getDeviceName());
		assertEquals("test-device-id", found.getId());
	}

	@Test
	public void testFindDeviceFromDeviceNameFuzzyMatch() {
		TestUtils.setupCustomer();
		subscriptionComponent.updateSubscription(CUSTOMER_ID, 1);
		Device device = new Device("test-device-id-2", CUSTOMER_ID, "SN:123456789:metadata");
		deviceComponent.addDevice(device);

		Device found = generationComponent.findDeviceFromDeviceNameFuzzy(CUSTOMER_ID, "123456789");
		assertNotNull(found);
		assertEquals("123456789", found.getDeviceName());
		assertEquals("test-device-id-2", found.getId());

		Device reloaded =
				deviceComponent.findDeviceById("test-device-id-2", CUSTOMER_ID).orElse(null);
		assertNotNull(reloaded);
		assertEquals("123456789", reloaded.getDeviceName());
	}

	@Test
	public void testFindDeviceFromDeviceNameFuzzyNoMatch() {
		TestUtils.setupCustomer();
		subscriptionComponent.updateSubscription(CUSTOMER_ID, 1);

		Device found = generationComponent.findDeviceFromDeviceNameFuzzy(CUSTOMER_ID, "999999999");
		assertNotNull(found);
		assertEquals("999999999", found.getDeviceName());

		Device reloaded =
				deviceComponent.findDeviceByDeviceName(CUSTOMER_ID, "999999999").orElse(null);
		assertNotNull(reloaded);
		assertEquals("999999999", reloaded.getDeviceName());
	}
}
