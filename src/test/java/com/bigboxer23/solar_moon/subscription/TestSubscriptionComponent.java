package com.bigboxer23.solar_moon.subscription;

import static org.junit.jupiter.api.Assertions.*;

import com.bigboxer23.solar_moon.IComponentRegistry;
import com.bigboxer23.solar_moon.TestConstants;
import com.bigboxer23.solar_moon.TestUtils;
import com.bigboxer23.solar_moon.data.Subscription;
import com.bigboxer23.solar_moon.dynamodb.AbstractDynamodbComponent;
import com.bigboxer23.solar_moon.util.TimeConstants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** */
public class TestSubscriptionComponent extends AbstractDynamodbComponent<Subscription>
		implements IComponentRegistry, TestConstants {
	@Test
	public void testSubscription() {
		Subscription sub = subscriptionComponent.updateSubscription(CUSTOMER_ID, 1);
		assertNotNull(sub);
		assertEquals(1, sub.getPacks());
		assertEquals(CUSTOMER_ID, sub.getCustomerId());
		assertEquals(
				1,
				subscriptionComponent
						.getSubscription(CUSTOMER_ID)
						.map(Subscription::getPacks)
						.orElse(0));
		subscriptionComponent.updateSubscription(CUSTOMER_ID, 0);
		assertEquals(
				0,
				subscriptionComponent
						.getSubscription(CUSTOMER_ID)
						.map(Subscription::getPacks)
						.orElse(-1));
		assertEquals(
				0,
				subscriptionComponent
						.getSubscription("1234")
						.map(Subscription::getPacks)
						.orElse(0));
	}

	@Test
	public void testDeleteSubscription() {
		subscriptionComponent.updateSubscription(CUSTOMER_ID, 1);
		assertEquals(
				1,
				subscriptionComponent
						.getSubscription(CUSTOMER_ID)
						.map(Subscription::getPacks)
						.orElse(0));
		subscriptionComponent.deleteSubscription(CUSTOMER_ID);
		assertEquals(
				0,
				subscriptionComponent
						.getSubscription(CUSTOMER_ID)
						.map(Subscription::getPacks)
						.orElse(0));
	}

	@Test
	public void testSubscriptionLimit() {
		int deviceCount = deviceComponent
				.getDevicesForCustomerId(TestConstants.CUSTOMER_ID)
				.size();
		for (int ai = deviceCount; ai < SubscriptionComponent.DEVICES_PER_SUBSCRIPTION; ai++) {
			assertNotNull(TestUtils.addDevice(
					TestConstants.deviceName + ai,
					TestUtils.getDevice(),
					false,
					TestUtils.getDevice().getSiteId()));
		}

		assertNull(TestUtils.addDevice(
				TestConstants.deviceName + "2222",
				TestUtils.getDevice(),
				false,
				TestUtils.getDevice().getSiteId()));
		deviceComponent.deleteDevicesByCustomerId(TestConstants.CUSTOMER_ID);
		assertNotNull(TestUtils.addDevice(
				TestConstants.deviceName + "2222",
				TestUtils.getDevice(),
				false,
				TestUtils.getDevice().getSiteId()));
		subscriptionComponent.updateSubscription(TestConstants.CUSTOMER_ID, 0);
		assertNull(TestUtils.addDevice(
				TestConstants.deviceName + "222222",
				TestUtils.getDevice(),
				false,
				TestUtils.getDevice().getSiteId()));
	}

	@Test
	public void canAddAnotherDevice() {
		// Test Old Trial
		deviceComponent.deleteDevicesByCustomerId(CUSTOMER_ID);
		assertTrue(deviceComponent.getDevicesForCustomerId(CUSTOMER_ID).isEmpty());
		// Manually create "old" subscription
		getTable()
				.updateItem(builder -> builder.item(new Subscription(
						CUSTOMER_ID,
						SubscriptionComponent.TRIAL_MODE,
						System.currentTimeMillis() - (TimeConstants.NINETY_DAYS + 1000))));
		assertNull(TestUtils.addDevice(
				TestConstants.deviceName + "blah",
				TestUtils.getDevice(),
				false,
				TestUtils.getDevice().getSiteId()));
		subscriptionComponent.deleteSubscription(CUSTOMER_ID);

		// Test Valid Trial
		subscriptionComponent.updateSubscription(CUSTOMER_ID, SubscriptionComponent.TRIAL_MODE);
		for (int ai = 0; ai < SubscriptionComponent.TRIAL_DEVICE_COUNT; ai++) {
			TestUtils.addDevice(
					TestConstants.deviceName + ai,
					TestUtils.getDevice(),
					false,
					TestUtils.getDevice().getSiteId());
		}
		assertEquals(
				SubscriptionComponent.TRIAL_DEVICE_COUNT,
				deviceComponent.getDevicesForCustomerId(CUSTOMER_ID).size());
		TestUtils.addDevice(
				TestConstants.deviceName + 100,
				TestUtils.getDevice(),
				false,
				TestUtils.getDevice().getSiteId());
		assertEquals(
				SubscriptionComponent.TRIAL_DEVICE_COUNT,
				deviceComponent.getDevicesForCustomerId(CUSTOMER_ID).size());

		// Test base subscription check
		subscriptionComponent.updateSubscription(CUSTOMER_ID, 1);
		for (int ai = deviceComponent.getDevicesForCustomerId(CUSTOMER_ID).size();
				ai < SubscriptionComponent.DEVICES_PER_SUBSCRIPTION + 1;
				ai++) {
			TestUtils.addDevice(
					TestConstants.deviceName + ai,
					TestUtils.getDevice(),
					false,
					TestUtils.getDevice().getSiteId());
		}
		assertEquals(
				SubscriptionComponent.DEVICES_PER_SUBSCRIPTION,
				deviceComponent.getDevicesForCustomerId(CUSTOMER_ID).size());

		// Check multiple subscriptions
		subscriptionComponent.updateSubscription(CUSTOMER_ID, 2);
		for (int ai = deviceComponent.getDevicesForCustomerId(CUSTOMER_ID).size();
				ai < (2 * SubscriptionComponent.DEVICES_PER_SUBSCRIPTION) + 2;
				ai++) {
			TestUtils.addDevice(
					TestConstants.deviceName + ai,
					TestUtils.getDevice(),
					false,
					TestUtils.getDevice().getSiteId());
		}
		assertEquals(
				2 * SubscriptionComponent.DEVICES_PER_SUBSCRIPTION,
				deviceComponent.getDevicesForCustomerId(CUSTOMER_ID).size());
	}

	@Test
	public void isTrialValid() {
		assertTrue(subscriptionComponent.isTrialValid(CUSTOMER_ID));
		// Manually create old subscription
		getTable()
				.updateItem(builder -> builder.item(new Subscription(
						CUSTOMER_ID,
						SubscriptionComponent.TRIAL_MODE,
						System.currentTimeMillis() - (TimeConstants.NINETY_DAYS + 1000))));
		assertFalse(subscriptionComponent.isTrialValid(CUSTOMER_ID));
	}

	@BeforeAll
	public static void beforeAll() {
		TestUtils.setupSite();
	}

	@AfterAll
	public static void afterAll() {
		TestUtils.nukeCustomerId(TestConstants.CUSTOMER_ID);
	}

	@Override
	protected String getTableName() {
		return subscriptionComponent.getTableName();
	}

	@Override
	protected Class<Subscription> getObjectClass() {
		return subscriptionComponent.getObjectClass();
	}
}
