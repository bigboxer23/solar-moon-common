package com.bigboxer23.solar_moon.subscription;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.TestConstants;
import com.bigboxer23.solar_moon.TestUtils;
import com.bigboxer23.solar_moon.data.Device;
import com.bigboxer23.solar_moon.data.Subscription;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

/** */
public class TestSubscriptionComponent implements IComponentRegistry, TestConstants {
	@Test
	public void testSubscription() {
		Subscription sub = subscriptionComponent.updateSubscription(CUSTOMER_ID, 1);
		assertNotNull(sub);
		assertEquals(1, sub.getPacks());
		assertEquals(CUSTOMER_ID, sub.getCustomerId());
		assertEquals(1, subscriptionComponent.getSubscriptionPacks(CUSTOMER_ID));
		subscriptionComponent.updateSubscription(CUSTOMER_ID, 0);
		assertEquals(0, subscriptionComponent.getSubscriptionPacks(CUSTOMER_ID));
		assertEquals(0, subscriptionComponent.getSubscriptionPacks("1234"));
	}

	@Test
	public void testDeleteSubscription() {
		subscriptionComponent.updateSubscription(CUSTOMER_ID, 1);
		assertEquals(1, subscriptionComponent.getSubscriptionPacks(CUSTOMER_ID));
		subscriptionComponent.deleteSubscription(CUSTOMER_ID);
		assertEquals(0, subscriptionComponent.getSubscriptionPacks(CUSTOMER_ID));
	}

	@Test
	public void testSubscriptionLimit() {
		int deviceCount = deviceComponent
				.getDevicesForCustomerId(TestConstants.CUSTOMER_ID)
				.size();
		Device testDevice = new Device();
		testDevice.setClientId(TestConstants.CUSTOMER_ID);
		for (int ai = deviceCount; ai < SubscriptionComponent.DEVICES_PER_SUBSCRIPTION; ai++) {
			testDevice.setId("test-" + ai);
			testDevice.setName(deviceName + ai);
			assertNotNull(deviceComponent.addDevice(testDevice));
		}
		assertNull(deviceComponent.addDevice(testDevice));
		deviceComponent.deleteDevicesByCustomerId(TestConstants.CUSTOMER_ID);
		assertNotNull(deviceComponent.addDevice(testDevice));
		subscriptionComponent.updateSubscription(TestConstants.CUSTOMER_ID, 0);
		assertNull(deviceComponent.addDevice(testDevice));
	}

	@AfterAll
	public static void afterAll() {
		TestUtils.nukeCustomerId(TestConstants.CUSTOMER_ID);
	}
}
